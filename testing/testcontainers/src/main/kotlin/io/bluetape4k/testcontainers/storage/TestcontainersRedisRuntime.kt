package io.bluetape4k.testcontainers.storage

/** [RedisServer]를 명시적 시작·종료 소유권으로 감싸는 Redis Testcontainers runtime입니다. */
class TestcontainersRedisRuntime(
    image: String = RedisServer.IMAGE,
    tag: String = RedisServer.TAG,
    useDefaultPort: Boolean = false,
    reuse: Boolean = false,
): RedisRuntime {

    private val delegate = OwnedRedisRuntime {
        TestcontainersRedisHandle(
            RedisServer(
                image = image,
                tag = tag,
                useDefaultPort = useDefaultPort,
                reuse = reuse,
            ),
        )
    }

    override val isRunning: Boolean get() = delegate.isRunning
    override val isClosed: Boolean get() = delegate.isClosed
    override fun start(): RedisEndpoint = delegate.start()
    override fun close() = delegate.close()

    private class TestcontainersRedisHandle(
        private val server: RedisServer,
    ): RedisContainerHandle {

        override fun start() = server.start()
        override fun stop() = server.stop()
        override fun endpoint(): RedisEndpoint = RedisEndpoint(server.host, server.port, server.url)
    }
}
