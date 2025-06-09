package io.bluetape4k.spring.cassandra

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class AbstractCassandraCoroutineTest(
    private val coroutineName: String = "cassandra4",
): AbstractCassandraTest(),
   CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName(coroutineName)) {

    companion object: KLoggingChannel()
}
