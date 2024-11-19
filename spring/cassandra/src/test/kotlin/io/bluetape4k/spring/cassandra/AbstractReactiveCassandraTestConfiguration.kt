package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import io.bluetape4k.testcontainers.storage.CassandraServer
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.config.SessionBuilderConfigurer

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = [AllPossibleTypes::class])
abstract class AbstractReactiveCassandraTestConfiguration: AbstractReactiveCassandraConfiguration() {

    // NOTE: 테스트 시에는 testcontainers 를 실행하기 위해 CassandraServer.Launcher 작업을 미리 수행해야 합니다.
    //
    companion object {
        const val DEFAULT_KEYSPACE = "examples"

        val server by lazy { CassandraServer.Launcher.cassandra4 }

        init {
            // default keyspace 를 재생성합니다.
            CassandraServer.Launcher.recreateKeyspace(DEFAULT_KEYSPACE)
        }
    }

    override fun getPort(): Int = CassandraServer.Launcher.cassandra4.port

    override fun getContactPoints(): String = CassandraServer.Launcher.cassandra4.host

    override fun getKeyspaceName(): String = DEFAULT_KEYSPACE

    override fun getLocalDataCenter(): String = CassandraServer.LOCAL_DATACENTER1

    override fun getSchemaAction(): SchemaAction = SchemaAction.RECREATE

    override fun getRequiredSession(): CqlSession {
        return CassandraServer.Launcher.newCqlSessionBuilder()
            .withKeyspace(keyspaceName)
            .apply { sessionBuilderConfigurer.configure(this) }
            .build()
    }

    /**
     * Custom Configuration을 사용하기 위해 (Profiles 등)
     */
    override fun getSessionBuilderConfigurer(): SessionBuilderConfigurer {
        return SessionBuilderConfigurer {
            it.withConfigLoader(DriverConfigLoader.fromClasspath("application.conf"))
        }
    }

    // NOTE: 테스트 대상 Entity가 있는 Package를 지정하거나 @EntityScan 을 사용하세요
    // override fun getEntityBasePackages(): Array<String> = arrayOf(AllPossibleTypes::class.packageName)
}
