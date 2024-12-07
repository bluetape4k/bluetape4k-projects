package io.bluetape4k.workshop.quarkus.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.quarkus.model.Greeting
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration

@ApplicationScoped
class GreetingReactiveService {

    companion object: KLogging()

    fun greeting(name: String): Uni<Greeting> {
        log.debug { "Greeting with name=$name" }

        return Uni.createFrom()
            .item(name)
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().transform { Greeting("Hello $it") }
            .emitOn(Infrastructure.getDefaultExecutor())
    }

    fun greetings(count: Int, name: String): Multi<Greeting> {
        log.debug { "Greetings with count=$count, name=$name" }

        return Multi.createFrom()
            .ticks().every(Duration.ofMillis(100))
            .onItem().transform { n -> Greeting("Hello $name - $n") }
            .onItem().invoke { greeting -> log.debug { "emit $greeting" } }
            .select().first(count.toLong())
            .emitOn(Infrastructure.getDefaultExecutor())
    }
}
