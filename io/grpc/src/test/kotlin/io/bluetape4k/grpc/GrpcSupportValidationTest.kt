package io.bluetape4k.grpc

import io.bluetape4k.grpc.examples.helloworld.GreeterService
import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessClient
import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GrpcSupportValidationTest {

    @Test
    fun `managedChannel 은 blank host 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel(" ", 50051) { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel 은 범위를 벗어난 port 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel("localhost", 0) { usePlaintext() }.shutdownNow()
        }
        assertFailsWith<IllegalArgumentException> {
            managedChannel("localhost", 65536) { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel target 은 blank 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel(" ") { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `grpcServerBuilder 는 범위를 벗어난 port 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            grpcServerBuilder(0) { }
        }
        assertFailsWith<IllegalArgumentException> {
            grpcServerBuilder(65536) { }
        }
    }

    @Test
    fun `유효한 입력이면 managedChannel 과 grpcServerBuilder 를 생성할 수 있다`() {
        val channel = managedChannel("localhost", 50051) { usePlaintext() }
        channel.authority() shouldBeEqualTo "localhost:50051"
        channel.shutdownNow()

        val serverBuilder = grpcServerBuilder(50051) { addService(GreeterService()) }
        serverBuilder.shouldNotBeNull()
    }

    @Test
    fun `inprocess client 는 blank name 을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            object: AbstractGrpcInprocessClient(" ") {}
        }
    }

    @Test
    fun `inprocess client 는 invalid address port 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            object: AbstractGrpcInprocessClient("localhost", 0) {}
        }
    }

    @Test
    fun `inprocess server 는 blank name 과 invalid port 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            object: AbstractGrpcInprocessServer(" ", GreeterService()) {}
        }
        assertFailsWith<IllegalArgumentException> {
            object: AbstractGrpcInprocessServer(0, GreeterService()) {}
        }
    }
}
