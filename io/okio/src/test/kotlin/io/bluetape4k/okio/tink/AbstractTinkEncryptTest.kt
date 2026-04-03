package io.bluetape4k.okio.tink

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.okio.AbstractOkioTest
import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors

abstract class AbstractTinkEncryptTest: AbstractOkioTest() {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5
    }

    protected fun encryptors(): List<TinkEncryptor> = listOf(
        TinkEncryptors.AES256_GCM,
        TinkEncryptors.AES128_GCM,
        TinkEncryptors.CHACHA20_POLY1305,
        TinkEncryptors.XCHACHA20_POLY1305,
        TinkEncryptors.DETERMINISTIC_AES256_SIV,
    )

    protected fun compressors() = listOf(
        Compressors.BZip2,
        Compressors.Deflate,
        Compressors.GZip,
        Compressors.LZ4,
        Compressors.Snappy,
        Compressors.Zstd,
    )
}
