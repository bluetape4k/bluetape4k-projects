package io.bluetape4k.workshop.quarkus

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.mutiny.deferUni
import io.bluetape4k.mutiny.onEach
import io.bluetape4k.workshop.quarkus.client.GreetingReactiveClient
import io.bluetape4k.workshop.quarkus.config.GreetingConfig
import io.bluetape4k.workshop.quarkus.model.Greeting
import io.bluetape4k.workshop.quarkus.services.GreetingReactiveService
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.jboss.resteasy.reactive.RestStreamElementType
import org.jetbrains.annotations.Blocking

/**
 * Quarkus Reactive with Kotlin
 *
 * 참고: [Quarkus - Using Kotlin](https://quarkus.io/guides/kotlin)
 */
@Path("/reactive")
@Produces(MediaType.APPLICATION_JSON)
class GreetingReactiveResource(private val greetingService: GreetingReactiveService) {

    companion object: KLogging()

    /**
     * Configuration 정보를 injection 받습니다
     */
    @Inject
    internal lateinit var greetingConfig: GreetingConfig

    /**
     * Current Resource에 대한 [UriInfo] 정보
     */
    @Context
    internal lateinit var uriInfo: UriInfo


    /**
     * `@RestClient` 를 적용하여 Inject 받을 수 있습니다.
     * baseUri 등의 정보는 application. properties 에서 읽어오게 하면 됩니다. (참고: rest-client-demo)
     *
     * NOTE: 실제 사용 시에는 @RestClient 로 injection 받는 것이 낫다 (baseUri 를 얻기 위해서 어쩔 수 없이 RestClientBuilder를 사용하였다)
     */
    private val greetingClient: GreetingReactiveClient by lazy {
        log.info { "Create GreetingReactiveClient. baseUri=${uriInfo.baseUri}" }
        RestClientBuilder.newBuilder()
            .baseUri(uriInfo.baseUri)             // or quarkus.test-host.url 을 써도 된다.
            .build(GreetingReactiveClient::class.java)
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking                           // Workder thread 에서 실행하도록 명시합니다. (동기방식으로)
    fun hello(): String {
        log.debug { "call blocking method" }
        Thread.sleep(100)
        return "${greetingConfig.message} ${greetingConfig.name}${greetingConfig.suffix}"
    }

    @GET
    @Path("/sequential-hello")
    @Produces(MediaType.TEXT_PLAIN)
    fun sequentialHello(): Multi<String> {
        return Multi.createBy()
            .repeating()
            .deferUni { getRemoteHello() }
            .atMost(4)
            .onEach { string ->
                log.debug { "sequential emit: $string" }
            }
    }

    @GET
    @Path("/greeting/{name}")
    fun greeting(name: String): Uni<Greeting> {
        return greetingService.greeting(name)
    }

    @GET
    @Path("/sequential-greeting/{name}")
    fun sequentialGreeting(name: String): Multi<Greeting> {
        return Multi.createBy()
            .repeating()
            .deferUni { getGreeting(name) }
            .atMost(4)
            .onEach { greeting ->
                log.debug { "sequential emit: $greeting" }
            }
    }

    @GET
    @Path("/greeting/{count}/{name}")
    fun greetings(count: Int, name: String): Multi<Greeting> {
        return greetingService.greetings(count, name)
    }

    @GET
    @Path("/stream/{count}/{name}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    fun greetingAsStream(count: Int, name: String): Multi<Greeting> {
        return greetingService.greetings(count, name)
    }

    private fun getRemoteHello(): Uni<String> {
        log.debug { "Get hello via remote client" }
        return greetingClient.hello()
    }

    private fun getGreeting(name: String): Uni<Greeting> {
        log.debug { "Get greeting via remote client. name=$name" }
        return greetingClient.greeting(name)
    }
}
