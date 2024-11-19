package io.bluetape4k.grpc.stub

import io.grpc.ClientCall

/**
 * [NoopClientCall] is a class that is designed for use in tests.  It is designed to be used
 * in places where a scriptable call is necessary.  By default, all methods are noops, and designed
 * to be overridden.
 */
class NoopClientCall<ReqT, RespT>: ClientCall<ReqT, RespT>() {
    /**
     * [NoopClientCall.NoopClientCallListener] is a class that is designed for use in tests.
     * It is designed to be used in places where a scriptable call listener is necessary.  By
     * default, all methods are noops, and designed to be overridden.
     */
    class NoopClientCallListener<T>: Listener<T>()

    override fun start(responseListener: Listener<RespT>?, headers: io.grpc.Metadata?) {
        // Nothing to do 
    }

    override fun request(numMessages: Int) {}

    override fun cancel(message: String?, cause: Throwable?) {}

    override fun halfClose() {}

    override fun sendMessage(message: ReqT) {}
}
