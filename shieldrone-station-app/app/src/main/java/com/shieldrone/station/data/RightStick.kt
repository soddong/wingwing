package com.shieldrone.station.data

import dji.v5.manager.aircraft.virtualstick.IStick


// IStick을 구현하는 RightStick 클래스
class RightStick(
    private var stickPosition: StickPosition = StickPosition()
) : IStick {

    override fun setVerticalPosition(position: Int) {
        // 입력된 위치가 허용된 범위 내인지 확인
        if (position in -660..660) {
            stickPosition = stickPosition.copy(verticalPosition = position)
        } else {
            throw IllegalArgumentException("Vertical position must be between -660 and 660")
        }
    }

    // verticalPosition 반환 메서드 구현
    override fun getVerticalPosition(): Int {
        return stickPosition.verticalPosition
    }

    // horizontalPosition 설정 메서드 구현
    override fun setHorizontalPosition(position: Int) {
        // 입력된 위치가 허용된 범위 내인지 확인
        if (position in -660..660) {
            stickPosition = stickPosition.copy(horizontalPosition = position)
        } else {
            throw IllegalArgumentException("Horizontal position must be between -660 and 660")
        }
    }

    // horizontalPosition 반환 메서드 구현
    override fun getHorizontalPosition(): Int {
        return stickPosition.horizontalPosition
    }
}
