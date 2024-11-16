import os
import json
import socket
import csv
import re
import time
import numpy as np
from datetime import datetime
from filterpy.kalman import KalmanFilter

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
        self.dest_lat = 0.0
        self.dest_lng = 0.0
        self.last_processed_time = None
        self.file_path_filtered = os.path.join(os.path.dirname(__file__), "filtered_location_log.csv")
        self.file_path_origin = os.path.join(os.path.dirname(__file__), "user_location_log.csv")

        # 칼만 필터 설정
        self.kf = KalmanFilter(dim_x=4, dim_z=2)
        dt = 1.0
        self.kf.F = np.array([[1, dt, 0,  0],
                              [0,  1, 0,  0],
                              [0,  0, 1, dt],
                              [0,  0, 0,  1]])
        self.kf.H = np.array([[1, 0, 0, 0],
                              [0, 0, 1, 0]])
        
        # 공분산 및 노이즈 설정
        self.kf.P *= 1000.0
        self.kf.R = np.array([[5, 0], [0, 5]])
        self.kf.Q = np.eye(4) * 0.1

        # 초기값 설정 여부를 추적하는 플래그
        self.initialized = False

    def send_initial_trigger(self):
        """
        최초 소켓 연결시, 앱서버에 startFlag 전송
        """
        data = {
            "startFlag" : True
        }
        message = json.dumps(data)
        self.sender_socket.sendto(message.encode('utf-8'), (self.target_host, self.target_port))  

    def handle_position_update(self, lat, lng, dest_lat, dest_lng):
        """
        위치 정보를 업데이트 하기 전, 첫 위치를 초기값으로 설정하고 이후에는 칼만 필터(보정) 적용
        """
        if not self.initialized:
            # 첫 번째 위치 업데이트 시 초기 상태 설정
            self.kf.x = np.array([float(lat), 0, float(lng), 0])  # 초기 위치 설정
            self.initialized = True
            print(f"[초기화] 초기 위치 설정됨: lat={lat}, lng={lng}")
        else:
            # 이후 위치 업데이트 시 칼만 필터로 보정 수행
            self.kf.predict()
            self.kf.update([float(lat), float(lng)])
            print(f"[보정] 측정값 lat={lat}, lng={lng} -> 보정값 lat={self.kf.x[0]}, lng={self.kf.x[2]}")

        with open(self.file_path_origin, mode="a") as file:
            file.write(f"{datetime.now().isoformat()},\"{self.lat}, {self.lng}\"\n")
            file.flush()
        
        # 필터링된 위치로 위치 업데이트
        filtered_lat = self.kf.x[0]
        filtered_lng = self.kf.x[2]
        self.dest_lat = dest_lat
        self.dest_lng = dest_lng

        with open(self.file_path_filtered, mode="a") as file:
            file.write(f"{datetime.now().isoformat()},\"{filtered_lat}, {filtered_lng}\"\n")
            file.flush()

        self.set_position({"lat": filtered_lat, "lng": filtered_lng})

    def set_position(self, data):
        """
        위치 정보를 업데이트하고, 업데이트가 발생할 때마다 UDP로 데이터를 전송.
        """
        
        self.lat = data.get("lat")
        self.lng = data.get("lng")
        print(f"[위치 업데이트] 필터링된 위치 정보는 lat:{self.lat}, lng:{self.lng} 입니다.")

        # 위치가 업데이트될 때마다 UDP로 전송
        self.send_data()

    def send_data(self):
        """
        현재 위치 정보를 UDP 소켓을 통해 앱서버로 전송.
        """
        location_data = {
            "location" : {
                "lat": self.lat,
                "lng": self.lng
            },
            "dest_location" : {
                "lat": self.dest_lat,
                "lng": self.dest_lng
            },
        }
        message = json.dumps(location_data)
        self.sender_socket.sendto(message.encode('utf-8'), (self.target_host, self.target_port))
        print(f"[데이터 전송] 위치 정보가 앱서버로 전송되었습니다.")
