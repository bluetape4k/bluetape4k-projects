package io.grpc.netty

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.grpc.Attributes
import io.grpc.MetricRecorder
import io.grpc.Status
import io.grpc.internal.ServerStream
import io.grpc.internal.ServerStreamListener
import io.grpc.internal.ServerTransportListener
import io.grpc.internal.TransportTracer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.Http2CodecUtil
import io.netty.handler.codec.http2.Http2Settings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 실제 gRPC handler의 SETTINGS ACK 이전 제한과 buffer 소유권을 검증한다.
 * package-private 접근은 테스트에만 한정하며 gRPC 버전 변경 시 factory 계약을 재확인한다.
 */
class NettyServerStreamLimitTest {
    private val transportListener = mockk<ServerTransportListener>(relaxed = true)
    private val streamListener = mockk<ServerStreamListener>(relaxed = true)
    private val metricRecorder = mockk<MetricRecorder>(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(transportListener, streamListener, metricRecorder)
        every { transportListener.transportReady(any()) } answers { firstArg() }
        every { transportListener.streamCreated(any(), any(), any()) } answers {
            firstArg<ServerStream>().setListener(streamListener)
        }
    }

    @Test
    fun `SETTINGS ACK 전에도 초과 stream을 거부하고 정상 stream을 유지한다`() {
        Fixture().use { fixture ->
            fixture.handler.connection().remote().maxActiveStreams() shouldBeEqualTo 1
            fixture.headers(3)
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 1
            fixture.headers(5)
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 1
            fixture.resetFrames() shouldBeEqualTo listOf(5 to 7L)
            verify(exactly = 1) { transportListener.streamCreated(any(), any(), any()) }
            fixture.server.isActive.shouldBeTrue()
            fixture.forceClose()
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 0
            fixture.server.isActive.shouldBeFalse()
            fixture.assertReleased()
        }
    }

    @Test
    fun `stream을 취소하면 ACK 전에도 다음 요청이 제한 슬롯을 재사용한다`() {
        Fixture().use { fixture ->
            fixture.headers(3)
            fixture.writer.writeRstStream(fixture.client.pipeline().firstContext(), 3, 8,
                fixture.client.newPromise())
            fixture.transfer()
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 0
            fixture.headers(5)
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 1
            verify(exactly = 2) { transportListener.streamCreated(any(), any(), any()) }
            fixture.forceClose()
            fixture.assertReleased()
        }
    }

    @Test
    fun `미완성 메시지를 받은 stream도 강제 종료하면 buffer를 해제한다`() {
        Fixture().use { fixture ->
            fixture.headers(3)
            // 길이 10인 메시지의 첫 byte만 보내 deframer가 미완성 데이터를 보유하게 한다.
            val partial = Unpooled.buffer().writeByte(0).writeInt(10).writeByte(1)
            fixture.writer.writeData(fixture.client.pipeline().firstContext(), 3, partial, 0, false,
                fixture.client.newPromise())
            val dataInputs = fixture.transfer()
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 1
            // 이번 DATA 전달분 중 서버가 실제 보유한 복사본을 종료 전후 동일 객체로 검사한다.
            val retainedData = dataInputs.single { it.refCnt() > 0 }
            fixture.forceClose()
            fixture.handler.connection().numActiveStreams() shouldBeEqualTo 0
            fixture.server.isActive.shouldBeFalse()
            retainedData.refCnt() shouldBeEqualTo 0
            fixture.assertReleased()
            verify(exactly = 1) { streamListener.closed(any()) }
        }
    }

    private inner class Fixture: AutoCloseable {
        val server = EmbeddedChannel()
        val client = EmbeddedChannel(ChannelInboundHandlerAdapter())
        val writer = DefaultHttp2FrameWriter()
        private val inbound = mutableListOf<ByteBuf>()
        val handler: NettyServerHandler = NettyServerHandler.newHandler(
            transportListener, server.newPromise(), emptyList(), TransportTracer(),
            1, false, 65535, 8192, 8192, 1024,
            Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
            false, Long.MAX_VALUE, 0, Long.MAX_VALUE, Attributes.EMPTY, metricRecorder
        )

        init {
            server.pipeline().addLast(handler)
            handler.handleProtocolNegotiationCompleted(Attributes.EMPTY, null)
            // Netty 기본 preface는 unreleasable 상수이므로 소유권 검증용 복사본을 전달한다.
            receive(Unpooled.copiedBuffer(Http2CodecUtil.connectionPrefaceBuf()))
            writer.writeSettings(client.pipeline().firstContext(), Http2Settings(), client.newPromise())
            transfer()
            // 서버 SETTINGS는 읽거나 ACK하지 않는다. 일반 클라이언트의 자동 ACK를 사용하지 않는다.
        }

        fun headers(streamId: Int) {
            val headers = DefaultHttp2Headers().method("POST").scheme("http")
                .path("/test.Service/stream").authority("localhost")
                .add("content-type", "application/grpc").add("te", "trailers")
            writer.writeHeaders(client.pipeline().firstContext(), streamId, headers, 0, false,
                client.newPromise())
            transfer()
        }

        fun transfer(): List<ByteBuf> {
            val transferred = mutableListOf<ByteBuf>()
            client.flushOutbound()
            while (true) {
                val encoded = client.readOutbound<ByteBuf>() ?: break
                // Pooled buffer는 해제 뒤 같은 객체가 재사용된다. refCnt 관찰에는 독립 복사본을 쓴다.
                val owned = Unpooled.copiedBuffer(encoded)
                encoded.release()
                transferred.add(owned)
                receive(owned)
            }
            server.runPendingTasks()
            server.checkException()
            return transferred
        }

        private fun receive(buffer: ByteBuf) {
            inbound.add(buffer)
            server.writeInbound(buffer)
        }

        fun forceClose() {
            server.writeAndFlush(ForcefulCloseCommand(Status.CANCELLED))
            server.runPendingTasks()
            server.checkException()
        }

        fun resetFrames(): List<Pair<Int, Long>> {
            val bytes = Unpooled.buffer()
            try {
                while (true) {
                    val frame = server.readOutbound<ByteBuf>() ?: break
                    try {
                        bytes.writeBytes(frame)
                    } finally {
                        frame.release()
                    }
                }
                val resets = mutableListOf<Pair<Int, Long>>()
                while (bytes.isReadable) {
                    val length = bytes.readUnsignedMedium()
                    val type = bytes.readUnsignedByte().toInt()
                    bytes.skipBytes(1)
                    val streamId = bytes.readInt() and Int.MAX_VALUE
                    if (type == 3) {
                        length shouldBeEqualTo 4
                        resets.add(streamId to bytes.readUnsignedInt())
                    } else {
                        bytes.skipBytes(length)
                    }
                }
                return resets
            } finally {
                bytes.release()
            }
        }

        fun assertReleased() {
            inbound.filter { it.refCnt() != 0 }.map { it.javaClass.simpleName } shouldBeEqualTo emptyList()
        }

        override fun close() {
            try {
                server.finishAndReleaseAll()
            } finally {
                writer.close()
                client.finishAndReleaseAll()
            }
        }
    }
}
