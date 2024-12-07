package io.bluetape4k.workshop.quarkus.client

import io.bluetape4k.workshop.quarkus.model.Greeting
import io.smallrye.mutiny.Multi
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestStreamElementType

/**
 * `quarkus-rest-client` 모듈을 이용하여 Feign, Retrofit2 처럼 외부 REST API 를 호출하기 위한 Client용 Interface를 정의합니다.
 *
 * 참고: [USING THE REST CLIENT](https://quarkus.io/guides/rest-client)
 *
 * Programming 방식으로는 다음과 같이 [RestClientBuilder] 를 사용하여 빌드할 수 있고 (Retrofit2 방식)
 *
 * ```
 * val greetingClient = RestClientBuilder.newBuilder()
 *          .baseUri(uriInfo.baseUri)
 *          .build(CoroutineGreetingClient::class.java)
 *
 * val greeting = greetingClient.greeting("debop")  // return Greeting(message="Hello debop")
 * ```
 *
 * [RegisterRestClient]로 등록된 REST Client를 inject 받아서 사용해도 된다
 *
 * ```
 * @Inject
 * @RestClient
 * internal lateinit var greetingClient: CoroutineGreetingClient
 * ```
 */
@RegisterRestClient(configKey = "coroutine")
@Path("/coroutine")
interface GreetingCoroutineClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String

    @GET
    @Path("/greeting/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun greeting(@PathParam("name") name: String): Greeting

    @GET
    @Path("/greeting/{count}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun greetings(
        @PathParam("count") count: Int,
        @PathParam("name") name: String,
    ): List<Greeting>

    /**
     * 서버에서 SSE Multi 로 전송한 것을 Flow로 받는 것은 안된다. [Multi] 로 받은 후, `asFlow` 로 변환해서 사용해야 한다.
     *
     * ```
     * @Produces(MediaType.SERVER_SENT_EVENTS)
     * @RestSseElementType(MediaType.APPLICATION_JSON)
     * ```
     * 로 정의해주어야 한다
     */
    @GET
    @Path("/stream/{count}/{name}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    fun greetingAsStream(
        @PathParam("count") count: Int,
        @PathParam("name") name: String,
    ): Multi<Greeting>
}
