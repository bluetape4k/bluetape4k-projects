package io.bluetape4k.testcontainers.aws.floci

import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.FlociServer

@Suppress("DEPRECATION")
abstract class AbstractFlociServiceTest : AbstractContainerTest() {
    protected val floci: FlociServer
        get() = FlociServer.Launcher.floci
}
