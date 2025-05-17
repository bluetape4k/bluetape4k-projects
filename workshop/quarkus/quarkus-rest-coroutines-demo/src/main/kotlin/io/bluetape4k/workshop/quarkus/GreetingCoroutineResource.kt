package io.bluetape4k.workshop.quarkus

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.quarkus.resteasy.AbstractCoroutineDefaultResource
import io.bluetape4k.workshop.quarkus.client.GreetingCoroutineClient
import io.bluetape4k.workshop.quarkus.config.GreetingConfig
import io.bluetape4k.workshop.quarkus.model.Greeting
import io.bluetape4k.workshop.quarkus.services.GreetingCoroutineService
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.UriInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.jboss.resteasy.reactive.RestStreamElementType

@Path("/coroutine")
@Produces(MediaType.APPLICATION_JSON)
class GreetingCoroutineResource(
    private val greetingService: GreetingCoroutineService,
): AbstractCoroutineDefaultResource() {

    companion object: KLoggingChannel()

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
    private val greetingClient: GreetingCoroutineClient by lazy {
        log.info { "Create GreetingCoroutineClient. baseUri=${uriInfo.baseUri}" }
        RestClientBuilder.newBuilder()
            .baseUri(uriInfo.baseUri)             // or quarkus.test-host.url 을 써도 된다.
            .build(GreetingCoroutineClient::class.java)
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String {
        log.debug { "call suspend method" }
        delay(10L)
        return "${greetingConfig.message} ${greetingConfig.name}${greetingConfig.suffix}"
    }

    @GET
    @Path("/sequential-hello")
    @Produces(MediaType.TEXT_PLAIN)
    fun sequentialHello(): Flow<String> = flow {
        repeat(4) {
            emit(getRemoteHello())
        }
    }.onEach { greeting ->
        log.debug { "sequential emit: $greeting" }
    }

    @GET
    @Path("/greeting/{name}")
    suspend fun greeting(name: String): Greeting {
        return greetingService.greeting(name)
    }

    @GET
    @Path("/sequential-greeting/{name}")
    fun sequentialGreeting(name: String): Flow<Greeting> = flow {
        repeat(4) {
            emit(getGreeting(name))
        }
    }.onEach { greeting ->
        log.debug { "sequential emit: $greeting" }
    }

    @GET
    @Path("/greeting/{count}/{name}")
    fun greetings(count: Int, name: String): Flow<Greeting> {
        log.debug { "greetings with count=$count, name=$name" }
        return greetingService.greetings(count, name)
    }

    @GET
    @Path("/stream/{count}/{name}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    fun greetingAsStream(count: Int, name: String): Flow<Greeting> {
        log.debug { "greetingAsStream with count=$count, name=$name" }
        return greetingService.greetings(count, name)
    }

    /**
     * [GreetingCoroutineClient] 를 이용하여 [io.bluetape4k.workshop.quarkus.GreetingCoroutineResource] 를 호출합니다
     *
     * 외부 REST API 를 호출할 때도 [RestClientBuilder] 를 이용하여 Retroft2 처럼 사용할 수 있습니다.
     */
    private suspend fun getRemoteHello(): String {
        log.debug { "Get hello via remote client" }
        return greetingClient.hello()
    }

    private suspend fun getGreeting(name: String): Greeting {
        log.debug { "Get remote greeting. name=$name" }
        return greetingClient.greeting(name)
    }
}
