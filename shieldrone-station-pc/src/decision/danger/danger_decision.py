import zmq
import json
import time
from datetime import datetime

class DangerDecision:
    def __init__(self):
        """
        DangerDecision 클래스 초기화 및 ZeroMQ 소켓 설정.
        각 트리거 신호를 저장할 변수를 초기화.
        """
        self.context = zmq.Context()
        self.socket_sensor = self.context.socket(zmq.SUB)
        self.socket_sensor.connect("tcp://127.0.0.1:5560")
        self.socket_sensor.setsockopt_string(zmq.SUBSCRIBE, "")
        self.socket_sensor.setsockopt(zmq.RCVTIMEO, 5000) 

        self.socket_camera = self.context.socket(zmq.SUB)
        self.socket_camera.connect("tcp://127.0.0.1:5580")
        self.socket_camera.setsockopt_string(zmq.SUBSCRIBE, "")
        self.socket_camera.setsockopt(zmq.RCVTIMEO, 5000) 

        self.socket_flag = self.context.socket(zmq.PUB)
        self.socket_flag.bind("tcp://127.0.0.1:5590")

        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        self.camera_flag_trigger = False

        # Poller 설정
        self.poller = zmq.Poller()
        self.poller.register(self.socket_sensor, zmq.POLLIN)
        self.poller.register(self.socket_camera, zmq.POLLIN)

        # 트리거와 타임스탬프 변수 초기화
        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        self.camera_flag_trigger = False
        self.pulse_flag_time = 0
        self.db_flag_time = 0
        self.camera_flag_time = 0

        # 타임아웃 설정 (30초)
        self.trigger_timeout = 30

    def receive_data(self):
        """
        ZeroMQ를 통해 두 소켓에서 동시에 메시지를 수신하고, 메시지 유형과 값에 따라 트리거 설정.
        """
        print("ZeroMQ 서버로부터 메시지 수신을 대기 중...")
        while True:
            try:
                # 두 소켓에서 메시지 수신 대기
                events = dict(self.poller.poll(timeout=100))  # 1초 대기 후 넘어감

                # socket_sensor에서 메시지 수신
                if self.socket_sensor in events:
                    message = self.socket_sensor.recv_string()
                    data = json.loads(message)
                    message_type = data.get("type")

                    if message_type == "sendPulseFlag":
                        self.set_pulse_flag_trigger(data)
                    elif message_type == "sendDbFlag":
                        self.set_db_flag_trigger(data)
                    else:
                        print(f"Unknown Message Type from socket_sensor: {message_type}")

                # socket_camera에서 메시지 수신
                if self.socket_camera in events:
                    message = self.socket_camera.recv_string()
                    data = json.loads(message)
                    message_type = data.get("type")

                    if message_type == "sendCameraFlag":
                        self.set_camera_flag_trigger(data)
                    else:
                        print(f"Unknown Message Type from socket_camera: {message_type}")

                self.check_condition()

            except zmq.ZMQError as e:
                print(f"ZeroMQ 에러 발생: {e}")
                break

    def set_pulse_flag_trigger(self, data):
        current_time = time.time()
        new_pulse_flag = data.get("pulseFlag")
        
        if self.pulse_flag_trigger:
            if current_time - self.pulse_flag_time > self.trigger_timeout:
                self.pulse_flag_trigger = new_pulse_flag
                self.pulse_flag_time = current_time
                print("[트리거 재설정] sendPulseFlag 트리거가 재설정되었습니다.")
        else:
            if new_pulse_flag:
                self.pulse_flag_trigger = True
                self.pulse_flag_time = current_time
                print("[트리거 설정] sendPulseFlag 트리거가 발동되었습니다.")

    def set_db_flag_trigger(self, data):
        current_time = time.time()
        new_db_flag = data.get("dbFlag")
        
        if self.db_flag_trigger:
            if current_time - self.db_flag_time > self.trigger_timeout:
                self.db_flag_trigger = new_db_flag
                self.db_flag_time = current_time
                print("[트리거 재설정] sendDbFlag 트리거가 재설정되었습니다.")
        else:
            if new_db_flag:
                self.db_flag_trigger = True
                self.db_flag_time = current_time
                print("[트리거 설정] sendDbFlag 트리거가 발동되었습니다.")

    def set_camera_flag_trigger(self, data):
        current_time = time.time()
        new_camera_flag = data.get("warningFlag")
        
        if self.camera_flag_trigger:
            if current_time - self.camera_flag_time > self.trigger_timeout:
                self.camera_flag_trigger = new_camera_flag
                self.camera_flag_time = current_time
                print("[트리거 재설정] sendCameraFlag 트리거가 재설정되었습니다.")
        else:
            if new_camera_flag:
                self.camera_flag_trigger = True
                self.camera_flag_time = current_time
                self.send_object_signal()
                print("[트리거 설정] sendCameraFlag 트리거가 발동되었습니다.")

    def check_condition(self):
        pulse_weight = 1/3
        db_weight = 1/3
        camera_weight = 1/3

        active_triggers = (self.pulse_flag_trigger * pulse_weight + 
                        self.db_flag_trigger * db_weight + 
                        self.camera_flag_trigger * camera_weight)

        if active_triggers >= 0.5:
            print("[경고] 조건이 충족되었습니다. 위험 상황 처리 로직을 수행합니다.")
            self.send_danger_signal()
            self.reset_triggers()


    def send_danger_signal(self):
        """
        위험 상황 발생 시 5590 포트로 sendWarningFlag 메시지를 전송합니다.
        """
        message = json.dumps({
            "type": "sendWarningFlag",
            "time": datetime.now().isoformat(),
            "warningFlag": True
        })
        self.socket_flag.send_string(message)
        print("[위험 경고 전송] 경고 메시지가 5590 포트로 전송되었습니다.")

    def send_object_signal(self):
        """
        객체 감지시 5590 포트로 sendObectFlag 메시지를 전송합니다.
        """
        message = json.dumps({
            "type": "sendObjectFlag",
            "time": datetime.now().isoformat(),
            "objectFlag": True
        })
        self.socket_flag.send_string(message)
        print("[위험 경고 전송] 경고 메시지가 5590 포트로 전송되었습니다.")

    def reset_triggers(self):
        """
        모든 트리거 변수를 False로 초기화하여 다시 대기 상태로 전환.
        """
        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        self.camera_flag_trigger = False
        print("[트리거 초기화] 모든 트리거가 초기화되었습니다.")

    def start(self):
        self.receive_data()

# 실행 예시
if __name__ == "__main__":
    client = DangerDecision()
    client.start()
