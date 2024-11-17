package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import org.amshove.kluent.shouldBeNear
import java.util.*

abstract class AbstractOkioTest {

    companion object: KLogging() {
        val faker = Faker(Locale.getDefault())
    }

    protected fun now(): Double = System.nanoTime() / 1_000_000.0

    protected fun assertElapsed(duration: Double, start: Double) {
        (now() - start - 200.0).shouldBeNear(duration, 250.0)
    }

}
