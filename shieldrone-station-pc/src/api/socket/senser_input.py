from flask import Flask, jsonify, request
import asyncio
import websockets
import threading
import json

app = Flask(__name__)
ws_clients = set() 

# 테스트용 GET 엔드포인트
@app.route('/test', methods=['GET'])
def test():
    return jsonify({"message": "This is a test endpoint", "status": "success"}), 200

@app.route('/connect', methods=['POST'])
def connect():
    data = request.get_json()
    user_id = data.get("user_id")
    
    if user_id:
        print(f"유저 {user_id}가 연결되었습니다. 이제 WebSocket을 통해 실시간 데이터를 전송할 수 있습니다.")
        return jsonify({"status": "connected", "ws_url": "ws://211.192.252.62:8765"}), 200
    else:
        return jsonify({"error": "user_id missing"}), 400

async def websocket_handler(websocket, path):
    ws_clients.add(websocket)
    print("WebSocket 클라이언트가 연결되었습니다.")

    try:
        async for message in websocket:
            data = json.loads(message)
            print("수신한 메시지:", data)
            # 클라이언트로부터 수신한 메시지 처리 로직 추가 가능
    except websockets.ConnectionClosed:
        print("WebSocket 연결이 닫혔습니다.")
    finally:
        ws_clients.remove(websocket)

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