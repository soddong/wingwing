import json
import time
from datetime import datetime

class DangerDecision:
    def __init__(self):
        """
        DangerDecision 클래스 초기화 및 카메라 소켓 설정.
        """
        
        # 트리거와 타임스탬프 변수 초기화
        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        self.camera_flag_trigger = False
        self.pulse_flag_time = 0
        self.db_flag_time = 0
        self.camera_flag_time = 0

        # 타임아웃 설정 (5초)
        self.trigger_timeout = 5

    def set_pulse_flag_trigger(self, pulse_flag):
        """
        심박 급등 시그널을 설정합니다.
        """
        current_time = time.time()
        if self.pulse_flag_trigger and current_time - self.pulse_flag_time > self.trigger_timeout:
            self.pulse_flag_trigger = pulse_flag
            self.pulse_flag_time = current_time
            print("[트리거 재설정] sendPulseFlag 트리거가 재설정되었습니다.")
        elif pulse_flag:
            self.pulse_flag_trigger = True
            self.pulse_flag_time = current_time
            print("[트리거 설정] sendPulseFlag 트리거가 발동되었습니다.")

    def set_db_flag_trigger(self, db_flag):
        """
        음성 위험 신호를 설정합니다.
        """
        current_time = time.time()
        if self.db_flag_trigger and current_time - self.db_flag_time > self.trigger_timeout:
            self.db_flag_trigger = db_flag
            self.db_flag_time = current_time
            print("[트리거 재설정] sendDbFlag 트리거가 재설정되었습니다.")
        elif db_flag:
            self.db_flag_trigger = True
            self.db_flag_time = current_time
            print("[트리거 설정] sendDbFlag 트리거가 발동되었습니다.")

    def set_camera_flag_trigger(self, camera_flag):
        """
        카메라에서 감지된 위험 신호를 설정합니다.
        """
        current_time = time.time()
        if self.camera_flag_trigger and current_time - self.camera_flag_time > self.trigger_timeout:
            self.camera_flag_trigger = camera_flag
            self.camera_flag_time = current_time
            print("[트리거 재설정] sendCameraFlag 트리거가 재설정되었습니다.")
        elif camera_flag:
            self.camera_flag_trigger = True
            self.camera_flag_time = current_time
            
    def check_condition(self):
        """
        각 트리거를 기반으로 위험 상황 조건을 검사하고, 위험이 감지되면 True를 반환합니다.
        """
        pulse_weight = 1/3
        db_weight = 1/3
        camera_weight = 0

        active_triggers = (self.pulse_flag_trigger * pulse_weight +
                           self.db_flag_trigger * db_weight +
                           self.camera_flag_trigger * camera_weight)

        print(f"[경고] 조건 체크 결과: {active_triggers}")
        if active_triggers >= 0.5:
            print("[경고] 조건이 충족되었습니다. 위험 상황 처리 로직을 수행합니다.")
            self.reset_triggers()
            return True
        return False

    def reset_triggers(self):
        """
        모든 트리거를 False로 초기화하여 다시 대기 상태로 전환합니다.
        """
        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        self.camera_flag_trigger = False
        print("[트리거 초기화] 모든 트리거가 초기화되었습니다.")
