package io.bluetape4k.retrofit2.client

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.retrofit2.services.Album
import io.bluetape4k.retrofit2.services.Comment
import io.bluetape4k.retrofit2.services.Post
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNullOrBlank

abstract class AbstractJsonPlaceHolderTest {

    companion object: KLogging() {

        const val REPEAT_SIZE = 3

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

    protected abstract val callFactory: okhttp3.Call.Factory

}
