package io.bluetape4k.tink

import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import java.util.*

abstract class AbstractTinkTest {

    companion object: KLogging() {

        const val REPEAT_SIZE = 5

        @JvmStatic
        val faker = Faker(Locale.getDefault())
    }
}
