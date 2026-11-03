package com.kangminsang.hagoondori

import android.app.Application
import com.kangminsang.hagoondori.widget.WidgetRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 앱 진입점. Hilt의 컴포넌트 트리는 여기서부터 시작된다.
 *
 * 이 앱은 서버도 계정도 없다(원칙 B) - 여기서 하는 일은 DI 그래프를 붙이는 것뿐이다.
 */
@HiltAndroidApp
class HagoondoriApp : Application() {

    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onCreate() {
        super.onCreate()
        // 백업 복원 뒤에는 프로세스가 재시작되므로, 새 DB 내용을 위젯에 다시 그린다.
        widgetRefresher.refresh()
    }
}
