import cv2
import numpy as np

class DroneController:
    def __init__(self):
        self.is_init = False

    def init(self, frame_width, frame_height):
        self.frame_width = frame_width
        self.frame_height =frame_height
        self.frame_center_x = frame_width / 2
        self.frame_center_y = frame_height / 2
        self.desired_box_width = 100
        self.desired_box_height = 200
        self.is_init = True
    
    def adjust_drone(self, bbox):
        """
        드론의 이동을 제어하기 위해 바운딩 박스의 중심과 화면 중심 간의 오프셋을 계산합니다.
        """
        obj_id, obj_class, score, xmin, ymin, xmax, ymax = bbox
        target_x = (xmin + xmax) / 2
        target_y = (ymin + ymax) / 2
        box_width = xmax - xmin
        box_height = ymax - ymin

        # 오프셋 계산 (화면 중심 기준)
        offset_x = target_x - self.frame_center_x
        offset_y = target_y - self.frame_center_y

        # 오프셋을 [0, 1] 범위로 정규화
        normalized_offset_x = abs(offset_x) / (self.frame_width / 2)
        normalized_offset_y = abs(offset_y) / (self.frame_height / 2)

        # 이동 방향 결정 (목표 바운딩 박스 크기와 비교)
        if box_width < self.desired_box_width or box_height < self.desired_box_height:
            movement = "forward"
        elif box_width > self.desired_box_width or box_height > self.desired_box_height:
            movement = "backward"
        else:
            movement = "hover"

        control_values = {
            'normalized_offset_x': normalized_offset_x,
            'normalized_offset_y': normalized_offset_y,
            'movement': movement,
            'box_width': box_width,
            'box_height': box_height
        }
        return control_values
    
    def visualize_control(self, frame, control_values):
        """
        드론 제어 정보를 시각적으로 표시합니다. 
        normalized_offset_x, normalized_offset_y를 사용하여 화살표를 그립니다.
        """
        # 제어 값 추출
        normalized_offset_x = control_values['normalized_offset_x']
        normalized_offset_y = control_values['normalized_offset_y']
        movement = control_values['movement']

        # 화살표의 길이를 조절 (화면의 크기와 정규화된 오프셋에 따라)
        arrow_length_x = int(self.frame_width * 0.5 * normalized_offset_x)
        arrow_length_y = int(self.frame_height * 0.5 * normalized_offset_y)

        # 화살표 끝 좌표 계산 (정규화된 오프셋 값 사용)
        end_x = int(self.frame_center_x + arrow_length_x)
        end_y = int(self.frame_center_y + arrow_length_y)

        # 화살표 그리기 (화면 중심에서 오프셋 방향으로)
        cv2.arrowedLine(frame, (int(self.frame_center_x), int(self.frame_center_y)), (end_x, end_y),
                        (0, 255, 0), 2, tipLength=0.3)

        # 정규화된 오프셋 값 및 드론 움직임 정보 출력
        cv2.putText(frame, f"Normalized Offset X: {normalized_offset_x:.2f}, Y: {normalized_offset_y:.2f}",
                    (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)
        cv2.putText(frame, f"Drone Movement: {movement}",
                    (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)

        return frame
