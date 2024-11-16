# server.py
import json
import os
import sys
import time
import zmq
from datetime import datetime
from flask import Flask
import asyncio
import websockets
import threading

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from decision.route.route_decision import RouteDecision
from decision.danger.danger_decision import DangerDecision

class Server:
    def __init__(self):
        """
        Server 클래스 초기화 및 Flask 앱, WebSocket 클라이언트 세트 생성.
        """
        self.app = Flask(__name__)
        self.ws_clients = set()
        self.frame_lasttime = datetime.now()
        self.frame = None

        # RouteDecision 및 DangerDecision 클래스 인스턴스 생성
        self.route_decision = RouteDecision()
        self.danger_decision = DangerDecision()

        # ZeroMQ 소켓 설정
        self.context = zmq.Context()
        self.socket_camera = self.context.socket(zmq.SUB)
        self.socket_camera.connect("tcp://127.0.0.1:5580")
        self.socket_camera.setsockopt_string(zmq.SUBSCRIBE, "")

    async def websocket_handler(self, websocket, path):
        """
        WebSocket 연결을 관리하며, 연결된 클라이언트로부터 메시지를 수신하고 처리함.
        """
        self.ws_clients.add(websocket)
        print("WebSocket 클라이언트가 연결되었습니다.")

        try:
            async for message in websocket:
                data = json.loads(message)
                message_type = data.get("type")

                if message_type == "trackPosition":
                    await self.handle_track_position(data)
                elif message_type == "sendPulseFlag":
                    self.danger_decision.set_pulse_flag_trigger(data.get("pulseFlag"))
                elif message_type == "sendDbFlag":
                    self.danger_decision.set_db_flag_trigger(data.get("dbFlag"))

                # DangerDecision을 통해 위험 상태 확인 후 클라이언트에게 알림
                if self.danger_decision.check_condition():
                    await self.send_warning_to_clients()

        except websockets.ConnectionClosed:
            print("WebSocket 연결이 닫혔습니다.")
        finally:
            self.ws_clients.remove(websocket)

    async def send_warning_to_clients(self):
        """
        위험 상황 발생 시 모든 WebSocket 클라이언트에 경고 메시지 전송.
        """
        message = json.dumps({
            "type": "sendWarningFlag",
            "time": datetime.now().isoformat(),
            "warningFlag": True,
            "frame": self.frame
        })
        for client in self.ws_clients:
            try:
                await client.send(message)
            except websockets.ConnectionClosed:
                print("클라이언트가 예상치 않게 연결 해제됨.")

    async def handle_track_position(self, data):
        """
        사용자의 위치 데이터를 처리하고 로그에 기록하며 RouteDecision에 전달.
        """
        time = data.get("time", datetime.now().isoformat())
        location = data.get("location", {})
        dest_location = data.get("dest_location", {})
        lat = location.get("lat")
        lng = location.get("lng")
        dest_lat = dest_location.get("lat")
        dest_lng = dest_location.get("lng")
        print(f"[유저 위치 전송] 시간: {time}, 현재 위도: {lat}, 현재 경도: {lng}, 목적지 위도: {dest_lat}, 목적지 경도: {dest_lng}")

        # 위치 정보를 RouteDecision에 직접 전달
        self.route_decision.handle_position_update(lat, lng, dest_lat, dest_lng)

    def camera_data_thread(self, loop):
        """
        별도 스레드에서 카메라 데이터를 수신하여 asyncio 루프에 전달.
        """
        asyncio.set_event_loop(loop)  # Set the event loop for this thread
        while True:
            try:
                message = self.socket_camera.recv_string(flags=zmq.NOBLOCK)
                data = json.loads(message)
                self.frame = data.get("frame")
                self.danger_decision.set_camera_flag_trigger(True)
                print("[카메라 데이터 수신] Frame 데이터 업데이트됨.")

                # Send the data update to the event loop
                asyncio.run_coroutine_threadsafe(self.update_clients_with_frame(), loop)

            except zmq.Again:
                time.sleep(0.1)  # 데이터가 없을 경우 잠시 대기

    async def update_clients_with_frame(self):
        """
        WebSocket 클라이언트에 업데이트된 카메라 프레임을 전송.
        """
        message = json.dumps({
            "type": "updateFrame",
            "time": datetime.now().isoformat(),
            "frame": self.frame
        })
        for client in self.ws_clients:
            try:
                await client.send(message)
            except websockets.ConnectionClosed:
                print("클라이언트가 연결 해제됨.")

    def run_flask(self):
        self.app.run(host="0.0.0.0", port=5000)

    async def run_websocket(self):
        """
        WebSocket 서버를 0.0.0.0:8765에서 대기 상태로 실행.
        """
        async with websockets.serve(self.websocket_handler, "0.0.0.0", 8765):
            print("WebSocket 서버가 8765 포트에서 대기 중입니다...")
            await asyncio.Future()

    def start(self):
        flask_thread = threading.Thread(target=self.run_flask)
        flask_thread.start()

        # Set up the main event loop
        loop = asyncio.get_event_loop()
        
        # Start the camera data thread with access to the main loop
        threading.Thread(target=self.camera_data_thread, args=(loop,), daemon=True).start()

        # Run WebSocket server in the main event loop
        loop.run_until_complete(self.run_websocket())

if __name__ == "__main__":
    server = Server()
    server.start()
