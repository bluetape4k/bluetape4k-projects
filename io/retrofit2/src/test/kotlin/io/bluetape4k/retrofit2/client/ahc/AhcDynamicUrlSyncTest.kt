package io.bluetape4k.retrofit2.client.ahc

import io.bluetape4k.retrofit2.client.AbstractDynamicUrlSyncTest
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactoryOf
import okhttp3.Call

class AhcDynamicUrlSyncTest: AbstractDynamicUrlSyncTest() {

    override val callFactory: Call.Factory = asyncHttpClientCallFactoryOf()
}
