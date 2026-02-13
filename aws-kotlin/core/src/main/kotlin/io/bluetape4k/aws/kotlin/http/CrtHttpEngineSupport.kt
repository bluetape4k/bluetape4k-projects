package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngineConfig
import io.bluetape4k.utils.ShutdownQueue

fun defaultCrtHttpEngineOf(
    config: CrtHttpEngineConfig = CrtHttpEngineConfig.Default,
): CrtHttpEngine =
    CrtHttpEngine(config).apply {
        ShutdownQueue.register(this)
    }
