package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.http.BluetapeHttpServer
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

abstract class AbstractJsonPlaceHolderApiTest {
    companion object: KLoggingChannel() {
        const val REPEAT_SIZE = 3

        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        @DynamicPropertySource
        fun startMockServer(@Suppress("UNUSED_PARAMETER") registry: DynamicPropertyRegistry) {
            // 서버 기동 → writeToSystemProperties() → testcontainers.bluetape-http.* 시스템 프로퍼티 자동 설정
            BluetapeHttpServer.Launcher.bluetapeHttpServer.url
        }

        @JvmStatic
        protected fun Post.verify() {
            log.debug { "Post=$this" }

            id shouldBeGreaterThan 0
            userId shouldBeGreaterThan 0
            title.shouldNotBeNullOrBlank()
            body.shouldNotBeNullOrBlank()
        }

        @JvmStatic
        protected fun Comment.verify() {
            log.debug { "Comment=$this" }

            id shouldBeGreaterThan 0
            postId shouldBeGreaterThan 0
            name.shouldNotBeNullOrBlank()
            email.shouldNotBeNullOrBlank()
            body.shouldNotBeNullOrBlank()
        }

        @JvmStatic
        protected fun Album.verify() {
            log.debug { "Album=$this" }
            id shouldBeGreaterThan 0
            title.shouldNotBeEmpty()
        }
    }

    fun newPost(): Post =
        Post(
            userId = faker.random().nextInt(999, 10000),
            id = faker.random().nextInt(999, 10000),
            title = faker.book().title(),
            body = Fakers.randomString(256, 2048)
        )
}
