package org.javers.core.model

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import org.javers.core.metamodel.annotation.DiffIgnore
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.PropertyName
import java.io.Serializable
import java.time.LocalDateTime

abstract class AbstractDummyUser: Serializable {
    var inhertedInt: Int? = null
}

class DummyUser(@Id val name: String, var surname: String? = null): AbstractDummyUser() {

    enum class SEX { FEMALE, MALE, OCCASIONALLY }

    companion object {
        fun dummyUser(name: String = "name") = DummyUser(name)
    }

    @field:Transient
    var someTransientField: Int = 0

    @field:Transient
    var propertyWithTransientAnn: Int = 0

    @DiffIgnore
    var propertyWithDiffIgnoreAnn: Int = 0

    var propertyWithDiffIgnoredType: DummyIgnoredType? = null
    var propertyWithDiffIgnoredSubType: IgnoredSubType? = null

    // primitives and primitive boxes
    var flag: Boolean = false
    var boxedFlag: Boolean? = null
    var age: Int = 0
    var _char: Char = 0.toChar()

    var sex: SEX? = null
    var largeInt: Int? = null

    // collections
    var stringSet: MutableSet<String> = unifiedSetOf()
    var stringList: MutableList<String> = fastListOf()
    var integerList: MutableList<Int> = fastListOf()
    var primitiveMap: MutableMap<String, LocalDateTime> = unifiedMapOf()
    var valueMap: MutableMap<String, LocalDateTime> = unifiedMapOf()

    var objectMap: MutableMap<String, DummyUserDetails> = unifiedMapOf()   // not supported

    // array
    var intArray: IntArray? = null
    var dateTimes: Array<LocalDateTime>? = null

    // reference
    var supervisor: DummyUser? = null
    var dummyUserDetails: DummyUserDetails? = null
    var dummyUserDetailsList: MutableList<DummyUserDetails> = fastListOf()
    var employeesList: MutableList<DummyUser> = fastListOf()

    fun addEmployee(employee: DummyUser) {
        employeesList.add(employee)
        employee.supervisor = this
    }

    fun withDetails(id: Int = 1) = apply {
        dummyUserDetails = DummyUserDetails(id)
    }

    fun withAddress(city: String) = apply {
        if (dummyUserDetails == null) {
            withDetails()
        }
        dummyUserDetails!!.dummyAddress = DummyAddress(city = city)
    }

    fun withAddresses(vararg addresses: DummyAddress) = apply {
        if (dummyUserDetails == null) {
            withDetails()
        }
        dummyUserDetails!!.addressList.addAll(addresses)
    }

    fun withSex(sex: SEX) = apply { this.sex = sex }

    fun withPrimitiveMap(map: MutableMap<String, LocalDateTime>) = apply {
        this.primitiveMap = map
    }

    fun withValueMap(map: MutableMap<String, LocalDateTime>) = apply {
        this.valueMap = map
    }

    fun withStringsSet(strings: MutableSet<String>) = apply {
        this.stringSet = strings
    }

    fun withIntegerList(list: MutableList<Int>) = apply {
        this.integerList = list
    }

    fun withAge(age: Int) = apply { this.age = age }

    fun withBoxedFlag(boxedFlag: Boolean) = apply { this.boxedFlag = boxedFlag }

    fun withInteger(largeInt: Int) = apply { this.largeInt = largeInt }

    fun withFlag(flag: Boolean) = apply { this.flag = flag }

    fun withSupervisor(supervisorName: String) = apply {
        this.supervisor = DummyUser(name = supervisorName)
    }

    fun withSupervisor(supervisor: DummyUser) = apply {
        this.supervisor = supervisor
    }

    fun withEmployees(numberOfEmployees: Int) = apply {
        repeat(numberOfEmployees) {
            this.addEmployee(DummyUser(name = "EM${it + 1}"))
        }
    }

    fun withEmployees(employees: List<DummyUser>) = apply {
        employees.forEach { this.addEmployee(it) }
    }

    fun withDetailsList(numberOfDetailsList: Int) = apply {
        this.dummyUserDetailsList = fastList(numberOfDetailsList) { DummyUserDetails(id = it) }
    }

    fun withIntArray(ints: List<Int>) = apply {
        this.intArray = ints.toIntArray()
    }
}

data class DummyUserDetails(
    @Id val id: Int = DEFAULT_ID,
    var dummyAddress: DummyAddress? = null,
): Serializable {

    companion object {
        const val DEFAULT_ID = 1
    }

    var someValue: String? = null
    var isTrue: Boolean? = null
    val addressList: MutableList<DummyAddress> = mutableListOf()
    val interList: MutableList<Int> = mutableListOf()

    @PropertyName("Customized Property")
    var customizedProperty: String? = null

    fun withAddress(street: String, city: String) = apply {
        this.dummyAddress = DummyAddress(city, street)
    }

    fun withAddress() = apply {
        this.dummyAddress = DummyAddress("city", "street")
    }

    fun withAddresses(vararg dummyAddresses: DummyAddress) = apply {
        this.addressList.addAll(dummyAddresses)
    }
}
