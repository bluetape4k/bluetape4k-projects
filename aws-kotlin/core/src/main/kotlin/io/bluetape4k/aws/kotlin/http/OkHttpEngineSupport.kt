package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngineConfig
import io.bluetape4k.utils.ShutdownQueue

fun okHttpEngineOf(
    config: OkHttpEngineConfig = OkHttpEngineConfig.Default,
): OkHttpEngine =
    OkHttpEngine(config).apply {
        ShutdownQueue.register(this)
    }
