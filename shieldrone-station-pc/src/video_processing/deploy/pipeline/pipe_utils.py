# Copyright (c) 2022 PaddlePaddle Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import time
import os
import glob
import numpy as np
import subprocess as sp
import cv2
import threading
import time
import socket

from python.keypoint_preprocess import expand_crop


class Times(object):
    def __init__(self):
        self.time = 0.
        # start time
        self.st = 0.
        # end time
        self.et = 0.

    def start(self):
        self.st = time.time()

    def end(self, repeats=1, accumulative=True):
        self.et = time.time()
        if accumulative:
            self.time += (self.et - self.st) / repeats
        else:
            self.time = (self.et - self.st) / repeats

    def reset(self):
        self.time = 0.
        self.st = 0.
        self.et = 0.

    def value(self):
        return round(self.time, 4)
    
class StreamingTimer(object):
    def __init__(self):
        self.video_time = time.time()
        self.frame_count = 0
        self.skip_frame_num = 0

    def info(self):
        fps = self.frame_count/(time.time()-self.video_time)
        return fps, self.skip_frame_num
    
    def clear(self):
        self.video_time = time.time()
        self.frame_count = 0
        self.skip_frame_num = 0

class PipeTimer(Times):
    def __init__(self):
        super(PipeTimer, self).__init__()
        self.total_time = Times()
        self.module_time = {
            'det': Times(),
            'mot': Times(),
            'attr': Times(),
            'kpt': Times(),
            'video_action': Times(),
            'skeleton_action': Times(),
            'reid': Times(),
            'det_action': Times(),
            'cls_action': Times(),
            'vehicle_attr': Times(),
            'vehicleplate': Times(),
            'lanes': Times(),
            'vehicle_press': Times(),
            'vehicle_retrograde': Times()
        }
        self.img_num = 0
        self.track_num = 0

    def get_total_time(self):
        total_time = self.total_time.value()
        total_time = round(total_time, 4)
        average_latency = total_time / max(1, self.img_num)
        qps = 0
        if total_time > 0:
            qps = 1 / average_latency
        return total_time, average_latency, qps

    def info(self):
        total_time, average_latency, qps = self.get_total_time()
        print("------------------ Inference Time Info ----------------------")
        print("total_time(ms): {}, img_num: {}".format(total_time * 1000,
                                                       self.img_num))

        for k, v in self.module_time.items():
            v_time = round(v.value(), 4)
            if v_time > 0 and k in ['det', 'mot', 'video_action']:
                print("{} time(ms): {}; per frame average time(ms): {}".format(
                    k, v_time * 1000, v_time * 1000 / self.img_num))
            elif v_time > 0:
                print("{} time(ms): {}; per trackid average time(ms): {}".
                      format(k, v_time * 1000, v_time * 1000 / self.track_num))

        print("average latency time(ms): {:.2f}, QPS: {:2f}".format(
            average_latency * 1000, qps))
        return qps

    def report(self, average=False):
        dic = {}
        dic['total'] = round(self.total_time.value() / max(1, self.img_num),
                             4) if average else self.total_time.value()
        dic['det'] = round(self.module_time['det'].value() /
                           max(1, self.img_num),
                           4) if average else self.module_time['det'].value()
        dic['mot'] = round(self.module_time['mot'].value() /
                           max(1, self.img_num),
                           4) if average else self.module_time['mot'].value()
        dic['attr'] = round(self.module_time['attr'].value() /
                            max(1, self.img_num),
                            4) if average else self.module_time['attr'].value()
        dic['kpt'] = round(self.module_time['kpt'].value() /
                           max(1, self.img_num),
                           4) if average else self.module_time['kpt'].value()
        dic['video_action'] = self.module_time['video_action'].value()
        dic['skeleton_action'] = round(
            self.module_time['skeleton_action'].value() / max(1, self.img_num),
            4) if average else self.module_time['skeleton_action'].value()

        dic['img_num'] = self.img_num
        return dic


class PushStream(object):
    def __init__(self, pushurl="rtsp://127.0.0.1:8554/"):
        self.command = ""
        # 自行设置
        self.pushurl = pushurl

    def initcmd(self, fps, width, height):
        self.command = [
            'ffmpeg', '-y', '-f', 'rawvideo', '-vcodec', 'rawvideo', '-pix_fmt',
            'bgr24', '-s', "{}x{}".format(width, height), '-r', str(fps), '-i',
            '-', '-pix_fmt', 'yuv420p', '-f', 'rtsp', self.pushurl
        ]
        self.pipe = sp.Popen(self.command, stdin=sp.PIPE)


