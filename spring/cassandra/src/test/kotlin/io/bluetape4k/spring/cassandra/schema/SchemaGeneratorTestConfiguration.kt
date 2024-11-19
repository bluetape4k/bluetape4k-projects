package io.bluetape4k.spring.cassandra.schema

import io.bluetape4k.spring.cassandra.AbstractCassandraTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses = [AllPossibleTypes::class])
class SchemaGeneratorTestConfiguration: AbstractCassandraTestConfiguration()
