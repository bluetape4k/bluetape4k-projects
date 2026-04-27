package io.bluetape4k.testcontainers.aws.ministack

import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer

abstract class AbstractMiniStackServiceTest : AbstractContainerTest() {
    protected val miniStack: MiniStackServer
        get() = MiniStackServer.Launcher.miniStack
}
