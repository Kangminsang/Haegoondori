# 이 앱은 현재 릴리스 빌드에서 코드 축소/난독화를 켜지 않는다(isMinifyEnabled = false,
# app/build.gradle.kts). 배포 전에 켜기로 하면 그때 Room/Hilt/kotlinx-serialization용
# 규칙을 여기 채운다(각 라이브러리가 기본 consumer-rules.pro를 제공하므로 대부분은
# 자동으로 해결되지만, kotlinx-serialization의 @Serializable DTO는 아래 같은 규칙이
# 필요할 수 있다).

# -keepattributes *Annotation*, InnerClasses
# -dontnote kotlinx.serialization.AnnotationsKt
# -keepclasseswithmembers class com.kangminsang.hagoondori.data.remote.holiday.** {
#     *** Companion;
# }
# -keepclasseswithmembers class com.kangminsang.hagoondori.data.remote.holiday.** {
#     kotlinx.serialization.KSerializer serializer(...);
# }
