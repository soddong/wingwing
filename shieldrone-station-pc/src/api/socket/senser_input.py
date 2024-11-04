from flask import Flask, jsonify, request
import asyncio
import websockets
import threading
import json
from datetime import datetime

app = Flask(__name__)
ws_clients = set() 

@app.route('/test', methods=['GET'])
def test():
    return jsonify({"message": "This is a test endpoint", "status": "success"}), 200

@app.route('/connect', methods=['POST'])
def connect():
    data = request.get_json()
    user_id = data.get("user_id")
    
    if user_id:
        print(f"유저 {user_id}가 연결되었습니다. 이제 WebSocket을 통해 실시간 데이터를 전송할 수 있습니다.")
        return jsonify({"status": "connected", "ws_url": "wss://96b8-222-107-238-22.ngrok-free.app"}), 200
    else:
        return jsonify({"error": "user_id missing"}), 400

async def websocket_handler(websocket, path):
    ws_clients.add(websocket)
    print("WebSocket 클라이언트가 연결되었습니다.")

    try:
        async for message in websocket:
            data = json.loads(message)
            message_type = data.get("type")

            if message_type == "trackPosition":
                await handle_track_position(data)
            elif message_type == "sendPulseFlag":
                await handle_send_pulse_flag(data)
            elif message_type == "sendDbFlag":
                await handle_send_db_flag(data)

    except websockets.ConnectionClosed:
        print("WebSocket 연결이 닫혔습니다.")
    finally:
        ws_clients.remove(websocket)

async def handle_track_position(data):
    time = data.get("time", datetime.now().isoformat())
    location = data.get("location", {})
    lat = location.get("lat")
    lng = location.get("lng")
    print(f"[유저 위치 전송] 시간: {time}, 위도: {lat}, 경도: {lng}")

async def handle_send_pulse_flag(data):
    time = data.get("time", datetime.now().isoformat())
    pulse_flag = data.get("pulseFlag", False)
    print(f"[심박수 급등 시그널] 시간: {time}, PulseFlag: {pulse_flag}")

async def handle_send_db_flag(data):
    time = data.get("time", datetime.now().isoformat())
    db_flag = data.get("dbFlag", False)
    print(f"[음성 위험 신호 수신] 시간: {time}, DbFlag: {db_flag}")

def run_flask():
    app.run(host="0.0.0.0", port=5000) 

async def run_websocket():
    async with websockets.serve(websocket_handler, "0.0.0.0", 8765):
        print("WebSocket 서버가 8765 포트에서 대기 중입니다...")
        await asyncio.Future()

def main():
    flask_thread = threading.Thread(target=run_flask)
    flask_thread.start()

    asyncio.run(run_websocket())

if __name__ == "__main__":
    main()
