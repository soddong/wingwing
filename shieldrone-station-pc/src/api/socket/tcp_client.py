import socket
import json
import os
import cv2
import numpy as np
from ultralytics import YOLO

# config.json 파일 경로 설정 및 로드
config_path = os.path.join(os.path.dirname(__file__), "config.json")
with open(config_path, "r") as config_file:
    config = json.load(config_file)

# 서버 정보 및 YOLO 모델 경로 설정
HOST = config["HOST"]
PORT = config["PORT"]
YOLO_MODEL_PATH = config["YOLO_MODEL_PATH"]

# YOLO 모델 로드 (사전 학습된 YOLO 모델 가정)
model = YOLO(YOLO_MODEL_PATH)

# TCP 클라이언트 소켓 생성
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
    client_socket.connect((HOST, PORT))
    print(f"Connected to server at {HOST}:{PORT}")

    # 프레임 크기 (4바이트) 수신
    size_data = client_socket.recv(4)
    if len(size_data) < 4:
        print("Error: Failed to receive the size of the data.")
    else:
        data_size = int.from_bytes(size_data, byteorder='big')
        print(f"Receiving data of size: {data_size} bytes")

        # 데이터 수신을 위한 버퍼 설정
        data_buffer = bytearray()
        while len(data_buffer) < data_size:
            chunk = client_socket.recv(min(4096, data_size - len(data_buffer)))
            if not chunk:
                print("Error: Connection closed or incomplete data received.")
                break
            data_buffer.extend(chunk)

        # 수신한 데이터 크기 검증
        if len(data_buffer) == data_size:
            # 수신한 바이트 배열을 numpy 배열로 변환하여 이미지로 디코딩
            frame_array = np.frombuffer(data_buffer, dtype=np.uint8)
            frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)

            if frame is not None:
                # YOLO 모델을 이용한 객체 탐지 수
                results = model(frame)

                # 탐지 결과 출력
                for result in results:
                    print("Detected objects:", result.boxes)
            else:
                print("Error: Failed to decode image.")
        else:
            print("Error: Incomplete data received.")
