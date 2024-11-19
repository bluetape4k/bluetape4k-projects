package io.bluetape4k.retrofit2.client.okhttp3

import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractJsonPlaceHolderSyncTest
import okhttp3.Call

class OkHttpJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderSyncTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = okhttp3Client { }

}
