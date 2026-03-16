package io.bluetape4k.vertx.sqlclient.mybatis.subquery

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.vertx.core.Vertx

class MysqlSubqueryTest: AbstractSubqueryTest() {

    companion object: KLoggingChannel()

    override fun Vertx.getPool() = getMySQLPool()
}
