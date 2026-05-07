package io.bluetape4k.grpc

import io.bluetape4k.grpc.examples.helloworld.GreeterService
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [managedChannel] 및 [grpcServer]/[grpcServerBuilder] 팩토리 함수 검증 테스트
 */
class ManagedChannelSupportTest {
    companion object : KLogging()

    @Test
    fun `managedChannel - host port 기반 채널을 생성한다`() {
        val channel = managedChannel("localhost", 50051) { usePlaintext() }
        channel.shouldNotBeNull()
        channel.shutdownNow()
    }

    @Test
    fun `managedChannel - target 기반 채널을 생성한다`() {
        val channel = managedChannel("localhost:50052") { usePlaintext() }
        channel.shouldNotBeNull()
        channel.shutdownNow()
    }

    @Test
    fun `managedChannel - blank host는 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel("", 50051) { usePlaintext() }.shutdownNow()
        }
        assertFailsWith<IllegalArgumentException> {
            managedChannel("  ", 50051) { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel - port 0은 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel("localhost", 0) { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel - port 65536은 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel("localhost", 65536) { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel - blank target은 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            managedChannel("") { usePlaintext() }.shutdownNow()
        }
    }

    @Test
    fun `managedChannel - 유효한 port 경계값 1은 채널을 생성한다`() {
        val channel = managedChannel("localhost", 1) { usePlaintext() }
        channel.shouldNotBeNull()
        channel.shutdownNow()
    }

    @Test
    fun `managedChannel - 유효한 port 경계값 65535는 채널을 생성한다`() {
        val channel = managedChannel("localhost", 65535) { usePlaintext() }
        channel.shouldNotBeNull()
        channel.shutdownNow()
    }

    @Test
    fun `grpcServerBuilder - 유효한 포트로 ServerBuilder를 생성한다`() {
        val builder = grpcServerBuilder(9090) { addService(GreeterService()) }
        builder.shouldNotBeNull()
    }

    @Test
    fun `grpcServerBuilder - port 0은 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            grpcServerBuilder(0) {}
        }
    }

    @Test
    fun `grpcServerBuilder - port 65536은 IllegalArgumentException을 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            grpcServerBuilder(65536) {}
        }
    }
}
