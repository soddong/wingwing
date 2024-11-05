package com.ssafy.shieldroneapp.data.model

import com.google.gson.Gson

data class AudioData(
    val time: Long = System.currentTimeMillis(),
    val dbFlag: Boolean
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): AudioData {
            return Gson().fromJson(json, AudioData::class.java)
        }
    }
}