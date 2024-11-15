import os
import zmq
import json
import socket
import csv
import re
import time
import aiofiles
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

        # UDP 소켓 설정
        self.sender_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.target_host = config["APPSERVER_HOST"]
        self.target_port = config["APPSERVER_PORT"]

        # 위치 정보 초기화
        self.lat = 0.0
        self.lng = 0.0
        self.last_processed_time = None
        self.file_path = os.path.join(os.path.dirname(__file__), "user_location_log.csv")

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
                self.set_position(position)  # 위치 업데이트 시 send_data가 호출됨
                time.sleep(1)

    def send_data(self):
        """
        현재 위치 정보를 UDP 소켓을 통해 앱서버로 전송.
        """
        location_data = {
            "lat": self.lat,
            "lng": self.lng
        }
        message = json.dumps(location_data)
        self.sender_socket.sendto(message.encode('utf-8'), (self.target_host, self.target_port))
        print(f"[데이터 전송] 위치 정보가 앱서버로 전송되었습니다.")

    def handle_position_update(self, lat, lng):
        """
        Updates the position with the new latitude and longitude
        received from the server.
        """
        self.set_position({"lat": lat, "lng": lng})
        print(f"[Position Update] New position received: lat={lat}, lng={lng}")

    def set_position(self, data):
        """
        위치 정보를 업데이트하고, 업데이트가 발생할 때마다 UDP로 데이터를 전송.
        """
        self.lat = data.get("lat")
        self.lng = data.get("lng")
        print(f"[위치 업데이트] 위치 정보가 lat:{self.lat}, lng:{self.lng}로 업데이트 되었습니다.")
        
        with open(self.file_path, mode="a") as file:
            file.write(f"{datetime.now().isoformat()},\"{self.lat}, {self.lng}\"\n")
            file.flush()  # 버퍼를 강제로 비워 파일에 즉시 쓰기


        # 위치가 업데이트될 때마다 UDP로 전송
        self.send_data()

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
            self.receive_data_set_positionfrom_csv(file_path)
        else:
            print("ZeroMQ 데이터 수신 기능은 현재 비활성화 상태입니다.")
            # ZeroMQ 데이터 수신 대신 다른 방법으로 데이터를 받을 경우 추가

# 실행 예시
if __name__ == "__main__":
    client = RouteDecision()
    # CSV 파일을 사용해 위치 데이터를 전송하려면 use_csv=True로 설정
    client.start(use_csv=False)
