package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.retrofit2.client.AbstractDynamicUrlSyncTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call

class VertxDynamicUrlSyncTest: AbstractDynamicUrlSyncTest() {

    override val callFactory: Call.Factory = vertxCallFactoryOf()
}
