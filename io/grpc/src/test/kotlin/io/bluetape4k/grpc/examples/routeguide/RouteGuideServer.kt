package io.bluetape4k.grpc.examples.routeguide

import io.bluetape4k.grpc.AbstractGrpcServer
import io.bluetape4k.logging.KLogging
import io.grpc.BindableService
import io.grpc.ServerBuilder

class RouteGuideServer private constructor(
    builder: ServerBuilder<*>,
    services: List<BindableService>,
): AbstractGrpcServer(builder, services) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(port: Int, featureData: String = defaultFeatureString()): RouteGuideServer {
            return invoke(ServerBuilder.forPort(port), featureData.parseJsonFeatures())
        }

        @JvmStatic
        operator fun invoke(builder: ServerBuilder<*>, features: Collection<Feature>): RouteGuideServer {
            return RouteGuideServer(builder, listOf(RouteGuideService(features)))
        }
    }
}
