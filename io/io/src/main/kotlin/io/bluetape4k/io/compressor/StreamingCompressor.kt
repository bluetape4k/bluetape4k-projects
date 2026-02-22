package io.bluetape4k.io.compressor

import io.bluetape4k.io.DEFAULT_BUFFER_SIZE
import io.bluetape4k.io.copyTo
import io.bluetape4k.io.toInputStream
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 스트리밍 방식으로 데이터를 압축/복원하는 압축기 계약입니다.
 *
 * 기존 [Compressor]는 바이트 배열 기반(one-shot) 계약을 유지하고,
 * 이 인터페이스는 대용량 데이터 처리에 적합한 스트리밍 계약을 제공합니다.
 */
interface StreamingCompressor {

    /**
     * [output]으로 압축 데이터를 기록하는 스트림을 생성합니다.
     *
     * 반환된 스트림을 닫으면 압축 푸터/패딩 등 마무리 작업이 수행됩니다.
     */
    fun compressing(output: OutputStream): OutputStream

    /**
     * [input]으로부터 복원 데이터를 읽는 스트림을 생성합니다.
     */
    fun decompressing(input: InputStream): InputStream

    /**
     * [source]를 압축하여 [sink]에 기록합니다.
     *
     * [source]와 내부 래핑 스트림은 작업 종료 시 닫힙니다.
     */
    fun compress(source: InputStream, sink: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }

        source.use { input ->
            compressing(sink).use { compressed ->
                return input.copyTo(compressed, bufferSize)
            }
        }
    }

    /**
     * [source]를 복원하여 [sink]에 기록합니다.
     *
     * [source]와 내부 래핑 스트림은 작업 종료 시 닫힙니다.
     */
    fun decompress(source: InputStream, sink: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }

        source.use { input ->
            decompressing(input).use { decompressed ->
                return decompressed.copyTo(sink, bufferSize)
            }
        }
    }

    /**
     * 바이트 배열을 압축합니다.
     */
    fun compress(plain: ByteArray?): ByteArray {
        if (plain.isNullOrEmpty()) {
            return emptyByteArray
        }

        val output = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        compress(plain!!.toInputStream(), output)
        return output.toByteArray()
    }

    /**
     * 바이트 배열을 복원합니다.
     */
    fun decompress(compressed: ByteArray?): ByteArray {
        if (compressed.isNullOrEmpty()) {
            return emptyByteArray
        }

        val output = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        decompress(compressed!!.toInputStream(), output)
        return output.toByteArray()
    }
}
