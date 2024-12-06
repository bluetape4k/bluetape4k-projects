package org.javers.core.repository

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.repository.inmemory.InMemoryRepository

class InMemoryCommitTest: AbstractJaversCommitTest() {

    override fun newJavers(): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(InMemoryRepository())
            .build()
}
