package io.bluetape4k.crypto.cipher

import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty
import javax.crypto.Cipher

fun Cipher.encrypt(plain: ByteArray?, offset: Int = 0, length: Int = plain?.size ?: 0): ByteArray {
    if (plain.isNullOrEmpty()) {
        return emptyByteArray
    }
    return doFinal(plain, offset, length)
}

fun Cipher.decrypt(encrypted: ByteArray?, offset: Int = 0, length: Int = encrypted?.size ?: 0): ByteArray {
    if (encrypted.isNullOrEmpty()) {
        return emptyByteArray
    }
    return update(encrypted, offset, length) +
            runCatching { doFinal() }.getOrDefault(emptyByteArray)
}