def get_test_images(infer_dir, infer_img):
    """
    Get image path list in TEST mode
    """
    assert infer_img is not None or infer_dir is not None, \
        "--infer_img or --infer_dir should be set"
    assert infer_img is None or os.path.isfile(infer_img), \
            "{} is not a file".format(infer_img)
    assert infer_dir is None or os.path.isdir(infer_dir), \
            "{} is not a directory".format(infer_dir)

    # infer_img has a higher priority
    if infer_img and os.path.isfile(infer_img):
        return [infer_img]

    images = set()
    infer_dir = os.path.abspath(infer_dir)
    assert os.path.isdir(infer_dir), \
        "infer_dir {} is not a directory".format(infer_dir)
    exts = ['jpg', 'jpeg', 'png', 'bmp']
    exts += [ext.upper() for ext in exts]
    for ext in exts:
        images.update(glob.glob('{}/*.{}'.format(infer_dir, ext)))
    images = list(images)

    assert len(images) > 0, "no image found in {}".format(infer_dir)
    print("Found {} inference images in total.".format(len(images)))

    return images


def crop_image_with_det(batch_input, det_res, thresh=0.3):
    boxes = det_res['boxes']
    score = det_res['boxes'][:, 1]
    boxes_num = det_res['boxes_num']
    start_idx = 0
    crop_res = []
    for b_id, input in enumerate(batch_input):
        boxes_num_i = boxes_num[b_id]
        if boxes_num_i == 0:
            continue
        boxes_i = boxes[start_idx:start_idx + boxes_num_i, :]
        score_i = score[start_idx:start_idx + boxes_num_i]
        res = []
        for box, s in zip(boxes_i, score_i):
            if s > thresh:
                crop_image, new_box, ori_box = expand_crop(input, box)
                if crop_image is not None:
                    res.append(crop_image)
        crop_res.append(res)
    return crop_res


def normal_crop(image, rect):
    imgh, imgw, c = image.shape
    label, conf, xmin, ymin, xmax, ymax = [int(x) for x in rect.tolist()]
    org_rect = [xmin, ymin, xmax, ymax]
    if label != 0:
        return None, None, None
    xmin = max(0, xmin)
    ymin = max(0, ymin)
    xmax = min(imgw, xmax)
    ymax = min(imgh, ymax)
    return image[ymin:ymax, xmin:xmax, :], [xmin, ymin, xmax, ymax], org_rect


def crop_image_with_mot(input, mot_res, expand=True):
    res = mot_res['boxes']
    crop_res = []
    new_bboxes = []
    ori_bboxes = []
    for box in res:
        if expand:
            crop_image, new_bbox, ori_bbox = expand_crop(input, box[1:])
        else:
            crop_image, new_bbox, ori_bbox = normal_crop(input, box[1:])
        if crop_image is not None:
            crop_res.append(crop_image)
            new_bboxes.append(new_bbox)
            ori_bboxes.append(ori_bbox)
    return crop_res, new_bboxes, ori_bboxes


def parse_mot_res(input):
    mot_res = []
    boxes, scores, ids = input[0]
    for box, score, i in zip(boxes[0], scores[0], ids[0]):
        xmin, ymin, w, h = box
        res = [i, 0, score, xmin, ymin, xmin + w, ymin + h]
        mot_res.append(res)
    return {'boxes': np.array(mot_res)}


def refine_keypoint_coordinary(kpts, bbox, coord_size):
    """
        This function is used to adjust coordinate values to a fixed scale.
    """
    tl = bbox[:, 0:2]
    wh = bbox[:, 2:] - tl
    tl = np.expand_dims(np.transpose(tl, (1, 0)), (2, 3))
    wh = np.expand_dims(np.transpose(wh, (1, 0)), (2, 3))
    target_w, target_h = coord_size
    res = (kpts - tl) / wh * np.expand_dims(
        np.array([[target_w], [target_h]]), (2, 3))
    return res


