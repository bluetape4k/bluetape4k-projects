package io.bluetape4k.retrofit2.client.okhttp3

import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderCoroutineTest
import okhttp3.Call

class OkHttpJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLoggingChannel()

    override val callFactory: Call.Factory = okhttp3Client()

}
