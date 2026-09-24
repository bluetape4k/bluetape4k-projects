package io.bluetape4k.examples.cassandra.reactive.auditing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CustomAuditingRepository: CoroutineCrudRepository<CustomAuditableOrder, String> 
