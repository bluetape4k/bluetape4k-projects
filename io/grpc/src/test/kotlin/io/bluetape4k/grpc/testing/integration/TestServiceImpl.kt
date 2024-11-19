package io.bluetape4k.grpc.testing.integration

import com.google.protobuf.ByteString
import io.bluetape4k.grpc.interceptor.echoRequestHeadersInterceptor
import io.bluetape4k.grpc.interceptor.echoRequestMetadataInHeaders
import io.bluetape4k.grpc.interceptor.echoRequestMetadataInTrailers
import io.bluetape4k.grpc.testing.integration.TestServiceTest.Companion.randomString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.grpc.Status
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class TestServiceImpl @JvmOverloads constructor(
    executor: Executor = Executors.newSingleThreadScheduledExecutor(),
): TestServiceGrpcKt.TestServiceCoroutineImplBase(executor.asCoroutineDispatcher()) {

    companion object: KLogging() {
        private val random = Random(System.currentTimeMillis())
        private val EMPTY = Emptys.Empty.getDefaultInstance()

        /** Returns interceptors necessary for full service implementation.  */
        @get:JvmStatic
        @get:JvmName("interceptors")
        val interceptors = listOf(
            echoRequestHeadersInterceptor(Util.METADATA_KEY),
            echoRequestMetadataInHeaders(Util.ECHO_INITIAL_METADATA_KEY),
            echoRequestMetadataInTrailers(Util.ECHO_TRAILING_METADATA_KEY)
        )

        suspend fun Flow<Int>.sum() = fold(0) { acc, value -> acc + value }

        private fun generatePayload(dataBuffer: ByteString, offset: Int, size: Int): Messages.Payload {
            val payloadChunks = mutableListOf<ByteString>()

            var begin = offset
            var end: Int
            var bytesLeft = size

            while (bytesLeft > 0) {
                end = minOf(begin + bytesLeft, dataBuffer.size())
                payloadChunks += dataBuffer.substring(begin, end)
                bytesLeft -= (end - begin)
                begin = end % dataBuffer.size()
            }

            return Messages.Payload.newBuilder()
                .apply {
                    body = ByteString.copyFrom(payloadChunks)
                }
                .build()
        }
    }

    private val compressableBuffer: ByteString = ByteString.copyFrom(randomString(1024).toUtf8Bytes())

    override suspend fun emptyCall(request: Emptys.Empty): Emptys.Empty = Emptys.Empty.getDefaultInstance()

    override suspend fun unaryCall(request: Messages.SimpleRequest): Messages.SimpleResponse {
        if (request.hasResponseStatus()) {
            throw Status
                .fromCodeValue(request.responseStatus.code)
                .withDescription(request.responseStatus.message)
                .asException()
        }

        return Messages.SimpleResponse
            .newBuilder()
            .apply {
                if (request.responseSize != 0) {
                    val offset = random.nextInt(compressableBuffer.size())
                    payload = generatePayload(compressableBuffer, offset, request.responseSize)
                }
            }
            .build()
    }

    override fun streamingOutputCall(request: Messages.StreamingOutputCallRequest): Flow<Messages.StreamingOutputCallResponse> {
        return flow {
            var offset = 0
            request.responseParametersList.forEach { params ->
                delay(TimeUnit.MICROSECONDS.toMillis(params.intervalUs.toLong()))

                val response = Messages.StreamingOutputCallResponse
                    .newBuilder()
                    .apply {
                        payload = generatePayload(compressableBuffer, offset, params.size)
                    }
                    .build()

                emit(response)

                offset += params.size
                offset %= compressableBuffer.size()
            }
        }
    }

    override suspend fun streamingInputCall(requests: Flow<Messages.StreamingInputCallRequest>): Messages.StreamingInputCallResponse {
        return Messages.StreamingInputCallResponse
            .newBuilder()
            .apply {
                aggregatedPayloadSize = requests.map { it.payload.body.size() }.sum()
            }
            .build()
    }

    /**
     * Full Duplex 방식으로 서로 간에 순서없이 메시지를 보낸다
     */
    override fun fullDuplexCall(requests: Flow<Messages.StreamingOutputCallRequest>): Flow<Messages.StreamingOutputCallResponse> {
        return requests.flatMapConcat { request ->
            if (request.hasResponseStatus()) {
                throw Status
                    .fromCodeValue(request.responseStatus.code)
                    .withDescription(request.responseStatus.message)
                    .asException()
            }
            streamingOutputCall(request)
        }
    }

    /**
     * Half Duplex 로 요청이 다 들어올 때까지 기다렸다가 응답 스트림을 보냅니다.
     */
    override fun halfDuplexCall(requests: Flow<Messages.StreamingOutputCallRequest>): Flow<Messages.StreamingOutputCallResponse> {
        return flow {
            val requestList = requests.toList()
            emitAll(requestList.asFlow().flatMapConcat { streamingOutputCall(it) })
        }
    }
}
