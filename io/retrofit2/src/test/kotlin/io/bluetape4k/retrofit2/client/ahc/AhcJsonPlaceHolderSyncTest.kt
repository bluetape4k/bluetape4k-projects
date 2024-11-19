package io.bluetape4k.retrofit2.client.ahc

import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderSyncTest
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactoryOf
import okhttp3.Call

class AhcJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderSyncTest() {

    override val callFactory: Call.Factory = asyncHttpClientCallFactoryOf()

}
