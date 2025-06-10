package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNullOrBlank

abstract class AbstractJsonPlaceHolderApiTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        val faker = Fakers.faker

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

    fun newPost(): Post = Post(
        userId = faker.random().nextInt(1, 1000),
        id = faker.random().nextInt(1, 1000),
        title = faker.book().title(),
        body = Fakers.randomString(256, 2048)
    )
}
