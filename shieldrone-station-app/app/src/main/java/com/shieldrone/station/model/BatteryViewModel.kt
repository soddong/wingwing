package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import com.shieldrone.station.model.BatteryModel.Companion.batteryPercent
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class BatteryViewModel(private val batteryModel: BatteryModel) {

    private val handler = Handler(Looper.getMainLooper())
    private val url = "http://k11a307.p.ssafy.io/api/tester" // 서버 주소 (엔드포인트 조정 가능)

    private val sendBatteryStatusRunnable = object : Runnable {
        override fun run() {
            // 배터리 퍼센트 가져오기


            // 서버로 전송
            sendBatteryPercentToServer(batteryPercent)

            // 3분(180,000ms) 후 다시 실행
            handler.postDelayed(this, 180000)
        }
    }
    private fun sendBatteryPercentToServer(batteryPercent: Int?) {
        if (batteryPercent == null) return

        Thread {
            try {
                val url = URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                // JSON 데이터 생성
                val jsonInputString = """{"batteryPercent":$batteryPercent}"""

                // 요청 본문에 데이터 쓰기
                connection.outputStream.use { os: OutputStream ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // 서버 응답 코드 확인
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 성공적으로 전송됨
                    println("Battery percent sent successfully")
                } else {
                    // 실패 시 로그 출력
                    println("Failed to send battery percent: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 시작 메서드: Runnable 시작
    fun startSendingBatteryStatus() {
        handler.post(sendBatteryStatusRunnable)
    }

    // 중지 메서드: Runnable 중지
    fun stopSendingBatteryStatus() {
        handler.removeCallbacks(sendBatteryStatusRunnable)
    }
}