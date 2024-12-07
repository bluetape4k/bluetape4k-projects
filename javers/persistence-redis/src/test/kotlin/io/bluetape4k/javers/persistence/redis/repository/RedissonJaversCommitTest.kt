package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.repository.AbstractJaversCommitTest

class RedissonJaversCommitTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    private val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }

    override fun newJavers(): Javers {
        // NOTE: 각각의 테스트가 Javers를 매번 새롭게 만들고, Snapshot정보를 clear해야 하므로 Redis를 Flush합니다.
        redisson.keys.flushdb()

        val repository = RedissonCdoSnapshotRepository("bluetape4k:redisson", redisson)

        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }
}
