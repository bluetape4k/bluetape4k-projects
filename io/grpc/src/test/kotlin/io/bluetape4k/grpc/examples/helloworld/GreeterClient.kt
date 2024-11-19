package io.bluetape4k.grpc.examples.helloworld

import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank


class GreeterClient private constructor(name: String): AbstractGrpcInprocessClient(name) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(name: String): GreeterClient {
            name.requireNotBlank("name")
            return GreeterClient(name)
        }
    }

    private val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    suspend fun sayHello(name: String): String {
        log.debug { "Sending request to grpcServer..." }
        val request = HelloRequest.newBuilder().setName(name).build()
        return stub.sayHello(request).message
    }
}
