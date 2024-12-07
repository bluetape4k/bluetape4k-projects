package io.bluetape4k.workshop.quarkus.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.quarkus.model.Greeting
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

/**
 * `@ApplicationScoped` 는 proxy를 이용하여 lazy initialization을 수행하고,
 * `@Singleton` 은 proxy를 사용하지 않고, injection 수행 시 생성한다.
 *
 * As a general rule of thumb, the recommendation is to use @ApplicationScoped by
 * default unless there is a compelling reason to use @Singleton. @ApplicationScoped
 * allows for more flexibility during live coding as well as when unit testing an application.
 */
@Singleton
class GreetingCoroutineService {

    companion object: KLogging()

    suspend fun greeting(name: String): Greeting {
        log.debug { "Greeting with name=$name" }
        delay(100L)
        return Greeting("Hello $name")
    }

    fun greetings(count: Int, name: String): Flow<Greeting> {
        log.debug { "Greetings with count=$count, name=$name" }

        return flow {
            repeat(count) {
                val greeting = Greeting("Hello $name - $it")
                emit(greeting)
            }
        }.onEach { greeting ->
            delay(100.milliseconds)
            log.debug { "emit $greeting" }
        }
    }
}
