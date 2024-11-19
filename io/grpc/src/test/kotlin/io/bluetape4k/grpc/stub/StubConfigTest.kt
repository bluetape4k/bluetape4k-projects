package io.bluetape4k.grpc.stub

import io.bluetape4k.grpc.testing.integration.Messages
import io.bluetape4k.grpc.testing.integration.TestServiceGrpc
import io.grpc.Channel
import io.grpc.Deadline
import io.grpc.MethodDescriptor
import io.grpc.stub.StreamObserver
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class StubConfigTest {

    private val channel = mockk<Channel>()
    private val responseObserver = mockk<StreamObserver<Messages.SimpleResponse>>()

    @BeforeEach
    fun setup() {
        clearMocks(channel, responseObserver)
        val call = NoopClientCall<Messages.SimpleRequest, Messages.SimpleResponse>()
        every {
            channel.newCall(
                any<MethodDescriptor<Messages.SimpleRequest, Messages.SimpleResponse>>(),
                any()
            )
        } returns call
    }

    @Test
    fun `configure dead line`() {
        val deadline = Deadline.after(2, TimeUnit.NANOSECONDS)

        // create a default stub
        val stub = TestServiceGrpc.newBlockingStub(channel)
        stub.callOptions.deadline.shouldBeNull()

        // Reconfigure it
        val reconfiguredStub = stub.withDeadline(deadline)

        // New altered config
        reconfiguredStub.callOptions.deadline shouldBeEqualTo deadline

        // Default config unchanged
        stub.callOptions.deadline.shouldBeNull()
    }

    @Test
    fun `stub call options populated to new call`() {
        val stub = TestServiceGrpc.newStub(channel)
        val options1 = stub.callOptions
        val request = Messages.SimpleRequest.getDefaultInstance()

        stub.unaryCall(request, responseObserver)
        verify { channel.newCall(eq(TestServiceGrpc.getUnaryCallMethod()), eq(options1)) }

        val stub2 = stub.withDeadlineAfter(2, TimeUnit.NANOSECONDS)
        val options2 = stub2.callOptions
        options2 shouldNotBeEqualTo options1
        stub2.unaryCall(request, responseObserver)
        verify { channel.newCall(eq(TestServiceGrpc.getUnaryCallMethod()), eq(options2)) }
    }
}
