import os
import zmq
import json
import socket
import csv
import re
import time
from datetime import datetime

class RouteDecision:
    def __init__(self):
        """
        RouteDecision 클래스 초기화 및 ZeroMQ 소켓 설정.
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

    def parse_wkt_point(self, wkt):
        """
        WKT 형식의 문자열을 파싱하여 좌표를 추출.
        """
        match = re.match(r'POINT \(([-\d.]+) ([-\d.]+)\)', wkt)
        if match:
            return float(match.group(1)), float(match.group(2))
        return None, None

    def read_position_from_csv(self, csv_file_path):
        """
        CSV 파일에서 위치 데이터를 읽어오는 함수.
        파일에는 WKT, Name, Description 열이 있어야 합니다.
        """
        positions = []
        with open(csv_file_path, mode='r') as file:
            reader = csv.DictReader(file)
            for row in reader:
                lng, lat = self.parse_wkt_point(row['WKT'])
                positions.append({'lat': lat, 'lng': lng})
        return positions

    def receive_data_from_csv(self, csv_file_path):
        """
        CSV 파일에서 데이터를 읽어와 ZeroMQ 메시지 수신 대신 사용.
        """
        print("CSV 파일에서 위치 데이터를 읽어옵니다...")
        positions = self.read_position_from_csv(csv_file_path)
        
        while True:
            for position in positions:
                self.set_position(position)
                self.send_data()
                time.sleep(1)


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
                        self.set_position(data.get("location"))
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
        self.lat = data.get("lat")
        self.lng = data.get("lng")
        print(f"[위치 업데이트] 위치 정보가 lat:{self.lat}, lng:{self.lng} 로 업데이트 되었습니다.")

    def select_file(self):
        """
        사용자로부터 'map_o', 'map_l', 또는 'map_z' 중 하나를 입력받아 해당 파일명을 반환.
        """
        while True:
            choice = input("Choose a map file (map_o, map_l, map_z, multi_map_L): ").strip()
            if choice in ['map_o', 'map_l', 'map_z', 'multi_map_L']:
                return f"{choice}.csv"
            else:
                print("Invalid choice. Please choose 'map_o', 'map_l', or 'map_z'. or 'multi_map_L")

    def start(self, use_csv=False):
        """
        CSV 파일 기반으로 데이터를 수신할지 ZeroMQ로 수신할지 결정.
        """
        if use_csv:
            file_path = self.select_file()
            self.receive_data_from_csv(file_path)
        else:
            self.receive_data()

# 실행 예시
if __name__ == "__main__":
    client = RouteDecision()
    # CSV 파일을 사용해 위치 데이터를 전송하려면 use_csv=True로 설정
    client.start(use_csv=True)
