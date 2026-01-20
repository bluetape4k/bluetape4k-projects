package io.bluetape4k.javers.examples

import io.bluetape4k.javers.repository.jql.queryByInstanceId
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldHaveSize
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.junit.jupiter.api.Test

class CommitAndQueryExamples {

    companion object: KLogging()

    private val javers: Javers = JaversBuilder.javers().build()

    @Test
    fun `Javers 저장소에 commit 하고 변화를 조회하기`() {
        val robert = Person("bob", "Robert Martin")
        javers.commit("user", robert)

        robert.name = "Robert C."
        robert.position = Position.Developer
        javers.commit("user", robert)

        val query = queryByInstanceId<Person>("bob")

        val shadows = javers.findShadows<Person>(query)
        log.debug { "shadows:" }
        shadows.forEach { log.debug { "\t$it" } }
        shadows shouldHaveSize 2

        val snapshots = javers.findSnapshots(query)
        log.debug { "snapshots:" }
        snapshots.forEach { log.debug { "\t$it" } }
        snapshots shouldHaveSize 2

        val changes = javers.findChanges(query)
        log.debug { "changes:" }
        changes.forEach { log.debug { "\t$it" } }
        changes shouldHaveSize 5
    }
}
