package io.bluetape4k.retrofit2.client.ahc

import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactoryOf
import okhttp3.Call

class AhcJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    override val callFactory: Call.Factory = asyncHttpClientCallFactoryOf()

}
