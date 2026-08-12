package io.bluetape4k.gradle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestcontainersTestSerializationTest {

    @Test
    fun `testcontainers module serializes its test tasks`() {
        assertTrue(
            shouldSerializeTestcontainersTests(
                projectPath = TESTCONTAINERS_PROJECT_PATH,
                dependencyProjectPaths = emptyList(),
            ),
        )
    }

    @Test
    fun `direct testcontainers project dependency serializes test tasks`() {
        assertTrue(
            shouldSerializeTestcontainersTests(
                projectPath = ":bluetape4k-vertx",
                dependencyProjectPaths = listOf(":bluetape4k-core", TESTCONTAINERS_PROJECT_PATH),
            ),
        )
    }

    @Test
    fun `non Docker module keeps parallel test execution`() {
        assertFalse(
            shouldSerializeTestcontainersTests(
                projectPath = ":bluetape4k-core",
                dependencyProjectPaths = listOf(":bluetape4k-logging"),
            ),
        )
    }
}
