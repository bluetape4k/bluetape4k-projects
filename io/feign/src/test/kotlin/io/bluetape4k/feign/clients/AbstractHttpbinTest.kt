package io.bluetape4k.feign.clients

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.services.HttpbinAnythingResponse
import io.bluetape4k.feign.services.Post
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank

@RandomizedTest
abstract class AbstractHttpbinTest: AbstractFeignTest() {

    companion object: KLogging() {

        const val REPEAT_SIZE = 3

        @JvmStatic
        val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

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
}
