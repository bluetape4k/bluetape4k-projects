package io.bluetape4k.spring.core.io.buffer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.io.getAllBytes
import io.bluetape4k.io.toInputStream
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.AbstractSpringTest
import io.bluetape4k.support.toUtf8Bytes
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asPublisher
import org.junit.jupiter.api.RepeatedTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.core.io.buffer.PooledDataBuffer
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
    fun `InputStream을 DataBuffer Flow로 읽을 수 있다`() = runSuspendIO {
        val content = faker.lorem().paragraph(8).toUtf8Bytes()
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
    fun `DataBuffer Flow를 OutputStream에 쓸 수 있다`() = runSuspendIO {
        val content = faker.lorem().paragraph(8).toUtf8Bytes()
        val dataBuffer: DataBuffer = bufferFactory.wrap(content)
        val outputStream = ByteArrayOutputStream()

        flowOf(dataBuffer)
            .asPublisher()
            .write(outputStream)
            .collect()

        outputStream.toByteArray() shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DefaultDataBuffer를 release 하면 false를 반환한다`() {
        val dataBuffer = bufferFactory.wrap(faker.lorem().sentence().toUtf8Bytes())
        val released = dataBuffer.release()
        released.shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Netty의 PooledDataBuffer를 release 하면 소유권을 해제하고 true를 반환한다`() {
        val dataBuffer = nettyBufferFactory.wrap(faker.lorem().sentence().toUtf8Bytes()) as PooledDataBuffer
        try {
            dataBuffer.isAllocated.shouldBeTrue()

            val released = dataBuffer.release()

            released.shouldBeTrue()
            dataBuffer.isAllocated.shouldBeFalse()
        } finally {
            if (dataBuffer.isAllocated) {
                dataBuffer.release()
            }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `takeUntilByteCount로 지정한 바이트 수만큼만 읽는다`() = runSuspendIO {
        val content = faker.lorem().sentence(3).toUtf8Bytes()
        val dataBuffer = bufferFactory.wrap(content)
        val publisher = flowOf(dataBuffer).asPublisher()

        val result = publisher.takeUntilByteCount(3)
        val bytes = result
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

    @RepeatedTest(REPEAT_SIZE)
    fun `skipUntilByteCount로 지정한 바이트 수만큼 스킵한다`() = runSuspendIO {
        val content = faker.lorem().sentence(3).toUtf8Bytes()
        val dataBuffer = bufferFactory.wrap(content)
        val publisher = flowOf(dataBuffer).asPublisher()

        val result = publisher.skipUntilByteCount(3).toList()
        val bytes = result
            .flatMap {
                it.readableByteBuffers().asSequence().flatMap { byteBuffer ->
                    byteBuffer.getAllBytes().asSequence()
                }
            }

        bytes shouldBeEqualTo content.drop(3)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `join으로 DataBuffer를 하나로 합칠 수 있다`() = runSuspendIO {
        val content1 = faker.lorem().word().toUtf8Bytes()
        val content2 = faker.lorem().word().toUtf8Bytes()
        val buffer1 = bufferFactory.wrap(content1)
        val buffer2 = bufferFactory.wrap(content2)
        val publisher = flowOf(buffer1, buffer2).asPublisher()

        val joined = publisher.join()
        val result = ByteArray(joined.readableByteCount())
        joined.read(result)
        result shouldBeEqualTo (content1 + content2)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `ReadableByteChannel을 DataBuffer Flow로 읽을 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toByteArray()
        val channel = Channels.newChannel(content.toInputStream())

        val result = channel.readAsDataBuffer(bufferFactory).toList()

        result.shouldNotBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AsynchronousFileChannel을 DataBuffer Flow로 읽을 수 있다`() = runSuspendIO {
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

    @RepeatedTest(REPEAT_SIZE)
    fun `Path를 DataBuffer Flow로 읽을 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toUtf8Bytes()
        val tempFile = Files.createTempFile("test-path-read", ".dat")
        Files.write(tempFile, content)

        try {
            val result = tempFile.readAsDataBuffer(bufferFactory).toList()
            result.shouldNotBeEmpty()
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Resource를 DataBuffer Flow로 읽을 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toUtf8Bytes()
        val resource = ByteArrayResource(content)

        val result = resource.readAsDataBuffer(bufferFactory).toList()

        result.shouldNotBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Publisher를 WritableByteChannel에 쓸 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toUtf8Bytes()
        val dataBuffer = bufferFactory.wrap(content)
        val outputStream = ByteArrayOutputStream()
        val channel = Channels.newChannel(outputStream)

        flowOf(dataBuffer).asPublisher().write(channel).collect()

        outputStream.toByteArray() shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Publisher를 AsynchronousFileChannel에 쓸 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toUtf8Bytes()
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

    @RepeatedTest(REPEAT_SIZE)
    fun `Publisher를 Path에 쓸 수 있다`() = runSuspendIO {
        val content = faker.lorem().sentence().toUtf8Bytes()
        val dataBuffer = bufferFactory.wrap(content)
        val tempFile = Files.createTempFile("test-path-write", ".dat")

        try {
            flowOf(dataBuffer).asPublisher().write(tempFile)
            Files.readAllBytes(tempFile) shouldBeEqualTo content
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DataBuffer retain은 같은 버퍼를 반환한다`() {
        val buffer = bufferFactory.wrap(faker.lorem().sentence().toUtf8Bytes())
        val retained = buffer.retain()
        retained shouldBeSameInstanceAs buffer
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DataBuffer touch는 같은 버퍼를 반환한다`() {
        val buffer = bufferFactory.wrap(faker.lorem().sentence().toUtf8Bytes())
        val touched = buffer.touch("hint-value")
        touched shouldBeSameInstanceAs buffer
    }
}
