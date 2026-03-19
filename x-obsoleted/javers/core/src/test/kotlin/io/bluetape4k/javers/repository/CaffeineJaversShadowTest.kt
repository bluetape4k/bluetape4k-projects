package io.bluetape4k.javers.repository

import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.AbstractJaversShadowTest

class CaffeineJaversShadowTest: AbstractJaversShadowTest() {

    override fun prepareJaversRepository(): JaversRepository =
        CaffeineCdoSnapshotRepository()
}