def parse_mot_keypoint(input, coord_size):
    parsed_skeleton_with_mot = {}
    ids = []
    skeleton = []
    for tracker_id, kpt_seq in input:
        ids.append(tracker_id)
        kpts = np.array(kpt_seq.kpts, dtype=np.float32)[:, :, :2]
        kpts = np.expand_dims(np.transpose(kpts, [2, 0, 1]),
                              -1)  #T, K, C -> C, T, K, 1
        bbox = np.array(kpt_seq.bboxes, dtype=np.float32)
        skeleton.append(refine_keypoint_coordinary(kpts, bbox, coord_size))
    parsed_skeleton_with_mot["mot_id"] = ids
    parsed_skeleton_with_mot["skeleton"] = skeleton
    return parsed_skeleton_with_mot

class HandAboveHeadTracker(object):
    def __init__(self, min_hold_time=5):
        self.holding_ids=dict()
        self.min_hold_time = min_hold_time

    def update(self, kpt_pred, mot_res):
        # 신뢰도 임계값 설정
        confidence_threshold = 0.5
        mot_bboxes = mot_res.get('boxes')

        # 키포인트 좌표 및 신뢰도 점수
        keypoints = kpt_pred['keypoint']  # (num_persons, num_keypoints, 2)
        scores = kpt_pred['score']  # (num_persons, num_keypoints)
        
        # COCO 포맷 기준으로 인덱스 설정
        head_y = keypoints[:, 0, 1]  # 코(nose)의 y 좌표
        left_wrist_y = keypoints[:, 9, 1]  # 왼쪽 손목 y 좌표
        right_wrist_y = keypoints[:, 10, 1]  # 오른쪽 손목 y 좌표

        # 신뢰도 기준을 충족하는 경우에만 손이 머리 위에 있는지 확인
        is_left_hand_above_head = (left_wrist_y < head_y) 
        is_right_hand_above_head = (right_wrist_y < head_y)

        # 결과: 왼쪽 또는 오른쪽 손이 머리 위에 있는지 여부
        is_hands_above_head = is_left_hand_above_head | is_right_hand_above_head
        cur_holding_trackers={key:False for key in self.holding_ids.keys()}
        for idx in range(len(is_hands_above_head)):
            if not is_hands_above_head[idx]:
                continue
            tracker_id = mot_bboxes[idx, 0]
            cur_holding_trackers[tracker_id]=True
            if tracker_id not in self.holding_ids:
                self.holding_ids[tracker_id]=time.time()
                print(f"{str(self.holding_ids)} is holding hand above head")
            else:
                if time.time() - self.holding_ids[tracker_id] >= self.min_hold_time:
                    self.holding_ids = dict()
                    print(f"{tracker_id} target Lock-in")
                    return int(tracker_id)
        for tracker, is_holding in cur_holding_trackers.items():
            if not is_holding:
                del self.holding_ids[tracker]
        return None

