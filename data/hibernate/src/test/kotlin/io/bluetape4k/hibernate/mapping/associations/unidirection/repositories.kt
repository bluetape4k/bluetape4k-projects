package io.bluetape4k.hibernate.mapping.associations.unidirection

import org.springframework.data.jpa.repository.JpaRepository

interface CloudRepository: JpaRepository<Cloud, Int>

interface SnowflakeRepository: JpaRepository<Snowflake, Int>
