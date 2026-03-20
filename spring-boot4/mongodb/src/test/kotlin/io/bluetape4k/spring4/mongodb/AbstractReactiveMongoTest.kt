package io.bluetape4k.spring4.mongodb

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.MongoDBServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Spring Data MongoDB Reactive 통합 테스트의 기반 클래스입니다.
 *
 * [MongoDBServer] Testcontainer를 통해 Docker 기반 MongoDB를 자동으로 실행하며,
 * [DynamicPropertySource]로 `spring.data.mongodb.uri`를 동적으로 설정합니다.
 *
 * **사용 방법**: 구체 테스트 클래스에 `@DataMongoTest` 어노테이션을 붙이고 이 클래스를 상속합니다.
 *
 * ```kotlin
 * @DataMongoTest
 * class MyMongoTest: AbstractReactiveMongoTest() {
 *     @Test
 *     fun `some test`() = runTest {
 *         val user = mongoOperations.insertSuspending(User(name = "Alice"))
 *         user.id.shouldNotBeNull()
 *     }
 * }
 * ```
 */
abstract class AbstractReactiveMongoTest {
    companion object : KLoggingChannel() {
        /** 지연 초기화되는 MongoDB 테스트 서버입니다. */
        val mongoServer: MongoDBServer by lazy { MongoDBServer.Launcher.mongoDB }

        /** 테스트 데이터 생성에 사용하는 Faker 인스턴스입니다. */
        val faker = Fakers.faker

        /**
         * `spring.data.mongodb.uri` 프로퍼티를 Testcontainer의 연결 URL로 설정합니다.
         *
         * ## 동작/계약
         * - Spring Framework 5.2.5+ 에서 추상 클래스의 [DynamicPropertySource]는 하위 클래스에 상속됩니다.
         * - `MongoDBServer.Launcher.mongoDB`를 첫 접근 시 자동으로 시작합니다.
         */
        @JvmStatic
        @DynamicPropertySource
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongoServer.url }
        }
    }

    /** 자동 주입되는 [ReactiveMongoOperations] 인스턴스입니다. */
    @Autowired
    protected lateinit var mongoOperations: ReactiveMongoOperations
}
