package io.bluetape4k.grpc.examples.helloworld

/**
 * GreeterService
 *
 * @see hello_world.proto
 */
class GreeterService: GreeterGrpcKt.GreeterCoroutineImplBase() {

    /**
     * Returns the response to an RPC for io.grpc.examples.helloworld.Greeter.SayHello.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    override suspend fun sayHello(request: HelloRequest): HelloReply {
        return HelloReply.newBuilder()
            .setMessage("Hello ${request.name}")
            .build()
    }
}
