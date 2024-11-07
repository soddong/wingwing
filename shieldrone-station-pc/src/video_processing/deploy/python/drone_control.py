import cv2
import numpy as np

class DroneController:
    def __init__(self):
        self.is_init = False

    def init(self, frame_width, frame_height, fps):
        self.frame_center_x = frame_width / 2
        self.frame_center_y = frame_height / 2
        self.fps = fps
        self.desired_box_width = 100
        self.desired_box_height = 200
        self.is_init = True

    def adjust_drone(self, bbox):
        obj_id, obj_class, score, xmin, ymin, xmax, ymax = bbox
        target_x = (xmin + xmax) / 2
        target_y = (ymin + ymax) / 2
        box_width = xmax - xmin
        box_height = ymax - ymin

        offset_x = target_x - self.frame_center_x
        offset_y = target_y - self.frame_center_y

        if box_width < self.desired_box_width or box_height < self.desired_box_height:
            movement = "forward"
        elif box_width > self.desired_box_width or box_height > self.desired_box_height:
            movement = "backward"
        else:
            movement = "hover"

        control_values = {
            'offset_x': offset_x,
            'offset_y': offset_y,
            'movement': movement,
            'box_width': box_width,
            'box_height': box_height
        }
        return control_values

    def visualize_control(self, frame, control_values):
        offset_x = control_values['offset_x']
        offset_y = control_values['offset_y']
        movement = control_values['movement']

        end_x = int(self.frame_center_x + offset_x * 0.5)
        end_y = int(self.frame_center_y + offset_y * 0.5)
        cv2.arrowedLine(frame, (int(self.frame_center_x), int(self.frame_center_y)), (end_x, end_y),
                        (0, 255, 0), 2, tipLength=0.5)

        cv2.putText(frame, f"Offset X: {offset_x:.2f}, Offset Y: {offset_y:.2f}",
                    (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
        cv2.putText(frame, f"Drone Movement: {movement}",
                    (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
        return frame