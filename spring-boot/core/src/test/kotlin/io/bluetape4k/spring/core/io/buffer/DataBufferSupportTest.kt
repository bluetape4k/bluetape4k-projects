package io.bluetape4k.spring.core.io.buffer

import io.bluetape4k.io.getAllBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.AbstractSpringTest
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.NettyDataBufferFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * DataBufferSupport 확장 함수 테스트
 */
class DataBufferSupportTest: AbstractSpringTest() {

    companion object: KLogging()

    private val bufferFactory = DefaultDataBufferFactory(true)
    private val nettyBufferFactory = NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT)

    @RepeatedTest(REPEAT_SIZE)
    fun `InputStream을 DataBuffer Flow로 읽을 수 있다`() =
        runTest {
            val content = faker.lorem().paragraph(8).toByteArray()
            val inputStream = ByteArrayInputStream(content)

            val result =
                inputStream
                    .readAsDataBuffers(bufferFactory)
                    .flatMapConcat {
                        it
                            .readableByteBuffers()
                            .asFlow()
                            .flatMapConcat { byteBuffer ->
                                byteBuffer.getAllBytes().toTypedArray().asFlow()
                            }
                    }

            result.take(content.size).toList().toByteArray() shouldBeEqualTo content
        }

    @RepeatedTest(REPEAT_SIZE)
    fun `DataBuffer Flow를 OutputStream에 쓸 수 있다`() =
        runTest {
            val content = faker.lorem().paragraph(8).toByteArray()
            val dataBuffer: DataBuffer = bufferFactory.wrap(content)
            val outputStream = ByteArrayOutputStream()

            flowOf(dataBuffer)
                .asPublisher()
                .write(outputStream)
                .collect()

            outputStream.toByteArray() shouldBeEqualTo content
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
    fun `takeUntilByteCount로 지정한 바이트 수만큼만 읽는다`() =
        runTest {
            val content = "abcdefg".toByteArray()
            val dataBuffer = bufferFactory.wrap(content)
            val publisher = flowOf(dataBuffer).asPublisher()

            val result = publisher.takeUntilByteCount(3)
            val bytes =
                result
                    .flatMapConcat {
                        it
                            .readableByteBuffers()
                            .asFlow()
                            .flatMapConcat { byteBuffer ->
                                byteBuffer.getAllBytes().toTypedArray().asFlow()
                            }
                    }

            bytes.toList() shouldBeEqualTo content.take(3)
        }

    @Test
    fun `skipUntilByteCount로 지정한 바이트 수만큼 스킵한다`() =
        runTest {
            val content = "abcdefg".toByteArray()
            val dataBuffer = bufferFactory.wrap(content)
            val publisher = flowOf(dataBuffer).asPublisher()

            val result = publisher.skipUntilByteCount(3).toList()
            val bytes =
                result
                    .flatMap {
                        it.readableByteBuffers().asSequence().flatMap { byteBuffer ->
                            byteBuffer.getAllBytes().asSequence()
                        }
                    }

            bytes shouldBeEqualTo content.drop(3)
        }

    @Test
    fun `join으로 DataBuffer를 하나로 합칠 수 있다`() =
        runTest {
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

    @Test
    fun `ReadableByteChannel을 DataBuffer Flow로 읽을 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val channel = Channels.newChannel(ByteArrayInputStream(content))

        val result = channel.readAsDataBuffer(bufferFactory).toList()

        result.shouldNotBeEmpty()
    }

    @Test
    fun `AsynchronousFileChannel을 DataBuffer Flow로 읽을 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val tempFile = Files.createTempFile("test-async-read", ".dat")
        Files.write(tempFile, content)

        try {
            val asyncChannel = AsynchronousFileChannel.open(tempFile, StandardOpenOption.READ)
            val result = asyncChannel.readAsDataBuffer(bufferFactory).toList()
            asyncChannel.close()
            result.shouldNotBeEmpty()
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `Path를 DataBuffer Flow로 읽을 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val tempFile = Files.createTempFile("test-path-read", ".dat")
        Files.write(tempFile, content)

        try {
            val result = tempFile.readAsDataBuffer(bufferFactory).toList()
            result.shouldNotBeEmpty()
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `Resource를 DataBuffer Flow로 읽을 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val resource = ByteArrayResource(content)

        val result = resource.readAsDataBuffer(bufferFactory).toList()

        result.shouldNotBeEmpty()
    }

    @Test
    fun `Publisher를 WritableByteChannel에 쓸 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val dataBuffer = bufferFactory.wrap(content)
        val outputStream = ByteArrayOutputStream()
        val channel = Channels.newChannel(outputStream)

        flowOf(dataBuffer).asPublisher().write(channel).collect()

        outputStream.toByteArray() shouldBeEqualTo content
    }

    @Test
    fun `Publisher를 AsynchronousFileChannel에 쓸 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val dataBuffer = bufferFactory.wrap(content)
        val tempFile = Files.createTempFile("test-async-write", ".dat")

        try {
            val asyncChannel = AsynchronousFileChannel.open(
                tempFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
            )
            flowOf(dataBuffer).asPublisher().write(asyncChannel, position = 0).collect()
            asyncChannel.close()

            Files.readAllBytes(tempFile) shouldBeEqualTo content
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `Publisher를 Path에 쓸 수 있다`() = runTest {
        val content = faker.lorem().sentence().toByteArray()
        val dataBuffer = bufferFactory.wrap(content)
        val tempFile = Files.createTempFile("test-path-write", ".dat")

        try {
            flowOf(dataBuffer).asPublisher().write(tempFile)
            Files.readAllBytes(tempFile) shouldBeEqualTo content
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `DataBuffer retain은 같은 버퍼를 반환한다`() {
        val buffer = bufferFactory.wrap("test".toByteArray())
        val retained = buffer.retain()
        assertSame(buffer, retained)
    }

    @Test
    fun `DataBuffer touch는 같은 버퍼를 반환한다`() {
        val buffer = bufferFactory.wrap("test".toByteArray())
        val touched = buffer.touch("hint-value")
        assertSame(buffer, touched)
    }
}
