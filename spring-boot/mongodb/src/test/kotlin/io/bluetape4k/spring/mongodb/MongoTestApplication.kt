package io.bluetape4k.spring.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.MongoDBServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration

/**
 * Spring Boot 4 MongoDB 통합 테스트용 애플리케이션 클래스입니다.
 *
 * Spring Boot 4에는 `@DataMongoTest` 슬라이스가 아직 없으므로 `@SpringBootTest`로 대체합니다.
 * companion object에서 [MongoDBServer]를 미리 시작하고, [MongoConfig]에서 직접
 * Testcontainers URL로 MongoDB 클라이언트를 설정합니다.
 */
@SpringBootApplication
class MongoTestApplication {
    companion object: KLoggingChannel() {
        val mongoServer: MongoDBServer = MongoDBServer.Launcher.mongoDB
    }

    /**
     * Testcontainers MongoDB 서버에 직접 연결하는 Reactive MongoDB 설정입니다.
     *
     * `@DynamicPropertySource` 대신 `AbstractReactiveMongoConfiguration`을 상속하여
     * Bean 생성 시점에 MongoDB URL을 직접 설정합니다.
     */
    @Configuration(proxyBeanMethods = false)
    class MongoConfig: AbstractReactiveMongoConfiguration() {
        override fun getDatabaseName(): String = MongoDBServer.DATABASE_NAME

        override fun configureClientSettings(builder: MongoClientSettings.Builder) {
            builder.applyConnectionString(
                ConnectionString(MongoDBServer.Launcher.mongoDB.url)
            )
        }
    }
}
