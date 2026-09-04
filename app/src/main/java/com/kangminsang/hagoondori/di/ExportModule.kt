package com.kangminsang.hagoondori.di

import com.kangminsang.hagoondori.core.export.ExportAdapter
import com.kangminsang.hagoondori.export.FakeExportAdapter
import com.kangminsang.hagoondori.export.RealExportAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * [ExportAdapter] 구현체 두 가지를 큐얼리파이어로 구분해 둘 다 주입 가능하게 한다
 * (스펙 6.3절) - "장치로 내보내기"(Real)와 "미리보기로 저장"(Fake)을 화면에서
 * 둘 다 쓸 수 있어야, 실기기 없이도 페이로드를 눈으로 확인할 수 있다(F16).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @RealAdapter
    abstract fun bindRealExportAdapter(impl: RealExportAdapter): ExportAdapter

    @Binds
    @FakeAdapter
    abstract fun bindFakeExportAdapter(impl: FakeExportAdapter): ExportAdapter
}
