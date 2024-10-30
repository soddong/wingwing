import socket
import cv2
import os
import json

# config.json 파일 경로 설정 및 로드
config_path = os.path.join(os.path.dirname(__file__), "config.json")
with open(config_path, "r") as config_file:
    config = json.load(config_file)

# 서버 정보 설정
HOST = config["HOST"]
PORT = config["PORT"]

# TCP 서버 소켓 생성
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
    server_socket.bind((HOST, PORT))
    server_socket.listen()
    print(f"Server is listening on {HOST}:{PORT}")

    # 클라이언트 연결 수락
    client_socket, client_address = server_socket.accept()
    with client_socket:
        print(f"Connected by {client_address}")
        
        # 전송할 이미지 경로 설정 (tcp_server.py와 같은 폴더 내에 위치한 test_image.jpg 사용)
        image = cv2.imread("test_image.jpg")  # 이미지를 읽기
        
        # 이미지를 바이트 배열로 인코딩
        if image is not None:
            _, buffer = cv2.imencode(".jpg", image)
            byte_data = buffer.tobytes()
            
            # 이미지 크기(4바이트) 전송
            client_socket.sendall(len(byte_data).to_bytes(4, byteorder='big'))
            print("Sending frame size...")

            # 이미지 데이터 전송
            client_socket.sendall(byte_data)
            print("Image frame sent to client. Closing connection.")
        else:
            print("Error: Unable to load image.")
