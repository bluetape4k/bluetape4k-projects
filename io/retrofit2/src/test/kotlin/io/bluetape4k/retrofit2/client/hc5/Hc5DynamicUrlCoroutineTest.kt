package io.bluetape4k.retrofit2.client.hc5

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.client.AbstractDynamicUrlCoroutineTest
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import okhttp3.Call

class Hc5DynamicUrlCoroutineTest: AbstractDynamicUrlCoroutineTest() {

    companion object: KLoggingChannel()

    override val callFactory: Call.Factory = hc5CallFactoryOf()
}
