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

import os
import re
import cv2
import numpy as np
import math
import paddle
import sys
import copy
import threading
import queue
import time
from collections import defaultdict
from datacollector import DataCollector, Result
import socket
import zmq

# add deploy path of PaddleDetection to sys.path
parent_path = os.path.abspath(os.path.join(__file__, *(['..'] * 2)))
sys.path.insert(0, parent_path)

from cfg_utils import argsparser, print_arguments, merge_cfg
from pipe_utils import PipeTimer, StreamingTimer
from pipe_utils import crop_image_with_mot, parse_mot_res
from pipe_utils import PushStream

# from python.infer import Detector
from python.keypoint_infer import KeyPointDetector
from python.keypoint_postprocess import translate_to_ori_images
from python.preprocess import decode_image
from python.visualize import visualize_box_mask, visualize_pose

from pptracking.python.mot_sde_infer import SDE_Detector
from pptracking.python.mot.visualize import plot_tracking_dict
from pptracking.python.mot.utils import flow_statistic

from pphuman.action_utils import HandAboveHeadTracker

from download import auto_download_model


class Pipeline(object):
    """
    Pipeline

    Args:
        args (argparse.Namespace): arguments in pipeline, which contains environment and runtime settings
        cfg (dict): config of models in pipeline
    """

    def __init__(self, args, cfg):
        self.is_video = True
        self.output_dir = args.output_dir
        self.vis_result = cfg['visual']
        self.input, input_type = self._parse_input(args.video_file, args.camera_id, args.udp_config)

        self.predictor = PipePredictor(args, cfg, self.input, input_type)
        if self.is_video:
            self.predictor.set_file_name(self.input)
            
    def is_valid_ip_port(self, input_string):
        # 정규표현식 패턴
        pattern = r"^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9]):([0-9]{1,5})$"
        
        # 패턴 매칭
        match = re.match(pattern, input_string)
        
        # 포트 범위 확인 (0 ~ 65535)
        if match:
            port = int(match.group(4))
            return 0 <= port <= 65535
        return False
    
    def _parse_input(self, video_file, camera_id, udp):
        # parse input as is_video and multi_camera
        input_type = None
        if video_file is not None:
            assert os.path.exists(
                video_file
            ) , "video_file not exists."
            input = video_file
            input_type = "file"

        elif camera_id != -1:
            input = camera_id
            input_type = "camera"

        elif udp is not None:
            assert self.is_valid_ip_port(udp.strip())
            input = udp.strip()
            input_type = "udp"

        else:
            raise ValueError(
                "Illegal Input, please set one of ['video_file', 'camera_id', 'udp_config']"
            )

        return input, input_type

    def run_multithreads(self):
        self.predictor.run()


def get_model_dir(cfg):
    """ 
        Auto download inference model if the model_path is a url link. 
        Otherwise it will use the model_path directly.
    """
    for key in cfg.keys():
        if type(cfg[key]) ==  dict and \
            ("enable" in cfg[key].keys() and cfg[key]['enable']
                or "enable" not in cfg[key].keys()):

            if "model_dir" in cfg[key].keys():
                model_dir = cfg[key]["model_dir"]
                print("모델 다운로드 합니다", model_dir)
                downloaded_model_dir = auto_download_model(model_dir)
                if downloaded_model_dir:
                    model_dir = downloaded_model_dir
                    cfg[key]["model_dir"] = model_dir
                print(key, " model dir: ", model_dir)

        elif key == "MOT":  # for idbased and skeletonbased actions
            model_dir = cfg[key]["model_dir"]
            downloaded_model_dir = auto_download_model(model_dir)
            if downloaded_model_dir:
                model_dir = downloaded_model_dir
                cfg[key]["model_dir"] = model_dir
            print("mot_model_dir model_dir: ", model_dir)


