package io.bluetape4k.spring4.cassandra.repository

import io.bluetape4k.spring4.cassandra.domain.model.AllPossibleTypes
import org.springframework.data.repository.CrudRepository

interface AllPossibleTypesRepository: CrudRepository<AllPossibleTypes, String>
