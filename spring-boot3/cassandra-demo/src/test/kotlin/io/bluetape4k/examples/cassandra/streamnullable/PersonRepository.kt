package io.bluetape4k.examples.cassandra.streamnullable

import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.Repository
import java.util.stream.Stream

interface PersonRepository: Repository<Person, String> {

    fun findById(id: String): Person?

    fun findAll(): Stream<Person>

    @Query("select * from stream_person where id = ?0")
    fun findPersonById(id: String): Person?

    fun findByPerson(person: Person): Person? = findPersonById(person.id)

    fun deleteAll()

    fun save(person: Person): Person

}
