package com.ssafy.shieldroneapp.utils

/**
 * 날짜 및 시간 처리를 위한 유틸리티 클래스.
 *
 * 생년월일과 같은 날짜 형식 포맷팅을 제공합니다.
 */
object DateUtils {
    /**
     * 생년월일 입력을 YYYY-MM-DD 형식으로 자동으로 포맷팅하는 함수.
     * 숫자만 입력받아, 현재 입력된 글자 수에 맞추어 자동으로 형식을 지정합니다.
     *
     * 입력: "199004151234" -> 출력: "1990-04-15" (길이 초과 시 첫 8자까지만 사용)
     *
     * @param input 사용자로부터 입력받은 생년월일 데이터 (문자열 형식)
     * @return 포맷된 "YYYY-MM-DD" 형식의 문자열
     */
    fun formatBirthInput(input: String): String {
        val digitsOnly = input.filter { it.isDigit() }

        return when {
            digitsOnly.length <= 4 -> digitsOnly // 연도 입력 중
            digitsOnly.length <= 6 -> "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4)}" // 월까지 입력
            digitsOnly.length <= 8 -> "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}" // 일까지 입력
            else -> "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6, 8)}" // 입력 값이 길 경우에도 8자까지만 사용
        }
    }

}