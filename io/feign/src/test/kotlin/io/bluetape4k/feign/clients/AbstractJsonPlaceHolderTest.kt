package io.bluetape4k.feign.clients

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.feign.services.Album
import io.bluetape4k.feign.services.Comment
import io.bluetape4k.feign.services.Post
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNullOrBlank

@RandomizedTest
abstract class AbstractJsonPlaceHolderTest {

    companion object: KLogging() {

        const val REPEAT_SIZE = 3

        @JvmStatic
        val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        @JvmStatic
        protected fun Post.verify() {
            log.trace { "Post=$this" }

            id shouldBeGreaterThan 0
            userId shouldBeGreaterThan 0
            title.shouldNotBeNullOrBlank()
            body.shouldNotBeNullOrBlank()
        }

        @JvmStatic
        protected fun Comment.verify() {
            log.trace { "Comment=$this" }

            id shouldBeGreaterThan 0
            postId shouldBeGreaterThan 0
            name.shouldNotBeNullOrBlank()
            email.shouldNotBeNullOrBlank()
            body.shouldNotBeNullOrBlank()
        }

        @JvmStatic
        protected fun Album.verify() {
            log.trace { "Album=$this" }
            id shouldBeGreaterThan 0
            title.shouldNotBeEmpty()
        }
    }
}
