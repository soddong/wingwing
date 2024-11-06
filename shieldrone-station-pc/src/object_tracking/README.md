# 실행 환경 설정

• **CUDA toolkit 11.2 with cuDNN v8.2.1(for multi card support, NCCL2.7 or higher；for PaddleTensorRT deployment, TensorRT8.0.3.4)**

paddlepaddle 2.6.1 설치…

https://www.paddlepaddle.org.cn/documentation/docs/en/2.6/install/pip/linux-pip_en.html 참조

# 패키지 설치
```bash
pip install -r requirements.txt
pip install wheel
pip install lap --no-build-isolation
```