package io.bluetape4k.spring.core.io.buffer

import io.bluetape4k.io.getAllBytes
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.NettyDataBufferFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * DataBufferSupport 확장 함수 테스트
 */
class DataBufferSupportTest {

    private val bufferFactory = DefaultDataBufferFactory(true)

    private val nettyBufferFactory = NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT)

    @Test
    fun `InputStream을 DataBuffer Flow로 읽을 수 있다`() = runTest {
        val content = "hello world".toByteArray()
        val inputStream = ByteArrayInputStream(content)

        val result = inputStream.readAsDataBuffers(bufferFactory)
            .toList()
            .flatMap {
                it.readableByteBuffers().asSequence().flatMap { byteBuffer ->
                    byteBuffer.getAllBytes().asSequence()
                }.toList()
            }

        result.take(content.size) shouldBeEqualTo content.toList()
    }

    @Test
    fun `DataBuffer Flow를 OutputStream에 쓸 수 있다`() = runTest {
        val content = "kotlin spring".toByteArray()
        val dataBuffer: DataBuffer = bufferFactory.wrap(content)
        val outputStream = ByteArrayOutputStream()

        flowOf(dataBuffer)
            .asPublisher()
            .write(outputStream)
            .collect()

        outputStream.toByteArray().toList() shouldBeEqualTo content.toList()
    }

    @Test
    fun `DefaultDataBuffer를 release 하면 false를 반환한다`() {
        val dataBuffer = bufferFactory.wrap("test".toByteArray())
        val released = dataBuffer.release()
        released.shouldBeFalse()
    }

    @Test
    fun `Netty의 PooledDataBuffer를 release 하면 false를 반환한다`() {
        val dataBuffer = nettyBufferFactory.wrap("test".toByteArray())
        val released = dataBuffer.release()
        released.shouldBeTrue()
    }

    @Test
    fun `takeUntilByteCount로 지정한 바이트 수만큼만 읽는다`() = runTest {
        val content = "abcdefg".toByteArray()
        val dataBuffer = bufferFactory.wrap(content)
        val publisher = flowOf(dataBuffer).asPublisher()

        val result = publisher.takeUntilByteCount(3).toList()
        val bytes = result.flatMap {
            it.readableByteBuffers().asSequence()
                .flatMap { byteBuffer ->
                    byteBuffer.getAllBytes().asSequence()
                }
        }
        bytes shouldBeEqualTo content.take(3)
    }

    @Test
    fun `skipUntilByteCount로 지정한 바이트 수만큼 스킵한다`() = runTest {
        val content = "abcdefg".toByteArray()
        val dataBuffer = bufferFactory.wrap(content)
        val publisher = flowOf(dataBuffer).asPublisher()

        val result = publisher.skipUntilByteCount(3).toList()
        val bytes = result.flatMap {
            it.readableByteBuffers().asSequence().flatMap { byteBuffer ->
                byteBuffer.getAllBytes().asSequence()
            }
        }

        bytes shouldBeEqualTo content.drop(3)
    }

    @Test
    fun `join으로 DataBuffer를 하나로 합칠 수 있다`() = runTest {
        val content1 = "abc".toByteArray()
        val content2 = "def".toByteArray()
        val buffer1 = bufferFactory.wrap(content1)
        val buffer2 = bufferFactory.wrap(content2)
        val publisher = flowOf(buffer1, buffer2).asPublisher()

        val joined = publisher.join()
        val result = ByteArray(joined.readableByteCount())
        joined.read(result)
        result shouldBeEqualTo (content1 + content2)
    }
}
