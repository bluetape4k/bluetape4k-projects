package io.bluetape4k.examples.cassandra.auditing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AuditedPersonRepository: CoroutineCrudRepository<AuditedPerson, Long>
