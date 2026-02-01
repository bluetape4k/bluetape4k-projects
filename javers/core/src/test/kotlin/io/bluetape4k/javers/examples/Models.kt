package io.bluetape4k.javers.examples

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSet
import io.bluetape4k.collections.eclipse.unifiedSetOf
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.TypeName
import java.io.Serializable
import java.time.ZonedDateTime

enum class Position {
    Assistant,
    Secretary,
    Developer,
    Specialist,
    Saleswoman,
    ScrumMaster,
    Townsman,
    Hero
}

data class Address(var city: String, var street: String = ""): Serializable

@TypeName("Person")
data class Person(
    @Id val login: String,
    var name: String,
): Serializable {
    val addresses: MutableList<Address> = mutableListOf()
    val addressMap: MutableMap<String, Address> = mutableMapOf()
    var position: Position? = null
}

@TypeName("Employee")
data class Employee(
    @Id val name: String,
    var salary: Int = 1000,
    val position: Position = Position.Developer,
): Serializable {

    var age: Int? = null

    var boss: Employee? = null
    val subordinates: MutableList<Employee> = fastListOf()

    var primaryAddress: Address? = null
    var postalAddress: Address? = null
    val skills: MutableSet<String> = unifiedSetOf()
    val performance: MutableMap<Int, String> = unifiedMapOf()
    var lastPromotionDate: ZonedDateTime? = null

    fun addSubordinates(vararg emps: Employee) {
        subordinates.addAll(emps)
        emps.forEach { it.boss = this }
    }
}

@TypeName("Boss")
data class Boss(@Id val name: String): Serializable {
    val suordinates: MutableCollection<Person> = fastListOf()
}
