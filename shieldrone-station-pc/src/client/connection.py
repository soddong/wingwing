from flask import Flask, jsonify, request
import asyncio
import websockets
import threading
import json
import zmq
import time
from datetime import datetime

class Server:
    def __init__(self):
        """
        Server 클래스 초기화 및 Flask 앱과 WebSocket 클라이언트 세트 생성.
        """
        self.app = Flask(__name__)
        self.ws_clients = set()
        self.setup_routes()
        self.setup_zmq()

    def setup_routes(self):
        """
        Flask 라우트를 설정하여 엔드포인트를 초기화.
        """
        self.app.add_url_rule('/test', 'test', self.test, methods=['GET'])
        self.app.add_url_rule('/connect', 'connect', self.connect, methods=['POST'])

    def setup_zmq(self):
        """
        ZeroMQ 소켓을 설정하고 TCP 주소와 타임아웃을 5초로 지정.
        """
        self.context = zmq.Context()
        self.socket_danger = self.context.socket(zmq.PUSH)
        self.socket_route = self.context.socket(zmq.PUSH)
        self.socket_flag = self.context.socket(zmq.PULL) 

        self.socket_danger.bind("tcp://127.0.0.1:5560") 
        self.socket_route.bind("tcp://127.0.0.1:5570")
        self.socket_flag.bind("tcp://127.0.0.1:5590") 

        self.socket_danger.setsockopt(zmq.SNDTIMEO, 5000)
        self.socket_route.setsockopt(zmq.SNDTIMEO, 5000)
        self.socket_route.setsockopt(zmq.RCVTIMEO, 5000)

        threading.Thread(target=self.receive_flag_data, daemon=True).start()

    def test(self):
        """
        테스트 엔드포인트로, 서버의 동작 여부 확인용.

        Returns:
            Response: 테스트 메시지와 상태를 포함한 JSON 응답.
        """
        return jsonify({"message": "This is a test endpoint", "status": "success"}), 200

    def connect(self):
        """
        Server와 user_id를 가지는 Client 간 WebSocket 연결 시도.

        Returns:
            Response: user_id가 없을 경우 400 에러 발생, 성공 시 200 상태 코드 반환.
        """
        data = request.get_json()

        if user_id:
            print(f"유저 {user_id}가 연결되었습니다. 이제 WebSocket을 통해 실시간 데이터를 전송할 수 있습니다.")
            return '', 200
        else:
            return jsonify({"error": "user_id missing"}), 400

    async def websocket_handler(self, websocket, path):
        """
        WebSocket 연결을 관리하며, 연결된 클라이언트로부터 메시지를 수신하고 처리함.

        Args:
            websocket (WebSocket): WebSocket 연결 인스턴스.
            path (str): WebSocket 경로.
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
                    await self.handle_send_pulse_flag(data)
                elif message_type == "sendDbFlag":
                    await self.handle_send_db_flag(data)

        except websockets.ConnectionClosed:
            print("WebSocket 연결이 닫혔습니다.")
        finally:
            self.ws_clients.remove(websocket)

    def receive_flag_data(self):
        """
        5590 포트에서 flag 데이터를 수신하여 처리.
        """
        while True:
            try:
                message = self.socket_flag.recv_string()
                data = json.loads(message)
                time = data.get("time", datetime.now().isoformat())
                warningFlag = data.get("warningFlag", False)
                print(f"[triggerWarningBeep] {data}")
                
                # 위험 상황 판단 및 경고음 전송
                if data.get("type") == "triggerWarningBeep" and warningFlag == True:
                    self.trigger_warning_beep()

            except zmq.ZMQError as e:
                print(f"ZeroMQ 에러 발생: {e}")
                break

    async def handle_track_position(self, data):
        """
        사용자의 위치 데이터를 처리하고 로그에 기록하며 ZeroMQ로 전송.

        Args:
            data (dict): 사용자의 시간 및 위치 정보 (위도와 경도) 포함.
        """
        time = data.get("time", datetime.now().isoformat())
        location = data.get("location", {})
        lat = location.get("lat")
        lng = location.get("lng")
        print(f"[유저 위치 전송] 시간: {time}, 위도: {lat}, 경도: {lng}")

        # ZeroMQ를 통해 데이터 전송
        zmq_data = json.dumps({
            "type": "trackPosition",
            "time": time,
            "location": {"lat": lat, "lng": lng}
        })
        await self.send_message_with_retry(self.socket_route, zmq_data) # type: ignore


    async def handle_send_pulse_flag(self, data):
        """
        심박수 급등 시그널 데이터를 처리하고 로그에 기록하며 ZeroMQ로 전송.

        Args:
            data (dict): 시간 및 pulseFlag 정보 포함.
        """
        time = data.get("time", datetime.now().isoformat())
        pulse_flag = data.get("pulseFlag", False)
        print(f"[심박수 급등 시그널] 시간: {time}, PulseFlag: {pulse_flag}")

        # ZeroMQ를 통해 데이터 전송
        zmq_data = json.dumps({
            "type": "sendPulseFlag",
            "time": time,
            "pulseFlag": pulse_flag
        })
        await self.send_message_with_retry(self.socket_danger, zmq_data) # type: ignore

    async def handle_send_db_flag(self, data):
        """
        음성 위험 신호 데이터를 처리하고 로그에 기록하며 ZeroMQ로 전송.

        Args:
            data (dict): 시간 및 dbFlag 정보 포함.
        """
        time = data.get("time", datetime.now().isoformat())
        db_flag = data.get("dbFlag", False)
        print(f"[음성 위험 신호 수신] 시간: {time}, DbFlag: {db_flag}")

        # ZeroMQ를 통해 데이터 전송
        zmq_data = json.dumps({
            "type": "sendDbFlag",
            "time": time,
            "dbFlag": db_flag
        })
        await self.send_message_with_retry(self.socket_danger, zmq_data) # type: ignore

    async def send_warning_beep(self):
        """
        경고음 전송 메시지를 모든 연결된 WebSocket 클라이언트에 전송.
        """
        time = datetime.now().isoformat()
        message = json.dumps({"type": "triggerWarningBeep", "time": time, "warningFlag": True})

        for client in self.ws_clients:
            try:
                await client.send(message)
                print(f"[경고음 전송] 시간: {time}, WarningFlag: True")
            except websockets.ConnectionClosed:
                print("클라이언트가 예상치 않게 연결 해제됨.")

    def trigger_warning_beep(self):
        """
        경고음을 WebSocket 클라이언트에 비동기적으로 전송함.
        """
        asyncio.run(self.send_warning_beep())

    def run_flask(self):
        self.app.run(host="0.0.0.0", port=5000)

    async def run_websocket(self):
        """
        WebSocket 서버를 0.0.0.0:8765에서 대기 상태로 실행.
        """
        async with websockets.serve(self.websocket_handler, "0.0.0.0", 8765):
            print("WebSocket 서버가 8765 포트에서 대기 중입니다...")
            await asyncio.Future()

    async def send_message_with_retry(self, socket, message):
        """
        메시지를 소켓을 통해 전송. 타임아웃 에러 발생 시 재시도.
        """
        retries=3
        attempt = 0
        while attempt < retries:
            try:
                socket.send_string(message)
                break  # 성공 시 루프 종료
            except zmq.Again:
                attempt += 1
                print(f"Attempt {attempt} failed. Retrying...")
                time.sleep(1)  # 잠시 대기 후 재시도
            except Exception as e:
                print(f"Unexpected error while sending message: {e}")
                break

        if attempt == retries:
            print("Failed to send message after multiple attempts.")

    def start(self):
        flask_thread = threading.Thread(target=self.run_flask)
        flask_thread.start()

        asyncio.run(self.run_websocket())

if __name__ == "__main__":
    server = Server()
    server.start()
