package io.bluetape4k.ktor.core.consumer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.ktor.core.ApplicationResourceClosePhase
import io.bluetape4k.ktor.core.ApplicationResourceCloseReport
import io.bluetape4k.ktor.core.ApplicationResourceRegistry
import io.bluetape4k.ktor.core.ApplicationResourceRegistryState
import io.bluetape4k.ktor.core.installApplicationResourceLifecycle
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * 별도 package의 caller가 접근하는 public lifecycle 계약을 고정합니다.
 */
class ApplicationResourceLifecyclePublicApiTest {

    @Test
    fun `consumer can use registry report and registration token`() {
        val registry = ApplicationResourceRegistry()
        val resource = AutoCloseable { }
        val action: () -> Unit = { }

        val resourceRegistration = registry.register(resource)
        registry.register(action)
        resourceRegistration.close()
        registry.close()

        registry.closeReport shouldBeEqualTo ApplicationResourceCloseReport(
            state = ApplicationResourceRegistryState.CLOSED,
            attempted = 2,
            inFlight = 0,
            closed = 2,
            failures = emptyList()
        )
        ApplicationResourceClosePhase.values().toList().size shouldBeEqualTo 3
    }

    @Test
    fun `consumer can install lifecycle on a Ktor application`() = testApplication {
        application {
            val registry = installApplicationResourceLifecycle()
            registry.register(AutoCloseable { })
        }
    }

    @Test
    fun `implementation seams and token implementations are not public API`() {
        val holder = Class.forName("io.bluetape4k.ktor.core.ApplicationResourceLifecycleHolder")
        Modifier.isPublic(holder.modifiers) shouldBeEqualTo false

        val registration = Class.forName("io.bluetape4k.ktor.core.ApplicationResourceRegistration")
        registration.isInterface shouldBeEqualTo true
        registration.declaredConstructors.isEmpty() shouldBeEqualTo true
    }
}
