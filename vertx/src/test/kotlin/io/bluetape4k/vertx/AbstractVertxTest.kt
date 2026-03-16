package io.bluetape4k.vertx

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
abstract class AbstractVertxTest {

    companion object: KLoggingChannel()

}
