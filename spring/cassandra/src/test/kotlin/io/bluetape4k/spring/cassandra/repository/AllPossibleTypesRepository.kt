package io.bluetape4k.spring.cassandra.repository

import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.springframework.data.repository.CrudRepository

interface AllPossibleTypesRepository: CrudRepository<AllPossibleTypes, String>
