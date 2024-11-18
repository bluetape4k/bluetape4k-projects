package io.bluetape4k.vertx.sqlclient.mybatis.joins

import io.bluetape4k.logging.KLogging
import io.vertx.core.Vertx

class MysqlJoinTest: AbstractJoinTest() {

    companion object: KLogging()

    override fun Vertx.getPool() = this.getMySQLPool()
}
