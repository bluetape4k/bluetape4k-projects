package io.bluetape4k.examples.cassandra.multitenancy.row

object TenantIdProvider {

    val tenantId: ThreadLocal<String> = ThreadLocal.withInitial { "" }

}
