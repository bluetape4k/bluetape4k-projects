package io.bluetape4k.retrofit2.client.hc5

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf

class Hc5JsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLogging()

    override val callFactory: okhttp3.Call.Factory = hc5CallFactoryOf()
}
