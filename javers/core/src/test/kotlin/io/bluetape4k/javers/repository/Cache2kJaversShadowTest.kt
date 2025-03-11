package io.bluetape4k.javers.repository

import io.bluetape4k.javers.repository.cache2k.Cache2KCdoSnapshotRepository
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.AbstractJaversShadowTest

class Cache2kJaversShadowTest: AbstractJaversShadowTest() {

    override fun prepareJaversRepository(): JaversRepository =
        Cache2KCdoSnapshotRepository()
}
