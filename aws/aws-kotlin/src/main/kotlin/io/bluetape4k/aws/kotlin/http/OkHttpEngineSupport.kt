package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngineConfig

/**
 * [OkHttpEngineConfig]을 사용하여 [OkHttpEngine] 인스턴스를 생성합니다.
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
    OkHttpEngine(config)
