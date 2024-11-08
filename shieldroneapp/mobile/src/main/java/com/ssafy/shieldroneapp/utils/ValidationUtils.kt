package com.ssafy.shieldroneapp.utils

/**
 * 사용자 입력 값 검증을 위한 유틸리티 클래스.
 *
 * 이름, 생년월일, 핸드폰 번호 등 사용자의 입력 값에 대한 유효성 검증 메서드 제공.
 */
object ValidationUtils {
    /**
     * 이름 유효성 검사
     *
     * @param name 검사할 이름
     * @return 유효성 검사 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "이름을 입력해주세요"
            name.length > 10 -> "이름은 10자 이내로 입력해주세요"
            !name.matches(Regex("^[가-힣a-zA-Z]+$")) -> "이름은 한글 완성형 또는 영문만 입력 가능합니다"
            else -> null
        }
    }

    /**
     * 생년월일 유효성 검사
     *
     * @param birth 검사할 생년월일 (YYYY-MM-DD 형식)
     * @return 유효성 검사 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    fun validateBirth(birth: String): String? {
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        // 기본 형식 확인 (YYYY-MM-DD)
        if (!birth.matches(dateRegex)) {
            return "생년월일은 YYYY-MM-DD 형식으로 입력해주세요"
        }

        try {
            val (year, month, day) = birth.split("-").map { it.toInt() }

            // 연도 범위 확인 (현재는 1900년 ~ 2024년 범위로 가정)
            if (year !in 1900..2024) {
                return "유효하지 않은 연도입니다 (1900~2024년 범위)"
            }

            // 월 범위 확인
            if (month !in 1..12) {
                return "유효하지 않은 월입니다 (1~12월)"
            }

            // 각 월의 최대 일수 확인 (윤년 계산 포함)
            val maxDays = when (month) {
                2 -> if (isLeapYear(year)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            if (day !in 1..maxDays) {
                return "유효하지 않은 일자입니다 (월별 최대 일 확인)"
            }

            // 모든 조건을 통과하면 null 반환 (유효한 생년월일)
            return null
        } catch (e: Exception) {
            return "생년월일 형식이 올바르지 않습니다"
        }
    }

    /**
     * 생년월일 입력을 YYYY-MM-DD 형식으로 자동으로 포맷팅하는 함수
     */
    fun formatBirthInput(input: String): String {
        // 숫자만 남기기
        val digitsOnly = input.filter { it.isDigit() }

        return when {
            digitsOnly.length <= 4 -> digitsOnly // 연도 입력 중
            digitsOnly.length <= 6 -> "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4)}" // 월까지 입력
            else -> "${digitsOnly.substring(0, 4)}-${digitsOnly.substring(4, 6)}-${digitsOnly.substring(6)}" // 일까지 입력
        }
    }

    /**
     * 윤년 확인 함수
     *
     * @param year 검사할 연도
     * @return 윤년 여부 (true: 윤년, false: 평년)
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * 전화번호 유효성 검사
     *
     * @param phone 검사할 전화번호
     * @return 유효성 검사 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    fun validatePhone(phone: String): String? {
        return when {
            !phone.matches(Regex("^01[0-1|6-9][0-9]{7,8}$")) ->
                "올바른 전화번호 형식이 아닙니다"
            else -> null
        }
    }

    /**
     * 인증번호 유효성 검사
     *
     * @param code 검사할 인증번호
     * @return 유효성 검사 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    fun validateVerificationCode(code: String): String? {
        return when {
            !code.matches(Regex("^\\d{6}$")) ->
                "인증번호는 6자리 숫자여야 합니다"
            else -> null
        }
    }
}