package com.kangminsang.hagoondori.di

import javax.inject.Qualifier

/** 실제 USB+SAF로 장치에 쓰는 [com.kangminsang.hagoondori.core.export.ExportAdapter]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealAdapter

/** 개발/미리보기용으로 앱 내부 저장소에 쓰는 [com.kangminsang.hagoondori.core.export.ExportAdapter]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FakeAdapter
