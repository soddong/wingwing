import zmq
import zmq.asyncio
import numpy as np
import asyncio

async def handle_message(socket):
    while True:
        # 메타데이터 수신 (비동기 대기)
        metadata = await socket.recv_json()
        data_shape = tuple(metadata['shape'])
        data_dtype = metadata['dtype']
        
        # 배열 데이터 수신 및 복원
        data_bytes = await socket.recv()
        processed_data = np.frombuffer(data_bytes, dtype=data_dtype).reshape(data_shape)

        # 수신한 데이터를 출력하여 확인
        print("수신한 데이터:", processed_data)

async def setup_subscriber():
    # zmq.asyncio를 사용하여 ZeroMQ 소켓 설정 (PULL 모드)
    context = zmq.asyncio.Context()
    socket = context.socket(zmq.PULL)
    socket.connect("tcp://localhost:5555")

    # 메시지 수신 핸들러 실행
    await handle_message(socket)

if __name__ == "__main__":
    asyncio.run(setup_subscriber())
