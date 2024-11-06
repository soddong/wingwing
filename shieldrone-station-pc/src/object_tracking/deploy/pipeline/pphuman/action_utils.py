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
import time

class KeyPointSequence(object):
    def __init__(self, max_size=100):
        self.frames = 0
        self.kpts = []
        self.bboxes = []
        self.max_size = max_size

    def save(self, kpt, bbox):
        self.kpts.append(kpt)
        self.bboxes.append(bbox)
        self.frames += 1
        if self.frames == self.max_size:
            return True
        return False


class KeyPointBuff(object):
    def __init__(self, max_size=100):
        self.flag_track_interrupt = False
        self.keypoint_saver = dict()
        self.max_size = max_size
        self.id_to_pop = set()
        self.flag_to_pop = False

    def get_state(self):
        return self.flag_to_pop

    def update(self, kpt_res, mot_res):
        kpts = kpt_res.get('keypoint')[0]
        bboxes = kpt_res.get('bbox')
        mot_bboxes = mot_res.get('boxes')
        updated_id = set()

        for idx in range(len(kpts)):
            tracker_id = mot_bboxes[idx, 0]
            updated_id.add(tracker_id)

            kpt_seq = self.keypoint_saver.get(tracker_id,
                                              KeyPointSequence(self.max_size))
            is_full = kpt_seq.save(kpts[idx], bboxes[idx])
            self.keypoint_saver[tracker_id] = kpt_seq

            #Scene1: result should be popped when frames meet max size
            if is_full:
                self.id_to_pop.add(tracker_id)
                self.flag_to_pop = True

        #Scene2: result of a lost tracker should be popped
        interrupted_id = set(self.keypoint_saver.keys()) - updated_id
        if len(interrupted_id) > 0:
            self.flag_to_pop = True
            self.id_to_pop.update(interrupted_id)

    def get_collected_keypoint(self):
        """
            Output (List): List of keypoint results for Skeletonbased Recognition task, where 
                           the format of each element is [tracker_id, KeyPointSequence of tracker_id]
        """
        output = []
        for tracker_id in self.id_to_pop:
            output.append([tracker_id, self.keypoint_saver[tracker_id]])
            del (self.keypoint_saver[tracker_id])
        self.flag_to_pop = False
        self.id_to_pop.clear()
        return output


class ActionVisualHelper(object):
    def __init__(self, frame_life=20):
        self.frame_life = frame_life
        self.action_history = {}

    def get_visualize_ids(self):
        id_detected = self.check_detected()
        return id_detected

    def check_detected(self):
        id_detected = set()
        deperate_id = []
        for mot_id in self.action_history:
            self.action_history[mot_id]["life_remain"] -= 1
            if int(self.action_history[mot_id]["class"]) == 0:
                id_detected.add(mot_id)
            if self.action_history[mot_id]["life_remain"] == 0:
                deperate_id.append(mot_id)
        for mot_id in deperate_id:
            del (self.action_history[mot_id])
        return id_detected

    def update(self, action_res_list):
        for mot_id, action_res in action_res_list:
            if mot_id in self.action_history:
                if int(action_res["class"]) != 0 and int(self.action_history[
                        mot_id]["class"]) == 0:
                    continue
            action_info = self.action_history.get(mot_id, {})
            action_info["class"] = action_res["class"]
            action_info["life_remain"] = self.frame_life
            self.action_history[mot_id] = action_info

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
                    print(f"{tracker_id} is holding hand 5")
                    return tracker_id
        for tracker, is_holding in cur_holding_trackers.items():
            if not is_holding:
                del self.holding_ids[tracker]
        return None