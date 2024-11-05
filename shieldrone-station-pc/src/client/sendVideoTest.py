import socket
import cv2
import numpy as np
import json
import os
import time

# config.json 파일 경로 설정 및 로드
config_path = os.path.join(os.path.dirname(__file__), "../../../config.json")
with open(config_path, "r") as config_file:
    config = json.load(config_file)

# 서버 정보 설정
HOST = config["HOST"]
PORT = config["PORT"]
VIDEO_PATH = "/S11P31A307/shieldrone-station-pc/src/object_tracking/test_video.mp4"  # 전송할 동영상 파일 경로 설정

def send_video():
    # UDP 소켓 생성
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    while True:
        # 동영상 파일 열기
        cap = cv2.VideoCapture(VIDEO_PATH)
        frame_count = 0
        start_time = time.time()
        
        try:
            while cap.isOpened():
                # 프레임 읽기
                ret, frame = cap.read()
                if not ret:
                    print("End of video file reached, restarting...")
                    break  # 동영상이 끝나면 루프를 빠져나와 다시 시작

                # 프레임 크기 조정 (예: 해상도를 640x480으로 줄이기)
                frame = cv2.resize(frame, (640, 480))

                # 프레임을 JPEG 형식으로 인코딩하며 품질을 낮추기
                encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 50]  # 품질을 50%로 설정
                _, buffer = cv2.imencode(".jpg", frame, encode_param)
                packet = buffer.tobytes()

                # 패킷이 너무 큰 경우 대비: 최대 패킷 크기를 넘으면 전송하지 않음
                if len(packet) > 65507:
                    print("Warning: Packet size too large, skipping frame.")
                    continue

                # 서버로 전송
                client_socket.sendto(packet, (HOST, PORT))
                frame_count += 1  # 전송한 프레임 수 증가

                # 약간의 지연을 추가하여 전송 속도 조절 (예: 초당 30 프레임)
                time.sleep(1/30)

                # 5초마다 초당 전송한 프레임 수 출력
                current_time = time.time()
                elapsed_time = current_time - start_time
                if elapsed_time >= 5.0:
                    fps = frame_count / elapsed_time  # 초당 전송한 프레임 수 계산
                    print("5초 요약:")
                    print(f" - 전송한 프레임 수: {frame_count}")
                    print(f" - 초당 전송한 프레임 수 (FPS): {fps:.2f}")
                    
                    # 초기화
                    frame_count = 0
                    start_time = current_time

        except Exception as e:
            print(f"Error: {e}")
        finally:
            cap.release()
            print("동영상 재생 종료 후 반복 재시작")

if __name__ == "__main__":
    send_video()
