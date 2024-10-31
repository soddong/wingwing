import socket
import json
import os
import cv2
import numpy as np
from ultralytics import YOLO

# config.json 파일 경로 설정 및 로드
config_path = os.path.join(os.path.dirname(__file__), "../../../config.json")
with open(config_path, "r") as config_file:
    config = json.load(config_file)

# 서버 정보 및 YOLO 모델 경로 설정
HOST = config["HOST"]
PORT = config["PORT"]
YOLO_MODEL_PATH = config["YOLO_MODEL_PATH"]

# YOLO 모델 로드 (사전 학습된 YOLO 모델 가정)
model = YOLO(YOLO_MODEL_PATH)

def main():
    # UDP 소켓 생성 및 바인딩
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind((HOST, PORT))
    print(f"서버가 {HOST}:{PORT}에서 대기 중입니다...")

    try:
        while True:
            # 최대 패킷 크기 수신 (65507은 IPv4에서 UDP 최대 크기)
            packet, addr = server_socket.recvfrom(65507)
            print(f"{addr}로부터 패킷 수신 완료")

            # 바이트 배열을 이미지로 변환
            frame = np.frombuffer(packet, dtype=np.uint8)
            img = cv2.imdecode(frame, cv2.IMREAD_COLOR)

            if img is not None:
                # YOLO 모델을 이용하여 객체 탐지 수행
                results = model(img)

                # 탐지 결과 출력
                for result in results:
                    print("Detected objects:", result.boxes)

                # 수신한 이미지 표시
                cv2.imshow("Received Frame", img)
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    break
            else:
                print("Error: Failed to decode image.")
                
    except Exception as e:
        print(f"Error: {e}")
    finally:
        server_socket.close()
        cv2.destroyAllWindows()

if __name__ == "__main__":
    main()

