package com.ssafy.shieldroneapp.utils

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 키보드 제어를 위한 KeyboardController 객체를 생성하고,
 * Compose 컴포저블 내에서 상태를 유지하기 위한 함수입니다.
 *
 * @return KeyboardController 인스턴스를 반환하여 키보드 제어 기능을 제공
 */
@Composable
fun rememberKeyboardController(): KeyboardController {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current // 포커스 매니저 추가

    // 키보드 제어를 위한 InputMethodManager
    val inputMethodManager = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    return remember {
        KeyboardController(context, inputMethodManager, focusManager)
    }
}

/**
 * 키보드와 포커스를 관리하기 위한 클래스
 *
 * @property context 현재 화면의 Context를 받아와서 Activity로 캐스팅하여 사용
 * @property inputMethodManager 시스템 InputMethodManager를 통해 키보드 숨김 제어
 * @property focusManager Compose에서 제공하는 FocusManager로 포커스 해제 기능을 지원
 */
class KeyboardController(
    private val context: Context,
    private val inputMethodManager: InputMethodManager,
    private val focusManager: androidx.compose.ui.focus.FocusManager
) {
    /**
     * 현재 화면의 포커스를 해제하고 키보드를 숨기는 메서드입니다.
     *
     * Activity의 현재 포커스된 뷰를 확인한 후, 해당 뷰의 윈도우 토큰을 이용하여
     * 키보드를 숨기고, 포커스를 해제합니다.
     */
    fun hideKeyboard() {
        val activity = context as? Activity
        activity?.currentFocus?.let { view ->
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            focusManager.clearFocus()
        }
    }
}