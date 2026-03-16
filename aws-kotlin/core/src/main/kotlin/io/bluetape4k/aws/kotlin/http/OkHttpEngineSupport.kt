package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngineConfig
import io.bluetape4k.utils.ShutdownQueue

/**
 * [OkHttpEngineConfig]을 사용하여 [OkHttpEngine] 인스턴스를 생성합니다.
 *
 * 생성된 엔진은 애플리케이션 종료 시 자동으로 닫힐 수 있도록 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val engine = okHttpEngineOf()
 * val client = S3Client { httpClient = engine }
 * ```
 *
 * @param config [OkHttpEngineConfig] 설정 (기본값: [OkHttpEngineConfig.Default])
 * @return [OkHttpEngine] 인스턴스
 */
fun okHttpEngineOf(config: OkHttpEngineConfig = OkHttpEngineConfig.Default): OkHttpEngine =
    OkHttpEngine(config).apply {
        ShutdownQueue.register(this)
    }
