import os
import zmq
import json
import socket
from datetime import datetime

class RouteDecision:
    def __init__(self):
        """
        DangerDecision 클래스 초기화 및 ZeroMQ 소켓 설정.
        각 트리거 신호를 저장할 변수를 초기화.
        """
        config_path = os.path.join(os.path.dirname(__file__), "../../config.json")
        with open(config_path, "r") as config_file:
            config = json.load(config_file)

        self.context = zmq.Context()
        self.receive_socket = self.context.socket(zmq.PULL)
        self.receive_socket.connect("tcp://127.0.0.1:5570")  

        self.sender_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.target_host = config["APPSERVER_HOST"]
        self.target_port = config["APPSERVER_PORT"]

        self.lat = 0.0
        self.lng = 0.0
        self.last_processed_time = None

    def receive_data(self):
        """
        ZeroMQ를 통해 서버로부터 데이터를 수신하고 메시지 유형과 값에 따라 트리거 설정.
        """
        print("ZeroMQ 서버로부터 메시지 수신을 대기 중...")
        while True:
            try:
                message = self.receive_socket.recv_string()
                data = json.loads(message)
                message_type = data.get("type")
                
                current_time = datetime.now()
                if self.last_processed_time is None or (current_time - self.last_processed_time).total_seconds() >= 1:
                    if message_type == "trackPosition":
                        self.set_position(data)
                        self.last_processed_time = current_time
                    else:
                        print(f"Unknown Message Type: {message_type}")

                    self.send_data()

            except zmq.ZMQError as e:
                print(f"ZeroMQ 에러 발생: {e}")
                break

    def send_data(self):
        """
        현재 위치 정보를 소켓을 통해 앱서버로 전송.
        """
        location_data = {
            "lat": self.lat,
            "lng": self.lng
        }
        message = json.dumps(location_data)

        self.sender_socket.sendto(message.encode('utf-8'), (self.target_host, self.target_port))
        print(f"[데이터 전송] 위치 정보가 앱서버로 전송되었습니다.")
        

    def set_position(self, data):
        self.lat = data.get("location").get("lat")
        self.lng = data.get("location").get("lng")
        print(f"[위치 업데이트] 위치 정보가 lat:{self.lat}, lng:{self.lng} 로 업데이트 되었습니다.")

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
    client = RouteDecision()
    client.start()
