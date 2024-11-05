package com.ssafy.shieldroneapp.data.model

import com.google.gson.Gson

data class AudioData(
    val time: Long = System.currentTimeMillis(),
    val audioData: ByteArray
) {
    fun toJson(): String = Gson().toJson(this)

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
        fun fromJson(json: String): AudioData {
            return Gson().fromJson(json, AudioData::class.java)
        }
    }
}