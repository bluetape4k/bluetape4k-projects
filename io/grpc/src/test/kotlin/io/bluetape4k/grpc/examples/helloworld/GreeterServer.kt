package io.bluetape4k.grpc.examples.helloworld

import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessServer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank

/**
 * GreeterServer
 */
class GreeterServer private constructor(name: String): AbstractGrpcInprocessServer(name, GreeterService()) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(name: String): GreeterServer {
            name.requireNotBlank("name")
            return GreeterServer(name)
        }
    }
}
