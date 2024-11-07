package com.shieldrone.station.controller

class PIDController(
    private val kp: Double, // 비례 이득
    private val ki: Double, // 적분 이득
    private val kd: Double  // 미분 이득
) {
    private var previousError = 0.0
    private var integral = 0.0

    fun calculate(setpoint: Double, measuredValue: Double, deltaTime: Double): Double {
        val error = setpoint - measuredValue
        integral += error * deltaTime
        val derivative = (error - previousError) / deltaTime
        previousError = error

        return (kp * error) + (ki * integral) + (kd * derivative)
    }

    fun reset() {
        previousError = 0.0
        integral = 0.0
    }
}
