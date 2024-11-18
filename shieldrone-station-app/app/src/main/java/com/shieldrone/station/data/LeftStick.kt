package com.shieldrone.station.data

import com.shieldrone.station.constant.FlightConstant.Companion.MAX_STICK_VALUE
import dji.v5.manager.aircraft.virtualstick.IStick


// IStick을 구현하는 LeftStick 클래스
class LeftStick(
    private var stickPosition: StickPosition = StickPosition()
) : IStick {

    override fun setVerticalPosition(position: Int) {
        // 입력된 위치가 허용된 범위 내인지 확인
        if (position in -MAX_STICK_VALUE..MAX_STICK_VALUE) {
            stickPosition = stickPosition.copy(verticalPosition = position)
        } else {
            throw IllegalArgumentException("Vertical position must be between -MAX_STICK_VALUE and MAX_STICK_VALUE")
        }
    }

    // verticalPosition 반환 메서드 구현
    override fun getVerticalPosition(): Int {
        return stickPosition.verticalPosition
    }

    // horizontalPosition 설정 메서드 구현
    override fun setHorizontalPosition(position: Int) {
        // 입력된 위치가 허용된 범위 내인지 확인
        if (position in -MAX_STICK_VALUE..MAX_STICK_VALUE) {
            stickPosition = stickPosition.copy(horizontalPosition = position)
        } else {
            throw IllegalArgumentException("Horizontal position must be between -MAX_STICK_VALUE and MAX_STICK_VALUE")
        }
    }

    // horizontalPosition 반환 메서드 구현
    override fun getHorizontalPosition(): Int {
        return stickPosition.horizontalPosition
    }
}
