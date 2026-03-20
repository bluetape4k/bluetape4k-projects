package io.bluetape4k.examples.cassandra.reactive.multitenant

import java.io.Serializable

data class Tenant(val tenantId: String): Serializable
