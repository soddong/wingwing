package com.ssafy.shieldroneapp.data.source.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

data class WarningData(
    val time: String,
    val warningFlag: Boolean
)

@Singleton
class WebSocketMessageParser @Inject constructor() {
    companion object {
        internal const val TAG = "모바일: 웹소켓 메시지 파서"
    }

    internal val gson = Gson()

    fun parseWarningMessage(message: String): WarningData? {
        return try {
            val jsonObject = JsonParser.parseString(message).asJsonObject
            Log.d(TAG, "파싱 시도 중인 메시지: $message")

            if (jsonObject.has("type") &&
                jsonObject.get("type").asString == "sendWarningFlag") {

                val time = jsonObject.get("time").asString
                val warningFlag = jsonObject.get("warningFlag").asBoolean

                Log.d(TAG, "경고음 메시지 파싱 성공 - time: $time, warningFlag: $warningFlag")

                WarningData(
                    time,
                    warningFlag
                )
            } else {
                Log.d(TAG, "sendWarningFlag 타입이 아니거나 필수 필드 누락")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "경고음 메시지 파싱 실패: ${e.message}")
            Log.e(TAG, "수신된 메시지: $message")
            e.printStackTrace()
            null
        }
    }

    fun getMessageType(message: String): String? {
        return try {
            val jsonObject = JsonParser.parseString(message).asJsonObject
            if (jsonObject.has("type")) {
                jsonObject.get("type").asString
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 타입 확인 실패", e)
            null
        }
    }

    // internal inline 함수로 변경
    internal inline fun <reified T> parseMessage(message: String): T? {
        return try {
            gson.fromJson(message, T::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "메시지 파싱 실패: ${T::class.java.simpleName}", e)
            null
        }
    }
}