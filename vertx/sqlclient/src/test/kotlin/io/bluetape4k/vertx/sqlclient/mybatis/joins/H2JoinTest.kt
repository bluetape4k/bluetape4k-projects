package io.bluetape4k.vertx.sqlclient.mybatis.joins

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.Vertx

class H2JoinTest: AbstractJoinTest() {

    companion object: KLoggingChannel()

    override fun Vertx.getPool() = this.getH2Pool()
}
