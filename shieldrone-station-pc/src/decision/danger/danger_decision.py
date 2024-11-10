import zmq
import json
from datetime import datetime

class DangerDecision:
    def __init__(self):
        """
        DangerDecision 클래스 초기화 및 ZeroMQ 소켓 설정.
        각 트리거 신호를 저장할 변수를 초기화.
        """
        self.context = zmq.Context()
        self.socket = self.context.socket(zmq.PULL)
        self.socket.connect("tcp://127.0.0.1:5560")  

        self.pulse_flag_trigger = False
        self.db_flag_trigger = False

    def receive_data(self):
        """
        ZeroMQ를 통해 서버로부터 데이터를 수신하고 메시지 유형과 값에 따라 트리거 설정.
        """
        print("ZeroMQ 서버로부터 메시지 수신을 대기 중...")
        while True:
            try:
                message = self.socket.recv_string()
                data = json.loads(message)
                message_type = data.get("type")

                if message_type == "sendPulseFlag":
                    self.set_pulse_flag_trigger(data)
                elif message_type == "sendDbFlag":
                    self.set_db_flag_trigger(data)
                else:
                    print(f"Unknown Message Type: {message_type}")

                self.check_condition()

            except zmq.ZMQError as e:
                print(f"ZeroMQ 에러 발생: {e}")
                break

    def set_pulse_flag_trigger(self, data):
        self.pulse_flag_trigger = data.get("pulseFlag")
        print("[트리거 설정] sendPulseFlag 트리거가 활성화되었습니다.")

    def set_db_flag_trigger(self, data):
        self.db_flag_trigger = data.get("dbFlag")
        print("[트리거 설정] sendDbFlag 트리거가 활성화되었습니다.")

    def check_condition(self):

        if all([self.pulse_flag_trigger, self.db_flag_trigger]):
            print("[경고] 조건이 충족되었습니다. 위험 상황 처리 로직을 수행합니다.")
            # TODO: 위험 상황 처리 로직 수행
        
            self.reset_triggers()

    def reset_triggers(self):
        """
        모든 트리거 변수를 False로 초기화하여 다시 대기 상태로 전환.
        """
        self.pulse_flag_trigger = False
        self.db_flag_trigger = False
        print("[트리거 초기화] 모든 트리거가 초기화되었습니다.")

    def start(self):
        self.receive_data()

# 실행 예시
if __name__ == "__main__":
    client = DangerDecision()
    client.start()
