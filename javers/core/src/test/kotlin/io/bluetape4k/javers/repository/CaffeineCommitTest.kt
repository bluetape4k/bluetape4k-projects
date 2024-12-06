package io.bluetape4k.javers.repository

import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.repository.AbstractJaversCommitTest

class CaffeineCommitTest: AbstractJaversCommitTest() {

    override fun newJavers(): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(CaffeineCdoSnapshotRepository())
            .build()
}
