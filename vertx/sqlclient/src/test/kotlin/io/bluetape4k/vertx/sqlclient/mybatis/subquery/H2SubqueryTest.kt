package io.bluetape4k.vertx.sqlclient.mybatis.subquery

import io.bluetape4k.logging.KLogging
import io.vertx.core.Vertx

class H2SubqueryTest: AbstractSubqueryTest() {

    companion object: KLogging()

    override fun Vertx.getPool() = this.getH2Pool()
}
