package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngineConfig
import io.bluetape4k.utils.ShutdownQueue

/**
 * [CrtHttpEngineConfig]을 사용하여 [CrtHttpEngine] 인스턴스를 생성합니다.
 *
 * 생성된 엔진은 애플리케이션 종료 시 자동으로 닫힐 수 있도록 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val engine = crtHttpEngineOf()
 * val client = DynamoDbClient { httpClient = engine }
 * ```
 *
 * @param config [CrtHttpEngineConfig] 설정 (기본값: [CrtHttpEngineConfig.Default])
 * @return [CrtHttpEngine] 인스턴스
 */
fun crtHttpEngineOf(config: CrtHttpEngineConfig = CrtHttpEngineConfig.Default): CrtHttpEngine =
    CrtHttpEngine(config)
        .apply {
            ShutdownQueue.register(this)
        }
