package io.bluetape4k.examples.cassandra.optimisticlocking

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OptimisticPersonRepository: CoroutineCrudRepository<OptimisticPerson, Long> 
