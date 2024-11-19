package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderSyncTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call

class VertxJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderSyncTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = vertxCallFactoryOf()

}
