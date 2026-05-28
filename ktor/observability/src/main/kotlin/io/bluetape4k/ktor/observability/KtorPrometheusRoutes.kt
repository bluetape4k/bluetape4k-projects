package io.bluetape4k.ktor.observability

import io.bluetape4k.support.requireNotBlank
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Adds a Prometheus scrape endpoint backed by [PrometheusMeterRegistry].
 *
 * ## Contract
 * - The registry is supplied by the application.
 * - The helper only exposes the route; it does not create exporters or global registries.
 */
fun Route.prometheusScrapeRoute(
    registry: PrometheusMeterRegistry,
    path: String = "/metrics",
) {
    path.requireAbsoluteKtorPath("path")

    get(path) {
        call.respondText(
            text = registry.scrape(),
            contentType = ContentType.Text.Plain
        )
    }
}

private fun String.requireAbsoluteKtorPath(parameterName: String): String {
    requireNotBlank(parameterName)
    require(startsWith("/")) { "$parameterName[$this] must start with '/'." }
    return this
}