class VideoReceiverHandler:
    def __init__(self, input_type, input_source):
        self.input_type = input_type
        self.input_source = input_source
        self.streaming_timer = self.StreamingTimer()
        self.start_time = time.time()  # FPS 측정 시작 시간
        self.fps_measured = False  # 첫 번째 FPS 측정 여부
        self.height = None
        self.width = None
        self.fps = None

    class StreamingTimer:
        def __init__(self):
            self.clear()

        def clear(self):
            self.frame_count = 0
            self.skip_frame_num = 0

        def increment_frame(self):
            self.frame_count += 1

        def increment_skip(self):
            self.skip_frame_num += 1

        def reset(self):
            self.clear()

    def update_fps(self):
        current_time = time.time()
        elapsed_time = current_time - self.start_time
        if elapsed_time >= 5.0:
            self.fps = self.streaming_timer.frame_count / elapsed_time
            skipped_frames = self.streaming_timer.skip_frame_num
            print("5초 요약:")
            print(f" - 건너뛴 프레임 수: {skipped_frames}")
            print(f" - 초당 프레임 수 (FPS): {self.fps:.2f}")
            self.streaming_timer.reset()
            self.start_time = current_time
            self.fps_measured = True  # 첫 번째 FPS 측정 완료

    def capture_video(self, queue, is_prepareing = False):
        assert self.input_type == "file"
        capture = cv2.VideoCapture(self.input_source)
        ret, frame = capture.read()
        if ret:
            self.height, self.width = frame.shape[:2]

        while is_prepareing ^ self.fps_measured:
            if queue.full():
                time.sleep(0.1)
            else:
                ret, frame = capture.read()
                if not ret:
                    return
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                if self.fps_measured:
                    queue.put({"frame": frame_rgb, "inputTime": time.time()})
                self.streaming_timer.increment_frame()
            self.update_fps()

    def receive_frames(self, queue, is_prepareing = False):
        assert self.input_type == "udp"
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        server_socket.bind((self.input_source.split(":")[0], int(self.input_source.split(":")[1])))
        print("서버가 대기 중입니다...")

        try:
            while is_prepareing ^ self.fps_measured:
                packet, addr = server_socket.recvfrom(65507)
                frame = np.frombuffer(packet, dtype=np.uint8)
                img = cv2.imdecode(frame, cv2.IMREAD_COLOR)
                if img is not None:
                    frame_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                    self.height, self.width = frame_rgb.shape[:2]
                    if self.fps_measured and not queue.full():
                        queue.put({"frame": frame_rgb, "inputTime": time.time()})
                    self.streaming_timer.increment_frame()
                else:
                    print("수신한 이미지를 디코딩하는데 실패했습니다.")
                self.update_fps()
        except Exception as e:
            print(f"Error: {e}")
        finally:
            server_socket.close()

    def capture_webcam(self, queue, is_prepareing = False):
        assert self.input_type == "camera"
        cap = cv2.VideoCapture(int(self.input_source))
        self.width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        self.height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        try:
            while is_prepareing ^ self.fps_measured:
                ret, frame = cap.read()
                if not ret:
                    print("웹캠에서 프레임을 읽지 못했습니다.")
                    break

                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                if frame_rgb is not None:
                    if self.fps_measured and not queue.full():
                        queue.put({"frame": frame_rgb, "inputTime": time.time()})
                    self.streaming_timer.increment_frame()
                self.update_fps()
        except Exception as e:
            print(f"Error: {e}")
        finally:
            cap.release()

    def start_video(self, framequeue):
        self.start_time = time.time()  # FPS 측정 시작 시간
        if self.input_type == "file":
            thread = threading.Thread(
                target=self.capture_video, args=(framequeue,))
        elif self.input_type == "camera":
            thread = threading.Thread(
                target=self.capture_webcam, args=(framequeue,))
        elif self.input_type == "udp":
            thread = threading.Thread(
                target=self.receive_frames, args=(framequeue,))
        thread.start()
        time.sleep(1)


    def prepare_video(self, framequeue):
        print("FPS 측정중")
        self.start_time = time.time()  # FPS 측정 시작 시간
        if self.input_type == "file":
            self.capture_video(framequeue, is_prepareing = True)
        elif self.input_type == "camera":
            self.capture_webcam(framequeue, is_prepareing = True)
        elif self.input_type == "udp":
            self.receive_frames(framequeue, is_prepareing = True)
        return self.height, self.width
    

class ResultSendHandler:
    def __init__(self, ip, port):
        """
        초기화 함수
        """
        self.ip = ip
        self.port = port
        self.socket = None
        self.thread = None

    def connect_socket(self):
        """
        UDP 소켓을 초기화하고 연결
        """
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.connect((self.ip, self.port))

    def close_socket(self):
        """
        소켓 연결 종료
        """
        if self.socket:
            self.socket.close()

    def startSending(self, resultqueue):
        """
        결과 전송을 시작하는 함수 (스레드 생성 및 시작)
        """
        # 소켓 연결
        self.connect_socket()

        # 스레드를 시작하여 send_result 실행
        self.thread = threading.Thread(
            target=self.send_result, args=(resultqueue,)
        )
        self.thread.start()

    def stopSending(self):
        """
        스레드 중지 및 소켓 종료
        """
        if self.thread is not None:
            self.thread.join()
        
        # 소켓 종료
        self.close_socket()

    def send_result(self, resultqueue):
        """
        큐에서 결과를 가져와 JSON 형식으로 변환한 뒤 소켓을 통해 전송하는 함수
        """
        while True:
            if resultqueue.empty():
                time.sleep(0.1)
            else:
                result = resultqueue.get()
                try:
                    # dict 형식의 결과를 JSON 문자열로 변환
                    json_result = json.dumps(result)
                    
                    # JSON 문자열을 소켓을 통해 전송
                    self.socket.sendall(json_result.encode('utf-8'))
                    print(f"Sent: {json_result}")
                except Exception as e:
                    print(f"Error sending data: {e}")
                
                # 큐 작업 완료 표시
                resultqueue.task_done()
                time.sleep(0.1)