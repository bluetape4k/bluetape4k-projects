package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 기본 [CoroutineScope] 입니다
 */
open class DefaultCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: CompletableJob = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    override fun toString(): String =
        "DefaultCoroutineScope(coroutineContext=$coroutineContext)"
}
