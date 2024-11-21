package io.bluetape4k.examples.cassandra.reactive.multitenant

import org.springframework.data.spel.spi.EvaluationContextExtension

class TenantExtension(val tenant: Tenant): EvaluationContextExtension {
    override fun getExtensionId(): String = "my-tenant-extension"
}
