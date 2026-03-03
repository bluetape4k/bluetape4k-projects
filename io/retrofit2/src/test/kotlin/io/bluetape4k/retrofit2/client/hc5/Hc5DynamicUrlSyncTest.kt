package io.bluetape4k.retrofit2.client.hc5

import io.bluetape4k.retrofit2.client.AbstractDynamicUrlSyncTest
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import okhttp3.Call

class Hc5DynamicUrlSyncTest: AbstractDynamicUrlSyncTest() {

    override val callFactory: Call.Factory = hc5CallFactoryOf()
}
