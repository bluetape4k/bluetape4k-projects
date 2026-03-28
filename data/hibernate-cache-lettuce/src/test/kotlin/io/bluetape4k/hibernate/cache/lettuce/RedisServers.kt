package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer

object RedisServers: KLogging() {

    val redis: RedisServer by lazy { RedisServer.Launcher.redis }

}
