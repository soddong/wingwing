import cv2
import numpy as np
import math

class SpatialInfoTracker:
    def __init__(self):
        # 드론의 고도 및 카메라 각도 (실시간 업데이트 가능)
        self.DRONE_ALTITUDE = 2.6  # 초기값 (미터)
        self.rotation_angle_deg = 45  # 초기값 (도)

        # 지수 이동 평균을 위한 알파 값 (0 < alpha <= 1)
        self.EMA_ALPHA = 0.2

        # 목표 사람의 바운딩 박스 크기(폭, 높이)의 EMA 값을 저장할 변수 초기화
        self.ema_bbox_width = None
        self.ema_bbox_height = None

        # 이전 프레임에서의 각 사람의 위치를 저장할 딕셔너리 초기화
        self.prev_positions = {}  # Key: obj_id, Value: (x, y)

    def lazy_init(self, FRAME_WIDTH, FRAME_HEIGHT):
        self.FRAME_WIDTH = FRAME_WIDTH
        self.FRAME_HEIGHT = FRAME_HEIGHT
        self.initialize_camera_params()

    def initialize_camera_params(self):
        # 수평 시야각 (도)
        self.horizontal_fov_deg = 82.1

        # 수평 시야각의 절반을 라디안으로 변환
        half_horizontal_fov_rad = np.radians(self.horizontal_fov_deg / 2)

        # 수평 시야각의 탄젠트 값
        tan_half_horizontal_fov = np.tan(half_horizontal_fov_rad)

        # 종횡비 계산
        aspect_ratio = self.FRAME_WIDTH / self.FRAME_HEIGHT

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

        # 수평 시야각의 탄젠트 값 저장 (거리 계산에 사용)
        self.tan_half_horizontal_fov = tan_half_horizontal_fov

        self.update_R_T()


    def update_R_T(self):
        # 회전 행렬 R 계산
        theta = np.radians(self.rotation_angle_deg)
        self.R = np.array([[1, 0, 0],
                           [0, np.cos(theta), -np.sin(theta)],
                           [0, np.sin(theta), np.cos(theta)]])

        # 카메라의 위치 (월드 좌표계에서)
        self.T = np.array([[0],
                           [0],
                           [self.DRONE_ALTITUDE]])

    def update_drone_parameters(self, altitude, rotation_angle_deg):
        # 드론의 고도와 카메라 각도를 업데이트
        self.DRONE_ALTITUDE = altitude
        self.rotation_angle_deg = rotation_angle_deg

        # 카메라 파라미터 재계산
        self.update_R_T()

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

    def run(self, target_bbox, other_bboxes, fps):
        delta_t = 1 / fps  # 프레임 간 시간 간격 (초)
        result = {
            "target": None,
            "others": {}
        }

        # 목표 사람의 이미지 좌표 초기화
        target_u = None
        target_v = None
        target_world_x = None
        target_world_y = None

        # 현재 프레임에서 목표 사람의 위치 찾기
        obj_id, obj_class, score, xmin, ymin, xmax, ymax = target_bbox

        # 바운딩 박스의 중심 좌표 계산
        target_u = (xmin + xmax) / 2
        target_v = (ymin + ymax) / 2

        # 바운딩 박스의 폭과 높이 계산 및 EMA 적용
        bbox_width = xmax - xmin
        bbox_height = ymax - ymin

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

        # 목표 사람의 정보 저장
        result["target"] = {
            "world_coords": (target_world_x, target_world_y),
        }

        # 목표 사람의 위치를 이전 위치로 저장
        self.prev_positions[obj_id] = (target_world_x, target_world_y)

        # 다른 사람들의 상대 위치 계산
        for box in other_bboxes:
            obj_id_other, obj_class_other, score_other, xmin_other, ymin_other, xmax_other, ymax_other = box
            obj_id_other = int(obj_id_other)

            # 바운딩 박스의 중심 좌표 계산
            person_u = (xmin_other + xmax_other) / 2
            person_v = (ymin_other + ymax_other) / 2

            # 이미지 좌표를 월드 좌표로 변환
            person_world_x, person_world_y = self.image_to_world(person_u, person_v)

            # 상대 위치 계산
            delta_x = person_world_x - target_world_x
            delta_y = person_world_y - target_world_y
            distance = math.hypot(delta_x, delta_y)

            # 목표 사람과의 거리 기준 (바운딩 박스 폭의 2배)
            bbox_width_meters = (target_bbox_width / self.FRAME_WIDTH) * (2 * self.DRONE_ALTITUDE * self.tan_half_horizontal_fov)
            distance_threshold = 2 * bbox_width_meters
            is_near = distance <= distance_threshold

            # 이전 위치가 있는 경우 속도 계산
            velocity = None
            getting_closer_quickly = False
            if obj_id_other in self.prev_positions:
                prev_x, prev_y = self.prev_positions[obj_id_other]
                vel_x = (person_world_x - prev_x) / delta_t
                vel_y = (person_world_y - prev_y) / delta_t

                dir_x = target_world_x - person_world_x
                dir_y = target_world_y - person_world_y
                distance_to_target = math.hypot(dir_x, dir_y)

                if distance_to_target != 0:
                    unit_dir_x = dir_x / distance_to_target
                    unit_dir_y = dir_y / distance_to_target
                    velocity = vel_x * unit_dir_x + vel_y * unit_dir_y
                    # 빠르게 접근하는 경우
                    if velocity and velocity > 0.5:
                        getting_closer_quickly = True

            # 객체 정보를 결과 리스트에 추가
            result["others"][obj_id_other]={
                "world_coords": (person_world_x, person_world_y),
                "delta_coords": (delta_x, delta_y),
                "distance": distance,
                "is_near": is_near,
                "velocity": velocity,
                "getting_closer_quickly": getting_closer_quickly
            }

            # 현재 위치를 저장
            self.prev_positions[obj_id_other] = (person_world_x, person_world_y)

        return result

    def visualize(self, frame, result, other_bboxes):

        # 다른 사람들 시각화
        for box in other_bboxes:
            obj_id_other, obj_class_other, score_other, xmin_other, ymin_other, xmax_other, ymax_other = box
            person = result["others"][int(obj_id_other)]
            person_x, person_y = person["world_coords"]
            delta_x, delta_y = person["delta_coords"]
            distance = person["distance"]
            velocity = person["velocity"]
            is_near = person["is_near"]
            getting_closer_quickly = person["getting_closer_quickly"]
            xmin_int = int(xmin_other)
            ymin_int = int(ymin_other)
            xmax_int = int(xmax_other)
            ymax_int = int(ymax_other)

            # 바운딩 박스 내부 오른쪽 하단에 정보 수직 표시
            info_lines = [
                f"ID: {int(obj_id_other)}",
                f"Δx: {delta_x:.2f}",
                f"Δy: {delta_y:.2f}",
                f"Dist: {distance:.2f}",
                f"Vel: {velocity:.2f}" if velocity is not None else "Vel: N/A",
                "Approaching" if getting_closer_quickly else "Stable"
            ]

            # 텍스트를 바운딩 박스 내부에 수직으로 나열
            text_x = xmax_int - 120  # 바운딩 박스의 오른쪽 하단 기준
            text_y = ymin_int + 20   # 첫 번째 텍스트 위치
            for line in info_lines:
                cv2.putText(frame, line, (text_x, text_y), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,0,255), 1)
                text_y += 20  # 다음 줄로 이동

        return frame