class PipePredictor(object):
    """
    Predictor in single camera
    
    The pipeline for video input: 

        1. Tracking

    Args:
        args (argparse.Namespace): arguments in pipeline, which contains environment and runtime settings
        cfg (dict): config of models in pipeline
        is_video (bool): whether the input is video, default as False
    """

    def __init__(self, args, cfg, input_source, input_type, is_video=True):
        # general module for pphuman and ppvehicle
        self.with_mot = cfg.get('MOT', False)['enable'] if cfg.get(
            'MOT', False) else False
        if self.with_mot:
            print('Multi-Object Tracking enabled')

        self.modebase = {
            "idbased": False,
            "skeletonbased": False
        }

        self.basemode = {
            "MOT": "idbased",
        }

        self.is_video = is_video
        self.cfg = cfg
        self.input_type = input_type
        self.input_source = input_source

        self.output_dir = args.output_dir
        self.draw_center_traj = args.draw_center_traj
        self.secs_interval = args.secs_interval

        self.warmup_frame = self.cfg['warmup_frame']
        self.pipeline_res = Result()
        self.pipe_timer = PipeTimer()
        self.file_name = None
        self.collector = DataCollector()

        # auto download inference model
        get_model_dir(self.cfg)

        kpt_cfg = self.cfg['KPT']
        kpt_model_dir = kpt_cfg['model_dir']
        kpt_batch_size = kpt_cfg['batch_size']
        self.kpt_predictor = KeyPointDetector(
            kpt_model_dir,
            args.device,
            args.run_mode,
            kpt_batch_size,
            args.trt_min_shape,
            args.trt_max_shape,
            args.trt_opt_shape,
            args.trt_calib_mode,
            args.cpu_threads,
            args.enable_mkldnn,
            use_dark=False)

        mot_cfg = self.cfg['MOT']
        model_dir = mot_cfg['model_dir']
        tracker_config = mot_cfg['tracker_config']
        batch_size = mot_cfg['batch_size']
        skip_frame_num = mot_cfg.get('skip_frame_num', -1)
        basemode = self.basemode['MOT']
        self.modebase[basemode] = True
        self.mot_predictor = SDE_Detector(
            model_dir,
            tracker_config,
            args.device,
            args.run_mode,
            batch_size,
            args.trt_min_shape,
            args.trt_max_shape,
            args.trt_opt_shape,
            args.trt_calib_mode,
            args.cpu_threads,
            args.enable_mkldnn,
            skip_frame_num=skip_frame_num,
            draw_center_traj=self.draw_center_traj,
            secs_interval=self.secs_interval,)
        self.handAboveHeadTracker = HandAboveHeadTracker()
        self.target_id = None
        self.streaming_timer = StreamingTimer()

    def set_file_name(self, path):
        if type(path) == int:
            self.file_name = path
        elif path is not None:
            self.file_name = os.path.split(path)[-1]
            if "." in self.file_name:
                self.file_name = self.file_name.split(".")[-2]
        else:
            # use camera id
            self.file_name = None

    def get_result(self):
        return self.collector.get_res()

    def run(self, thread_idx=0):
        self.predict_video(thread_idx=thread_idx)
        self.pipe_timer.info()
        if hasattr(self, 'mot_predictor'):
            self.mot_predictor.det_times.tracking_info(average=True)

    def predict_image(self, input):
        # det
        # det -> attr
        batch_loop_cnt = math.ceil(
            float(len(input)) / self.det_predictor.batch_size)
        self.warmup_frame = min(10, len(input) // 2) - 1
        for i in range(batch_loop_cnt):
            start_index = i * self.det_predictor.batch_size
            end_index = min((i + 1) * self.det_predictor.batch_size, len(input))
            batch_file = input[start_index:end_index]
            batch_input = [decode_image(f, {})[0] for f in batch_file]

            if i > self.warmup_frame:
                self.pipe_timer.total_time.start()
                self.pipe_timer.module_time['det'].start()
            # det output format: class, score, xmin, ymin, xmax, ymax
            det_res = self.det_predictor.predict_image(
                batch_input, visual=False)
            det_res = self.det_predictor.filter_box(det_res,
                                                    self.cfg['crop_thresh'])
            if i > self.warmup_frame:
                self.pipe_timer.module_time['det'].end()
                self.pipe_timer.track_num += len(det_res['boxes'])
            self.pipeline_res.update(det_res, 'det')

            self.pipe_timer.img_num += len(batch_input)
            if i > self.warmup_frame:
                self.pipe_timer.total_time.end()

            if self.cfg['visual']:
                self.visualize_image(batch_file, batch_input, self.pipeline_res)

    def capture_video(self, capture, queue):
        assert self.input_type == "file"
        while (1):
            if queue.full():
                time.sleep(0.1)
            else:
                ret, frame = capture.read()
                if not ret:
                    return
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                queue.put({"frame":frame_rgb,"inputTime":time.time()})

    def receive_frames(self, queue):
        # 소켓 초기화 및 바인딩
        assert self.input_type == "udp"
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        server_socket.bind((self.input_source.split(":")[0], self.input_source.split(":")[1]))
        print(f"서버가 대기 중입니다...")

        frame_count = 0
        skip_frame_num = 0
        start_time = time.time()

        try:
            while True:
                # 프레임 수신
                packet, addr = server_socket.recvfrom(65507)
                frame = np.frombuffer(packet, dtype=np.uint8)
                img = cv2.imdecode(frame, cv2.IMREAD_COLOR)
                frame_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

                if frame_rgb is not None:
                    if not queue.full():
                        queue.put({"frame":frame_rgb,"inputTime":time.time()})
                        frame_count += 1  # 프레임 수 카운트 증가
                    else:
                        skip_frame_num += 1
                else:
                    print("수신한 이미지를 디코딩하는데 실패했습니다.")

                # 5초마다 FPS 출력
                current_time = time.time()
                elapsed_time = current_time - start_time
                if elapsed_time >= 5.0:
                    fps = frame_count / elapsed_time  # FPS 계산
                    print("5초 요약:")
                    print(f" - 수신한 프레임 수: {frame_count}")
                    print(f" - 건너뛴 프레임 수: {skip_frame_num}")
                    print(f" - 초당 프레임 수 (FPS): {fps:.2f}")
                    
                    # 카운터와 타이머 초기화
                    frame_count = 0
                    skip_frame_num = 0
                    start_time = current_time
                
        except Exception as e:
            print(f"Error: {e}")
        finally:
            server_socket.close()

    def capture_webcam(self, queue):
        assert self.input_type == "camera"
        cap = cv2.VideoCapture(int(self.input_source))  # 기본 웹캠
        start_time = time.time()
        self.streaming_timer.clear()
        try:
            while True:
                # 프레임 수신
                ret, frame = cap.read()

                if not ret:
                    print("웹캠에서 프레임을 읽지 못했습니다.")
                    break

                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                if frame_rgb is not None:
                    if not queue.full():
                        queue.put({"frame":frame_rgb,"inputTime":time.time()})
                        self.streaming_timer.frame_count += 1  # 프레임 수 카운트 증가
                    else:
                        self.streaming_timer.skip_frame_num += 1
                else:
                    print("수신한 이미지를 디코딩하는데 실패했습니다.")

                # 5초마다 FPS 출력
                current_time = time.time()
                elapsed_time = current_time - start_time
                if elapsed_time >= 5.0:
                    fps,skiped_frame = self.streaming_timer.info()

                    print("5초 요약:")
                    print(f" - 건너뛴 프레임 수: {skiped_frame}")
                    print(f" - 초당 프레임 수 (FPS): {fps:.2f}")

                    start_time = current_time
                
        except Exception as e:
            print(f"Error: {e}")
        finally:
            cap.release()

    def predict_video(self, thread_idx=0, udp=True):
        # mot
        # mot -> attr

        # Get Video info : resolution, fps, frame count
        width = 1280
        height = 720
        fps = 20

        if self.cfg['visual']:
            video_out_name = 'output' if (
                self.file_name is None or
                type(self.file_name) == int) else self.file_name
            if not os.path.exists(self.output_dir):
                os.makedirs(self.output_dir)
            out_path = os.path.join(self.output_dir, video_out_name + ".mp4")
            fourcc = cv2.VideoWriter_fourcc(* 'mp4v')
            writer = cv2.VideoWriter(out_path, fourcc, fps, (width, height))

        frame_id = 0

        records, center_traj = None, None
        if self.draw_center_traj:
            center_traj = [{}]
        id_set = set()
        interval_id_set = set()
        in_id_list = list()
        out_id_list = list()
        prev_center = dict()
        records = list()

        video_fps = fps
     
        framequeue = queue.Queue(10)
        thread = threading.Thread(
            target=self.capture_webcam, args=(framequeue,))
        # thread = threading.Thread(
        #     target=self.receive_frames, args=(framequeue,))
            
        thread.start()
        time.sleep(1)

        context = zmq.Context()
        socket = context.socket(zmq.PUSH)
        socket.bind("tcp://127.0.0.1:5555")  # 송신자 주소를 지정 (예: 포트 5555)
        socket.setsockopt(zmq.SNDTIMEO, 5000)  # 5초 타임아웃 설정

        # while (not framequeue.empty()):
        while (1):
            if framequeue.empty():
                time.sleep(0.01)
                continue
                
            if frame_id % 10 == 0:
                print('Thread: {}; frame id: {}'.format(thread_idx, frame_id))

            frame_data = framequeue.get()
            frame_rgb = frame_data["frame"]
            frame_time = frame_data["inputTime"]

            if frame_id > self.warmup_frame:
                self.pipe_timer.total_time.start()

            if self.modebase["idbased"] or self.modebase["skeletonbased"]:
                if frame_id > self.warmup_frame:
                    self.pipe_timer.module_time['mot'].start()

                mot_skip_frame_num = self.mot_predictor.skip_frame_num
                reuse_det_result = False
                if mot_skip_frame_num > 1 and frame_id > 0 and frame_id % mot_skip_frame_num > 0:
                    reuse_det_result = True
                res = self.mot_predictor.predict_image(
                    [copy.deepcopy(frame_rgb)],
                    visual=False,
                    reuse_det_result=reuse_det_result,
                    frame_count=frame_id)

                # mot output format: id, class, score, xmin, ymin, xmax, ymax
                mot_res = parse_mot_res(res)
                # print("MOT 결과")
                # print(mot_res)

                # data_bytes = mot_res["boxes"].tobytes()
                # data_shape = mot_res["boxes"].shape
                # data_dtype = str(mot_res["boxes"].dtype)
                # print("데이터 변환")

                # # 배열 데이터와 메타 정보를 함께 전송
                # socket.send_json({'shape': data_shape, 'dtype': data_dtype})
                # print("json 전송")
                # socket.send(data_bytes)
                # print("데이터를 전송했습니다.")

                if frame_id > self.warmup_frame:
                    self.pipe_timer.module_time['mot'].end()
                    self.pipe_timer.track_num += len(mot_res['boxes'])

                if frame_id % 10 == 0:
                    print("Thread: {}; trackid number: {}".format(
                        thread_idx, len(mot_res['boxes'])))

                # flow_statistic only support single class MOT
                boxes, scores, ids = res[0]  # batch size = 1 in MOT
                mot_result = (frame_id + 1, boxes[0], scores[0],
                              ids[0])  # single class
                statistic = flow_statistic(
                    mot_result,
                    self.secs_interval,
                    video_fps,
                    id_set,
                    interval_id_set,
                    in_id_list,
                    out_id_list,
                    prev_center,
                    records,
                    ids2names=self.mot_predictor.pred_config.labels)
                records = statistic['records']
                

                # nothing detected
                if len(mot_res['boxes']) == 0:
                    frame_id += 1
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.img_num += 1
                        self.pipe_timer.total_time.end()
                    if self.cfg['visual']:
                        _, _, fps = self.pipe_timer.get_total_time()
                        im = self.visualize_video(
                            frame_rgb, mot_res, frame_id, fps, records, center_traj,  latency = frame_time)  # visualize
                        if udp:
                            cv2.imshow('Paddle-Pipeline', im)
                            if cv2.waitKey(1) & 0xFF == ord('q'):
                                break
                        else:
                            writer.write(im)
                            if self.file_name is None:  # use camera_id
                                cv2.imshow('Paddle-Pipeline', im)
                                if cv2.waitKey(1) & 0xFF == ord('q'):
                                    break
                    continue

                self.pipeline_res.update(mot_res, 'mot')

                if self.target_id is not None and not np.isin(self.target_id, mot_res["boxes"][:, 0].astype(int)):
                    self.target_id=None
                if self.target_id is None:
                    crop_input, new_bboxes, ori_bboxes = crop_image_with_mot(
                        frame_rgb, mot_res)
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['kpt'].start()
                    kpt_pred = self.kpt_predictor.predict_image(
                    crop_input, visual=False)
                    self.target_id = self.handAboveHeadTracker.update(kpt_pred, mot_res)
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['kpt'].end()
                    
                    if self.cfg['visual']:
                        keypoint_vector, score_vector = translate_to_ori_images(
                            kpt_pred, np.array(new_bboxes))
                        kpt_res = {}
                        kpt_res['keypoint'] = [
                            keypoint_vector.tolist(), score_vector.tolist()
                        ] if len(keypoint_vector) > 0 else [[], []]
                        kpt_res['bbox'] = ori_bboxes
                        if self.target_id is None: 
                            self.pipeline_res.update(kpt_res, 'kpt')
                        else:
                            self.pipeline_res.clear('kpt')

            if frame_id > self.warmup_frame:
                self.pipe_timer.img_num += 1
                self.pipe_timer.total_time.end()
            frame_id += 1

            if self.cfg['visual']:
                _, _, fps = self.pipe_timer.get_total_time()

                im = self.visualize_video(frame_rgb, self.pipeline_res,
                                          frame_id, fps, records, center_traj, latency = frame_time)  # visualize

                if udp:
                    cv2.imshow('Paddle-Pipeline', im)
                    if cv2.waitKey(1) & 0xFF == ord('q'):
                        break
                else:
                    writer.write(im)
                    if self.file_name is None:  # use camera_id
                        cv2.imshow('Paddle-Pipeline', im)
                        if cv2.waitKey(1) & 0xFF == ord('q'):
                            break
        socket.close()
        context.term()

        if self.cfg['visual']:
            writer.release()
            print('save result to {}'.format(out_path))

    def visualize_video(self,
                        image_rgb,
                        result,
                        frame_id,
                        fps,
                        records=None,
                        center_traj=None,
                        latency=None):
        image = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)
        mot_res = copy.deepcopy(result.get('mot'))

        if mot_res is not None:
            ids = mot_res['boxes'][:, 0]
            scores = mot_res['boxes'][:, 2]
            boxes = mot_res['boxes'][:, 3:]
            boxes[:, 2] = boxes[:, 2] - boxes[:, 0]
            boxes[:, 3] = boxes[:, 3] - boxes[:, 1]
        else:
            boxes = np.zeros([0, 4])
            ids = np.zeros([0])
            scores = np.zeros([0])

        # single class, still need to be defaultdict type for ploting
        num_classes = 1
        online_tlwhs = defaultdict(list)
        online_scores = defaultdict(list)
        online_ids = defaultdict(list)
        online_tlwhs[0] = boxes
        online_scores[0] = scores
        online_ids[0] = ids

        if mot_res is not None:
            image = plot_tracking_dict(
                image,
                num_classes,
                online_tlwhs,
                online_ids,
                online_scores,
                frame_id=frame_id,
                fps=fps,
                ids2names=self.mot_predictor.pred_config.labels,
                records=records,
                center_traj=center_traj,
                target_id=self.target_id)

        kpt_res = result.get('kpt')
        if kpt_res is not None:
            image = visualize_pose(
                image,
                kpt_res,
                # visual_thresh=self.cfg['kpt_thresh'],
                returnimg=True)
        
        if latency:
            # 이미지에 텍스트 표시
            # 이미지 처리 후 우상단에 레이턴시 표시
            latency = time.time() - latency
            text = f"Latency: {latency:.2f} sec"

            # 이미지 크기 가져오기
            height, width, _ = image.shape

            # 텍스트 표시할 위치를 오른쪽 상단으로 설정
            font = cv2.FONT_HERSHEY_SIMPLEX
            font_scale = 1
            font_color = (0, 255, 0)  # 녹색
            thickness = 2

            # 텍스트 크기 계산
            (text_width, text_height), _ = cv2.getTextSize(text, font, font_scale, thickness)
            position = (width - text_width - 10, text_height + 10)  # 오른쪽 상단에서 약간의 여백을 줌

            # 이미지에 텍스트 표시
            cv2.putText(image, text, position, font, font_scale, font_color, thickness)

        return image

    def visualize_image(self, im_files, images, result):
        start_idx, boxes_num_i = 0, 0
        det_res = result.get('det')

        for i, (im_file, im) in enumerate(zip(im_files, images)):
            if det_res is not None:
                det_res_i = {}
                boxes_num_i = det_res['boxes_num'][i]
                det_res_i['boxes'] = det_res['boxes'][start_idx:start_idx +
                                                      boxes_num_i, :]
                im = visualize_box_mask(
                    im,
                    det_res_i,
                    labels=['target'],
                    threshold=self.cfg['crop_thresh'])
                im = np.ascontiguousarray(np.copy(im))
                im = cv2.cvtColor(im, cv2.COLOR_RGB2BGR)

            img_name = os.path.split(im_file)[-1]
            if not os.path.exists(self.output_dir):
                os.makedirs(self.output_dir)
            out_path = os.path.join(self.output_dir, img_name)
            cv2.imwrite(out_path, im)
            print("save result to: " + out_path)
            start_idx += boxes_num_i


def main():
    cfg = merge_cfg(FLAGS)  # use command params to update config
    print_arguments(cfg)

    pipeline = Pipeline(FLAGS, cfg)
    pipeline.run_multithreads()


if __name__ == '__main__':
    paddle.enable_static()

    # parse params from command
    parser = argsparser()
    FLAGS = parser.parse_args()
    FLAGS.device = FLAGS.device.upper()
    assert FLAGS.device in ['CPU', 'GPU', 'XPU', 'NPU'
                            ], "device should be CPU, GPU, XPU or NPU"

    main()
