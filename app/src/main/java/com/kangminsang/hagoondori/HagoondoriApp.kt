package com.kangminsang.hagoondori

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 앱 진입점. Hilt의 컴포넌트 트리는 여기서부터 시작된다.
 *
 * 이 앱은 서버도 계정도 없다(원칙 B) - 여기서 하는 일은 DI 그래프를 붙이는 것뿐이다.
 */
@HiltAndroidApp
class HagoondoriApp : Application()
