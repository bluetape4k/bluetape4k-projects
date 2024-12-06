package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.AbstractJaversShadowTest

class LettuceJaversShadowTest: AbstractJaversShadowTest() {

    companion object: KLogging()

    override fun prepareJaversRepository(): JaversRepository {
        // NOTE: 각각의 테스트가 Javers를 매번 새롭게 만들고, Snapshot정보를 clear해야 하므로 Redis를 Flush합니다.
        val lettuceClient = RedisServer.Launcher.LettuceLib.getRedisClient()
        lettuceClient.connect().sync().flushdb()
        return LettuceCdoSnapshotRepository("kommons:lettuce", lettuceClient)
    }
}
