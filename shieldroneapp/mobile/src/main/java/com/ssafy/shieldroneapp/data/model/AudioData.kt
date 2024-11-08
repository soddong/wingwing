package com.ssafy.shieldroneapp.data.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

data class AudioData(
    val time: Long = System.currentTimeMillis(),
    val audioData: ByteArray
) {
    // 바이너리 형식
    fun toByteArray(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            DataOutputStream(bos).use { dos ->
                // 타임스탬프
                dos.writeLong(time)
                // 오디오 데이터 길이 (4바이트)
                dos.writeInt(audioData.size)
                // 실제 오디오 데이터
                dos.write(audioData)
                dos.flush()
            }
            bos.toByteArray()
        }
    }

    // ByteArray는 equals()와 hashCode()를 별도로 구현 필요
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData

        if (time != other.time) return false
        if (!audioData.contentEquals(other.audioData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + audioData.contentHashCode()
        return result
    }

    companion object {
        // 바이너리 데이터로부터 AudioData 객체 생성
        fun fromByteArray(bytes: ByteArray): AudioData {
            return ByteArrayInputStream(bytes).use { bis ->
                DataInputStream(bis).use { dis ->
                    // 타임스탬프 읽기
                    val time = dis.readLong()
                    // 오디오 데이터 길이 읽기
                    val audioSize = dis.readInt()
                    // 오디오 데이터 읽기
                    val audioData = ByteArray(audioSize)
                    dis.readFully(audioData)
                    
                    AudioData(time, audioData)
                }
            }
        }
    }
}