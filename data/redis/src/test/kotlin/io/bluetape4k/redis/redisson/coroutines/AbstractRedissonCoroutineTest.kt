package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class AbstractRedissonCoroutineTest: AbstractRedissonTest() {

    companion object: KLogging()

    protected val scope = CoroutineScope(CoroutineName("redisson") + Dispatchers.IO)

    protected val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            log.error(exception) {
                "CoroutineExceptionHandler get $exception with suppressed ${exception.suppressed.contentToString()} "
            }
            throw RuntimeException("Fail to execute in coroutine", exception)
        }
}
