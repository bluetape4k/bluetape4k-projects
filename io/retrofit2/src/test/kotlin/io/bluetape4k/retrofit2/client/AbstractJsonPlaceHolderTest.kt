package io.bluetape4k.retrofit2.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeNullOrBlank
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.retrofit2.AbstractRetrofitTest
import io.bluetape4k.retrofit2.services.HttpbinAnythingResponse
import io.bluetape4k.retrofit2.services.Post

abstract class AbstractJsonPlaceHolderTest: AbstractRetrofitTest() {

    companion object: KLogging() {

        const val REPEAT_SIZE = 3

        @JvmStatic
        protected fun Post.verify() {
            log.trace { "Post=$this" }

            title.shouldNotBeNullOrBlank()
            body.shouldNotBeNullOrBlank()
        }

        @JvmStatic
        protected fun HttpbinAnythingResponse.verify(method: String, path: String) {
            log.trace { "Httpbin response=$this" }
            this.method.shouldNotBeNull() shouldBeEqualTo method
            this.url.shouldNotBeNull().shouldContain(path)
        }

        @JvmStatic
        protected fun HttpbinAnythingResponse.verifyQuery(name: String, value: Int) {
            args[name].shouldNotBeNull() shouldBeEqualTo value.toString()
        }

        @JvmStatic
        protected fun HttpbinAnythingResponse.verifyJsonPost(post: Post) {
            val bodyJson = json.shouldNotBeNull()
            bodyJson["userId"].toString().toInt() shouldBeEqualTo post.userId
            bodyJson["title"].toString() shouldBeEqualTo post.title
            bodyJson["body"].toString() shouldBeEqualTo post.body
        }
    }

    protected abstract val callFactory: okhttp3.Call.Factory

}
