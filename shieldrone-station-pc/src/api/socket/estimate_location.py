import cv2
import numpy as np
import math

class DroneTracker:
    def __init__(self, video_path, detection_data_path, target_id):
        # 비디오 및 검출 데이터 로드
        self.cap = cv2.VideoCapture(video_path)
        self.det_res = np.load(detection_data_path)
        self.frame_num = -1
        self.target_id = target_id  # 목표 사람의 ID

        # 드론의 고도 및 카메라 각도 (실시간 업데이트 가능)
        self.DRONE_ALTITUDE = 2.6  # 초기값 (미터)
        self.rotation_angle_deg = 45  # 초기값 (도)

        # 이미지 해상도 (픽셀)
        self.FRAME_WIDTH = int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        self.FRAME_HEIGHT = int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        # 카메라 파라미터 초기화
        self.initialize_camera_params()

        # 지수 이동 평균을 위한 알파 값 (0 < alpha <= 1)
        self.EMA_ALPHA = 0.2

        # 목표 사람의 바운딩 박스 크기(폭, 높이)의 EMA 값을 저장할 변수 초기화
        self.ema_bbox_width = None
        self.ema_bbox_height = None

        # 이전 프레임에서의 각 사람의 위치를 저장할 딕셔너리 초기화
        self.prev_positions = {}  # Key: obj_id, Value: (x, y)

        # FPS 가져오기
        fps = self.cap.get(cv2.CAP_PROP_FPS)
        if fps == 0 or fps is None:
            fps = 24  # 기본 FPS 값 설정 (필요에 따라 조정)
        self.delta_t = 1 / fps  # 프레임 간 시간 간격 (초)

    def initialize_camera_params(self):
        # 수평 시야각 (도)
        self.horizontal_fov_deg = 82.1

        # 수평 시야각의 절반을 라디안으로 변환
        half_horizontal_fov_rad = np.radians(self.horizontal_fov_deg / 2)

        # 수평 시야각의 탄젠트 값
        tan_half_horizontal_fov = np.tan(half_horizontal_fov_rad)

        # 새로운 종횡비 (16:9)
        aspect_ratio = self.FRAME_WIDTH / self.FRAME_HEIGHT  # 16/9

        # 수직 시야각 계산
        tan_half_vertical_fov = tan_half_horizontal_fov / aspect_ratio
        half_vertical_fov_rad = np.arctan(tan_half_vertical_fov)
        self.vertical_fov_rad = 2 * half_vertical_fov_rad
        self.vertical_fov_deg = np.degrees(self.vertical_fov_rad)

        # 초점 거리 계산
        self.f_x = (self.FRAME_WIDTH / 2) / tan_half_horizontal_fov
        self.f_y = (self.FRAME_HEIGHT / 2) / tan_half_vertical_fov

        # 카메라 매트릭스 구성
        self.CAMERA_MATRIX = np.array([[self.f_x, 0, self.FRAME_WIDTH / 2],
                                       [0, self.f_y, self.FRAME_HEIGHT / 2],
                                       [0, 0, 1]])

        # 왜곡 계수 (왜곡이 없다고 가정)
        self.DIST_COEFFS = np.zeros((4, 1))

        # 회전 행렬 R 계산
        theta = np.radians(self.rotation_angle_deg)
        self.R = np.array([[1, 0, 0],
                           [0, np.cos(theta), -np.sin(theta)],
                           [0, np.sin(theta), np.cos(theta)]])

        # 카메라의 위치 (월드 좌표계에서)
        self.T = np.array([[0],
                           [0],
                           [self.DRONE_ALTITUDE]])

        # 수평 시야각의 탄젠트 값 저장 (거리 계산에 사용)
        self.tan_half_horizontal_fov = tan_half_horizontal_fov

    def update_drone_parameters(self, altitude, rotation_angle_deg):
        # 드론의 고도와 카메라 각도를 업데이트
        self.DRONE_ALTITUDE = altitude
        self.rotation_angle_deg = rotation_angle_deg

        # 카메라 파라미터 재계산
        self.initialize_camera_params()

    def image_to_world(self, u, v):
        # 이미지 좌표를 정규화된 이미지 좌표로 변환
        uv1 = np.array([[u],
                        [v],
                        [1]])
        inv_camera_matrix = np.linalg.inv(self.CAMERA_MATRIX)
        normalized_coords = inv_camera_matrix @ uv1

        # 카메라 좌표계에서의 광선 방향 벡터
        ray_direction = self.R @ normalized_coords

        # 지면(z=0)과의 교점을 계산
        s = -self.T[2][0] / ray_direction[2][0]
        world_coords = self.T + s * ray_direction

        return world_coords[0][0], world_coords[1][0]

    def run(self):
        while self.cap.isOpened():
            ret, frame = self.cap.read()
            self.frame_num += 1
            if not ret or self.frame_num >= len(self.det_res):
                break

            # 드론의 고도와 회전 각도를 실시간으로 업데이트 (예시)
            # 실제 드론에서 데이터를 받아와서 업데이트해야 함
            # self.update_drone_parameters(new_altitude, new_rotation_angle_deg)

            # 현재 프레임의 검출 결과 가져오기
            boxes = self.det_res[self.frame_num]

            # 목표 사람의 이미지 좌표 초기화
            target_u = None
            target_v = None
            target_world_x = None
            target_world_y = None
            target_bbox_width = None
            target_bbox_height = None

            # 현재 프레임에서 목표 사람의 위치 찾기
            for box in boxes:
                obj_id, obj_class, score, xmin, ymin, xmax, ymax = box

                if obj_id == self.target_id:
                    # 바운딩 박스의 중심 좌표 계산
                    target_u = (xmin + xmax) / 2
                    target_v = (ymin + ymax) / 2

                    # 바운딩 박스의 폭과 높이 계산
                    bbox_width = xmax - xmin
                    bbox_height = ymax - ymin

                    # EMA 적용
                    if self.ema_bbox_width is None:
                        self.ema_bbox_width = bbox_width
                        self.ema_bbox_height = bbox_height
                    else:
                        self.ema_bbox_width = self.EMA_ALPHA * bbox_width + (1 - self.EMA_ALPHA) * self.ema_bbox_width
                        self.ema_bbox_height = self.EMA_ALPHA * bbox_height + (1 - self.EMA_ALPHA) * self.ema_bbox_height

                    target_bbox_width = self.ema_bbox_width
                    target_bbox_height = self.ema_bbox_height

                    # 이미지 좌표를 월드 좌표로 변환
                    target_world_x, target_world_y = self.image_to_world(target_u, target_v)

                    # 목표 사람의 위치를 이전 위치로 저장
                    self.prev_positions[obj_id] = (target_world_x, target_world_y)
                    break  # 목표 사람을 찾았으므로 루프 종료

            # 만약 목표 사람이 현재 프레임에 없다면, 다음 프레임으로 넘어감
            if target_u is None or target_v is None:
                continue

            # 다른 사람들의 상대 위치 계산 및 시각화
            for box in boxes:
                obj_id, obj_class, score, xmin, ymin, xmax, ymax = box

                # 바운딩 박스 그리기
                xmin_int = int(xmin)
                ymin_int = int(ymin)
                xmax_int = int(xmax)
                ymax_int = int(ymax)

                # 목표 사람은 빨간색, 다른 사람은 초록색으로 표시
                if obj_id == self.target_id:
                    color = (0, 0, 255)  # 빨간색
                else:
                    color = (0, 255, 0)  # 초록색

                # 바운딩 박스 그리기
                cv2.rectangle(frame, (xmin_int, ymin_int), (xmax_int, ymax_int), color, 2)

                # 바운딩 박스의 중심 좌표 계산
                person_u = (xmin + xmax) / 2
                person_v = (ymin + ymax) / 2

                # 이미지 좌표를 월드 좌표로 변환
                person_world_x, person_world_y = self.image_to_world(person_u, person_v)

                # 상대 위치 계산
                delta_x = person_world_x - target_world_x
                delta_y = person_world_y - target_world_y

                # 유클리드 거리 계산
                distance = math.hypot(delta_x, delta_y)

                # 목표 사람과의 거리 기준 (바운딩 박스 폭의 2배)
                bbox_width_meters = (target_bbox_width / self.FRAME_WIDTH) * (2 * self.DRONE_ALTITUDE * self.tan_half_horizontal_fov)
                distance_threshold = 2 * bbox_width_meters

                # 다른 사람이 목표 사람 근처에 있는지 판별
                near = False
                if obj_id != self.target_id and distance <= distance_threshold:
                    near = True
                    near_text = "Near"
                    # 근처에 있는 사람은 노란색으로 표시
                    color = (0, 255, 255)  # 노란색
                    cv2.rectangle(frame, (xmin_int, ymin_int), (xmax_int, ymax_int), color, 2)
                    cv2.putText(frame, near_text, (xmin_int, ymin_int - 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

                # 이전 위치가 있는 경우 속도 계산
                if obj_id in self.prev_positions:
                    prev_x, prev_y = self.prev_positions[obj_id]
                    # 속도 계산 (m/s)
                    vel_x = (person_world_x - prev_x) / self.delta_t
                    vel_y = (person_world_y - prev_y) / self.delta_t

                    # 목표 사람에 대한 상대 속도 계산
                    dir_x = target_world_x - person_world_x
                    dir_y = target_world_y - person_world_y
                    distance_to_target = math.hypot(dir_x, dir_y)

                    # 단위 방향 벡터 계산
                    if distance_to_target != 0:
                        unit_dir_x = dir_x / distance_to_target
                        unit_dir_y = dir_y / distance_to_target

                        # 상대 속도 계산 (스칼라 값)
                        relative_velocity = vel_x * unit_dir_x + vel_y * unit_dir_y  # m/s

                        # 접근 속도 임계값 설정 (필요에 따라 조정)
                        velocity_threshold = 0.5  # m/s

                        # 빠르게 접근하는지 판별
                        if obj_id != self.target_id and relative_velocity > velocity_threshold:
                            # 빠르게 접근하는 사람은 보라색으로 표시
                            approach_text = "Approaching"
                            color = (255, 0, 255)  # 보라색
                            cv2.rectangle(frame, (xmin_int, ymin_int), (xmax_int, ymax_int), color, 2)
                            cv2.putText(frame, approach_text, (xmin_int, ymin_int - 40), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

                    # 현재 위치를 저장
                    self.prev_positions[obj_id] = (person_world_x, person_world_y)
                else:
                    # 현재 위치를 저장
                    self.prev_positions[obj_id] = (person_world_x, person_world_y)

                # 정보 텍스트 작성
                label = f"ID:{int(obj_id)}, Δx:{delta_x:.2f}, Δy:{delta_y:.2f}"
                text_size, _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1)
                text_x = xmin_int + 5  # 바운딩 박스의 왼쪽 상단에서 약간 오른쪽으로
                text_y = ymin_int + text_size[1] + 5  # 바운딩 박스의 상단에서 약간 아래로
                cv2.putText(frame, label, (text_x, text_y), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)

            # 프레임 표시
            cv2.imshow('Frame', frame)
            if cv2.waitKey(42) == 27:  # ESC 키를 누르면 종료
                break

        self.cap.release()
        cv2.destroyAllWindows()

# 사용 예시
if __name__ == "__main__":
    video_path = '/home/soddong/S11P31A307/shieldrone-station-pc/src/api/socket/2.6mTest.mp4'
    detection_data_path = '/home/soddong/S11P31A307/shieldrone-station-pc/src/api/socket/data_3d_array.npy'
    target_id = 2  # 목표 사람의 ID

    tracker = DroneTracker(video_path, detection_data_path, target_id)
    tracker.run()
