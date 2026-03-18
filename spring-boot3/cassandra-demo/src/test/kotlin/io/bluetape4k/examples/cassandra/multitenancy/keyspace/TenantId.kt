package io.bluetape4k.examples.cassandra.multitenancy.keyspace

import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component

@Component
@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
class TenantId(private var id: String = "defaults") {

    fun get(): String = id

    fun set(id: String) {
        this.id = id
    }
}
