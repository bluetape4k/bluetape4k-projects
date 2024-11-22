package io.bluetape4k.workshop.mongo.domain

import org.springframework.data.repository.CrudRepository

interface PersonRepository: CrudRepository<Person, String> {

    fun findOneOrNoneByFirstname(firstname: String): Person?

    fun findNullableByFirstname(firstname: String): Person?

    fun findOneByFirstname(firstname: String): Person

}
