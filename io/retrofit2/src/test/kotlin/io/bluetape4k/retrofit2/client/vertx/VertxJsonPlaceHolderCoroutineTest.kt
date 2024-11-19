package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call

class VertxJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = vertxCallFactoryOf()

}
