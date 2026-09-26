package io.bluetape4k

import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import java.util.*

abstract class AbstractCoreTest {

    companion object: KLogging() {

        @JvmStatic
        val faker = Faker(Locale.getDefault())
    }
}
