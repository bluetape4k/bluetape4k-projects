package io.bluetape4k.grpc.testing.integration

import io.bluetape4k.grpc.AbstractGrpcServer
import io.bluetape4k.logging.KLogging
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import java.util.concurrent.Executors

class TestServiceServer private constructor(
    builder: ServerBuilder<*>,
    services: List<BindableService>,
): AbstractGrpcServer(builder, services) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(port: Int): TestServiceServer {
            return invoke(ServerBuilder.forPort(port))
        }

        @JvmStatic
        operator fun invoke(builder: ServerBuilder<*>): TestServiceServer {
            return TestServiceServer(builder, listOf(TestServiceImpl()))
        }
    }

    override fun createServer(): Server {
        val executor = Executors.newSingleThreadScheduledExecutor()
        val service = ServerInterceptors.intercept(TestServiceImpl(executor), TestServiceImpl.interceptors)

        return builder
            .addService(service)
            .build()
    }
}
