package io.bluetape4k.retrofit2.client.ahc

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactoryOf
import okhttp3.Call

class AhcJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLoggingChannel()

    override val callFactory: Call.Factory = asyncHttpClientCallFactoryOf()

}
