package com.kangminsang.hagoondori.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kangminsang.hagoondori.BuildConfig
import com.kangminsang.hagoondori.data.remote.holiday.HolidayApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 공휴일 조회(F15)에만 쓰이는 네트워크 계층 - 이 앱의 유일한 네트워크 사용처다
 * (원칙 B: 서버를 두지 않는다, 유일한 네트워크 사용은 공휴일 조회).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(HolidayApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHolidayApiService(retrofit: Retrofit): HolidayApiService =
        retrofit.create(HolidayApiService::class.java)
}
