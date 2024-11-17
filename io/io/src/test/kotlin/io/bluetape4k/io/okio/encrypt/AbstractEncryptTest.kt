package io.bluetape4k.io.okio.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import java.util.*

abstract class AbstractEncryptTest: AbstractOkioTest() {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5

        @JvmStatic
        val faker = Faker(Locale.getDefault())
    }

    fun getEncryptors(): List<Encryptor> = listOf(
        Encryptors.AES,
        Encryptors.DES,
        Encryptors.TripleDES,
        Encryptors.RC2,
        Encryptors.RC4
    )
}
