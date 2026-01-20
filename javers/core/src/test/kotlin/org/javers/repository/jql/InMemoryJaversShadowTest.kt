package org.javers.repository.jql

import org.javers.repository.api.JaversRepository
import org.javers.repository.inmemory.InMemoryRepository

class InMemoryJaversShadowTest: AbstractJaversShadowTest() {

    override fun prepareJaversRepository(): JaversRepository = InMemoryRepository()

}
