package com.kangminsang.hagoondori.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.kangminsang.hagoondori.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.ceil

/**
 * 위젯에 쓰는 글꼴. [resId]는 `res/font`의 Galmuri 파일이고, [fakeBold]는 앱의 Compose 쪽이
 * Galmuri14를 굵게 그리는 방식(합성 볼드)을 맞추기 위해 켠다.
 */
enum class WidgetFont(val resId: Int, val fakeBold: Boolean = false) {
    TITLE(R.font.galmuri14, fakeBold = true),
    BODY(R.font.galmuri11),
    BOLD(R.font.galmuri11_bold),
}

/**
 * 위젯 글자를 [Bitmap]으로 그린다.
 *
 * **글자를 비트맵으로 그리는 이유:** RemoteViews는 홈 앱 프로세스의 제한된 컨텍스트에서 부풀려진다.
 * 그래서 `android:fontFamily="@font/..."`의 글꼴 리소스를 읽지 못하고 시스템 글꼴로 떨어진다
 * (실기기 One UI에서 확인). 앱이 글꼴을 직접 읽어 미리 그린 비트맵을 ImageView로 넣으면 글꼴이 그대로 나온다.
 * 대신 글자는 선택하거나 읽을 수 없는 그림이 되므로, 호출하는 쪽이 ImageView에 원문을
 * `setContentDescription`으로 붙인다.
 *
 * 한 줄 또는 `'\n'`으로 나눈 여러 줄을 그린다. 여러 줄은 가운데 정렬한다.
 */
class WidgetTextBitmap @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val typefaces: Map<WidgetFont, Typeface> by lazy {
        WidgetFont.entries.associateWith { font ->
            requireNotNull(ResourcesCompat.getFont(context, font.resId)) { "위젯 글꼴을 읽지 못했다: ${font.name}" }
        }
    }

    /** 위젯 글꼴 파일을 읽은 [Typeface]. 달력 위젯 비트맵처럼 직접 그리는 쪽이 같은 글꼴을 쓰도록 공유한다. */
    fun typeface(font: WidgetFont): Typeface = typefaces.getValue(font)

    /**
     * [text]를 [font]·[sizeSp]·[colorRes]로 그린 비트맵을 돌려준다.
     *
     * - 크기는 [TypedValue.applyDimension]으로 sp를 픽셀로 바꾼다.
     * - 폭은 가장 긴 줄의 `ceil(measureText) + 1`(최소 1), 높이는 줄 수 × `descent - ascent`다.
     * - [Bitmap.setDensity]를 기기 밀도로 맞춘다. 그래야 홈 앱이 ImageView에 넣을 때 픽셀 그대로,
     *   올바른 크기로 보인다.
     */
    fun draw(text: String, font: WidgetFont, sizeSp: Float, colorRes: Int): Bitmap {
        val metrics = context.resources.displayMetrics
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = typeface(font)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
            color = ContextCompat.getColor(context, colorRes)
            isFakeBoldText = font.fakeBold
        }
        val lines = text.split('\n')
        val fontMetrics = paint.fontMetricsInt
        val lineHeight = fontMetrics.descent - fontMetrics.ascent
        val width = ceil(lines.maxOf { paint.measureText(it) }).toInt() + 1
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            (lineHeight * lines.size).coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        bitmap.density = metrics.densityDpi
        val canvas = Canvas(bitmap)
        lines.forEachIndexed { index, line ->
            val x = (bitmap.width - paint.measureText(line)) / 2f
            // baseline은 줄 상단에서 ascent(음수)만큼 내린 자리다.
            canvas.drawText(line, x, index * lineHeight - fontMetrics.ascent.toFloat(), paint)
        }
        return bitmap
    }
}
