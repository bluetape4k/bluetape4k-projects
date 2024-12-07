package io.bluetape4k.workshop.quarkus.client

import io.bluetape4k.workshop.quarkus.model.Greeting
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestStreamElementType

@RegisterRestClient(configKey = "reactive")
@Path("/reactive")
interface GreetingReactiveClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): Uni<String>

    @GET
    @Path("/greeting/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun greeting(@PathParam("name") name: String): Uni<Greeting>

    // NOTE: Multi<Greeting> 을 반환하면, 클라이언트에서는 Uni<List<Greeting>> 으로 받아야 한다.
    @GET
    @Path("/greeting/{count}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun greetings(
        @PathParam("count") count: Int,
        @PathParam("name") name: String,
    ): Uni<List<Greeting>>

    /**
     * 서버에서 SSE Multi 로 전송한 것을 Multi로 받고자 한다면,
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
