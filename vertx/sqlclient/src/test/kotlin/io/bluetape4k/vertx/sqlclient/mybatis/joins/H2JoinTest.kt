package io.bluetape4k.vertx.sqlclient.mybatis.joins

import io.bluetape4k.logging.KLogging
import io.vertx.core.Vertx

class H2JoinTest: AbstractJoinTest() {

    companion object: KLogging()

    override fun Vertx.getPool() = this.getH2Pool()
}
