package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.repository.AbstractJaversCommitTest

class RedissonJaversCommitTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    override fun newJavers(): Javers {
        // NOTE: 각각의 테스트가 Javers를 매번 새롭게 만들고, Snapshot정보를 clear해야 하므로 Redis를 Flush합니다.
        val redisson = RedisServer.Launcher.RedissonLib.getRedisson()
        redisson.keys.flushdb()

        val repository = RedissonCdoSnapshotRepository("kommons:redisson", redisson)

        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }
}
