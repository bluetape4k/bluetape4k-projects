package io.bluetape4k.workshop.quarkus.exceptions

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.resteasy.reactive.RestResponse

@Provider
class ResourceExceptionManager: ExceptionMapper<Exception> {

    companion object: KLogging()

    @Inject
    internal lateinit var objectMapper: ObjectMapper

    override fun toResponse(exception: Exception): Response {
        log.error(exception) { "Fail to handle request" }

        val code = when (exception) {
            is WebApplicationException -> exception.response.status
            else                       -> RestResponse.StatusCode.INTERNAL_SERVER_ERROR
        }

        val node = objectMapper.createObjectNode()
        node.put("exceptionType", exception.javaClass.name)
        node.put("code", code)
        node.put("producer", "Bluetape4k ResourceExceptionManager")

        exception.message?.let { node.put("error", it) }

        return Response.status(code).entity(node).build()
    }
}
