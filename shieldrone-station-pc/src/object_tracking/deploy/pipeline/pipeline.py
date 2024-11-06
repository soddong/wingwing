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
from pipe_utils import PipeTimer
from pipe_utils import crop_image_with_det, crop_image_with_mot, parse_mot_res, parse_mot_keypoint
from pipe_utils import PushStream

from python.infer import Detector
from python.keypoint_infer import KeyPointDetector
from python.keypoint_postprocess import translate_to_ori_images
from python.preprocess import decode_image
from python.visualize import visualize_box_mask, visualize_attr, visualize_pose

from pptracking.python.mot_sde_infer import SDE_Detector
from pptracking.python.mot.visualize import plot_tracking_dict
from pptracking.python.mot.utils import flow_statistic

from pphuman.attr_infer import AttrDetector
from pphuman.reid import ReID
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
        self.illegal_parking_time = -1
        reid_cfg = cfg.get('REID', False)
        self.enable_mtmct = reid_cfg['enable'] if reid_cfg else False
        self.is_video = False
        self.output_dir = args.output_dir
        self.vis_result = cfg['visual']
        self.input = self._parse_input(None, None,
                                       args.video_file, None,
                                       args.camera_id, args.rtsp)

        self.predictor = PipePredictor(args, cfg, self.is_video)
        if self.is_video:
            self.predictor.set_file_name(self.input)

    def _parse_input(self, image_file, image_dir, video_file, video_dir,
                     camera_id, rtsp):
        # parse input as is_video and multi_camera

        if video_file is not None:
            assert os.path.exists(
                video_file
            ) or 'rtsp' in video_file, "video_file not exists and not an rtsp site."
            input = video_file
            self.is_video = True

        elif rtsp is not None:
            if len(rtsp) > 1:
                rtsp = [rtsp_item for rtsp_item in rtsp if 'rtsp' in rtsp_item]
                input = rtsp
            else:
                input = rtsp[0]
            self.is_video = True

        elif camera_id != -1:
            input = camera_id
            self.is_video = True

        else:
            raise ValueError(
                "Illegal Input, please set one of ['video_file', 'camera_id', 'image_file', 'image_dir']"
            )

        return input

    def run_multithreads(self):
        self.predictor.run(self.input)


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
    
    The pipeline for image input: 

        1. Detection
        2. Detection -> Attribute

    The pipeline for video input: 

        1. Tracking
        2. Tracking -> Attribute
        3. Tracking -> KeyPoint -> SkeletonAction Recognition
        4. VideoAction Recognition

    Args:
        args (argparse.Namespace): arguments in pipeline, which contains environment and runtime settings
        cfg (dict): config of models in pipeline
        is_video (bool): whether the input is video, default as False
        multi_camera (bool): whether to use multi camera in pipeline, 
            default as False
    """

    def __init__(self, args, cfg, is_video=True, multi_camera=False):
        # general module for pphuman and ppvehicle
        self.with_mot = cfg.get('MOT', False)['enable'] if cfg.get(
            'MOT', False) else False
        self.with_human_attr = cfg.get('ATTR', False)['enable'] if cfg.get(
            'ATTR', False) else False
        if self.with_mot:
            print('Multi-Object Tracking enabled')
        if self.with_human_attr:
            print('Human Attribute Recognition enabled')

        self.with_mtmct = cfg.get('REID', False)['enable'] if cfg.get(
            'REID', False) else False

        if self.with_mtmct:
            print("MTMCT enabled")

        self.modebase = {
            "framebased": False,
            "videobased": False,
            "idbased": False,
            "skeletonbased": False
        }

        self.basemode = {
            "MOT": "idbased",
            "ATTR": "idbased",
            "VIDEO_ACTION": "videobased",
            "SKELETON_ACTION": "skeletonbased",
            "ID_BASED_DETACTION": "idbased",
            "ID_BASED_CLSACTION": "idbased",
            "REID": "idbased",
            "VEHICLE_PLATE": "idbased",
            "VEHICLE_ATTR": "idbased",
            "VEHICLE_PRESSING": "idbased",
            "VEHICLE_RETROGRADE": "idbased",
        }

        self.is_video = is_video
        self.cfg = cfg

        self.output_dir = args.output_dir
        self.draw_center_traj = args.draw_center_traj
        self.secs_interval = args.secs_interval

        self.warmup_frame = self.cfg['warmup_frame']
        self.pipeline_res = Result()
        self.pipe_timer = PipeTimer()
        self.file_name = None
        self.collector = DataCollector()

        self.pushurl = args.pushurl

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
        if self.with_human_attr:
            attr_cfg = self.cfg['ATTR']
            basemode = self.basemode['ATTR']
            self.modebase[basemode] = True
            self.attr_predictor = AttrDetector.init_with_cfg(args, attr_cfg)

        if self.with_mtmct:
            reid_cfg = self.cfg['REID']
            basemode = self.basemode['REID']
            self.modebase[basemode] = True
            self.reid_predictor = ReID.init_with_cfg(args, reid_cfg)

        if self.with_mot or self.modebase["idbased"] or self.modebase[
                "skeletonbased"]:
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

    def run(self, input, thread_idx=0):
        self.predict_video(input, thread_idx=thread_idx)
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

            if self.with_human_attr:
                crop_inputs = crop_image_with_det(batch_input, det_res)
                attr_res_list = []

                if i > self.warmup_frame:
                    self.pipe_timer.module_time['attr'].start()

                for crop_input in crop_inputs:
                    attr_res = self.attr_predictor.predict_image(
                        crop_input, visual=False)
                    attr_res_list.extend(attr_res['output'])

                if i > self.warmup_frame:
                    self.pipe_timer.module_time['attr'].end()

                attr_res = {'output': attr_res_list}
                self.pipeline_res.update(attr_res, 'attr')

            self.pipe_timer.img_num += len(batch_input)
            if i > self.warmup_frame:
                self.pipe_timer.total_time.end()

            if self.cfg['visual']:
                self.visualize_image(batch_file, batch_input, self.pipeline_res)

    def capturevideo(self, capture, queue):
        frame_id = 0
        while (1):
            if queue.full():
                time.sleep(0.1)
            else:
                ret, frame = capture.read()
                if not ret:
                    return
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                queue.put(frame_rgb)

    def receive_frames(self, queue):
        # 소켓 초기화 및 바인딩
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        server_socket.bind(("0.0.0.0", 65432))
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
                        queue.put(frame_rgb)
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
        cap = cv2.VideoCapture(0)  # 기본 웹캠
        frame_count = 0
        skip_frame_num = 0
        start_time = time.time()

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
                        queue.put(frame_rgb)
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
            cap.release()

    def predict_video(self, video_file, thread_idx=0, udp=True):
        # mot
        # mot -> attr
        # mot -> pose -> action
        if not udp:
            capture = cv2.VideoCapture(video_file)

        # Get Video info : resolution, fps, frame count
        width = 640
        height = 640
        fps = 20
        frame_count = 0
        if not udp:
            width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
            height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
            fps = int(capture.get(cv2.CAP_PROP_FPS))
            frame_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT))
        print("video fps: %d, frame_count: %d" % (fps, frame_count))

        if len(self.pushurl) > 0:
            video_out_name = 'output' if self.file_name is None else self.file_name
            pushurl = os.path.join(self.pushurl, video_out_name)
            print("the result will push stream to url:{}".format(pushurl))
            pushstream = PushStream(pushurl)
            pushstream.initcmd(fps, width, height)
        elif self.cfg['visual']:
            video_out_name = 'output' if (
                self.file_name is None or
                type(self.file_name) == int) else self.file_name
            if type(video_file) == str and "rtsp" in video_file:
                video_out_name = video_out_name + "_t" + str(thread_idx).zfill(
                    2) + "_rtsp"
            if not os.path.exists(self.output_dir):
                os.makedirs(self.output_dir)
            out_path = os.path.join(self.output_dir, video_out_name + ".mp4")
            fourcc = cv2.VideoWriter_fourcc(* 'mp4v')
            writer = cv2.VideoWriter(out_path, fourcc, fps, (width, height))

        frame_id = 0

        entrance, records, center_traj = None, None, None
        if self.draw_center_traj:
            center_traj = [{}]
        id_set = set()
        interval_id_set = set()
        in_id_list = list()
        out_id_list = list()
        prev_center = dict()
        records = list()

        video_fps = fps

        video_action_imgs = []
     
        framequeue = queue.Queue(60)

        if udp:
            thread = threading.Thread(
                target=self.capture_webcam, args=(framequeue,))
            # thread = threading.Thread(
            #     target=self.receive_frames, args=(framequeue,))
        else:
            thread = threading.Thread(
            target=self.capturevideo, args=(capture, framequeue))
            
        thread.start()
        time.sleep(1)

        context = zmq.Context()
        socket = context.socket(zmq.PUSH)
        socket.bind("tcp://127.0.0.1:5555")  # 송신자 주소를 지정 (예: 포트 5555)
        socket.setsockopt(zmq.SNDTIMEO, 5000)  # 5초 타임아웃 설정

        print(self.cfg)
        # while (not framequeue.empty()):
        while (1):
            if framequeue.empty():
                time.sleep(0.1)
                continue
                
            if frame_id % 10 == 0:
                print('Thread: {}; frame id: {}'.format(thread_idx, frame_id))

            frame_rgb = framequeue.get()
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
                    False,
                    False,
                    "custom",
                    video_fps,
                    entrance,
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
                            frame_rgb, mot_res, self.collector, frame_id, fps,
                            entrance, records, center_traj)  # visualize
                        if len(self.pushurl) > 0:
                            pushstream.pipe.stdin.write(im.tobytes())
                        else:
                            writer.write(im)
                            if self.file_name is None:  # use camera_id
                                cv2.imshow('Paddle-Pipeline', im)
                                if cv2.waitKey(1) & 0xFF == ord('q'):
                                    break
                    continue

                self.pipeline_res.update(mot_res, 'mot')
                crop_input, new_bboxes, ori_bboxes = crop_image_with_mot(
                    frame_rgb, mot_res)
                if frame_id > self.warmup_frame:
                    self.pipe_timer.module_time['kpt'].start()
                kpt_pred = self.kpt_predictor.predict_image(
                crop_input, visual=False)
                self.handAboveHeadTracker.update(kpt_pred, mot_res)
                keypoint_vector, score_vector = translate_to_ori_images(
                    kpt_pred, np.array(new_bboxes))
                kpt_res = {}
                kpt_res['keypoint'] = [
                    keypoint_vector.tolist(), score_vector.tolist()
                ] if len(keypoint_vector) > 0 else [[], []]
                kpt_res['bbox'] = ori_bboxes
                if frame_id > self.warmup_frame:
                    self.pipe_timer.module_time['kpt'].end()

                self.pipeline_res.update(kpt_res, 'kpt')

                if self.with_human_attr:
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['attr'].start()
                    attr_res = self.attr_predictor.predict_image(
                        crop_input, visual=False)
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['attr'].end()
                    self.pipeline_res.update(attr_res, 'attr')


                if self.with_mtmct and frame_id % 10 == 0:
                    crop_input, img_qualities, rects = self.reid_predictor.crop_image_with_mot(
                        frame_rgb, mot_res)
                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['reid'].start()
                    reid_res = self.reid_predictor.predict_batch(crop_input)

                    if frame_id > self.warmup_frame:
                        self.pipe_timer.module_time['reid'].end()

                    reid_res_dict = {
                        'features': reid_res,
                        "qualities": img_qualities,
                        "rects": rects
                    }
                    self.pipeline_res.update(reid_res_dict, 'reid')
                else:
                    self.pipeline_res.clear('reid')
            self.collector.append(frame_id, self.pipeline_res)

            if frame_id > self.warmup_frame:
                self.pipe_timer.img_num += 1
                self.pipe_timer.total_time.end()
            frame_id += 1

            if self.cfg['visual']:
                _, _, fps = self.pipe_timer.get_total_time()

                im = self.visualize_video(frame_rgb, self.pipeline_res,
                                          self.collector, frame_id, fps,
                                          entrance, records, center_traj)  # visualize
                if len(self.pushurl) > 0:
                    pushstream.pipe.stdin.write(im.tobytes())
                else:
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

        if self.cfg['visual'] and len(self.pushurl) == 0:
            writer.release()
            print('save result to {}'.format(out_path))

    def visualize_video(self,
                        image_rgb,
                        result,
                        collector,
                        frame_id,
                        fps,
                        entrance=None,
                        records=None,
                        center_traj=None,
                        do_illegal_parking_recognition=False,
                        illegal_parking_dict=None):
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
                center_traj=center_traj)

        human_attr_res = result.get('attr')
        if human_attr_res is not None:
            boxes = mot_res['boxes'][:, 1:]
            human_attr_res = human_attr_res['output']
            image = visualize_attr(image, human_attr_res, boxes)
            image = np.array(image)

        if mot_res is not None:
            vehicleplate = False
            plates = []
            for trackid in mot_res['boxes'][:, 0]:
                plate = collector.get_carlp(trackid)
                if plate != None:
                    vehicleplate = True
                    plates.append(plate)
                else:
                    plates.append("")
                kpt_res = result.get('kpt')

        kpt_res = result.get('kpt')
        if kpt_res is not None:
            image = visualize_pose(
                image,
                kpt_res,
                # visual_thresh=self.cfg['kpt_thresh'],
                returnimg=True)

        return image

    def visualize_image(self, im_files, images, result):
        start_idx, boxes_num_i = 0, 0
        det_res = result.get('det')
        human_attr_res = result.get('attr')

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
            if human_attr_res is not None:
                human_attr_res_i = human_attr_res['output'][start_idx:start_idx
                                                            + boxes_num_i]
                im = visualize_attr(im, human_attr_res_i, det_res_i['boxes'])

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
