package io.bluetape4k.http.hc5.routing

import org.apache.hc.client5.http.SchemePortResolver
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver
import org.apache.hc.client5.http.routing.RoutingSupport
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequest

/**
 * HTTP 처리에서 `determineHost` 함수를 제공합니다.
 */
fun HttpRequest.determineHost(): HttpHost = RoutingSupport.determineHost(this)

/**
 * Normalize the target host.
 *
 * @param schemePortResolver Scheme port resolver.
 * @return normalized host.
 */
fun HttpHost.normalize(
    schemePortResolver: SchemePortResolver = DefaultSchemePortResolver.INSTANCE,
): HttpHost =
    RoutingSupport.normalize(this, schemePortResolver)
