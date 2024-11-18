package io.bluetape4k.hibernate.mapping.simple

import org.springframework.data.jpa.repository.JpaRepository

interface SimpleEntityRepository: JpaRepository<SimpleEntity, Long>
