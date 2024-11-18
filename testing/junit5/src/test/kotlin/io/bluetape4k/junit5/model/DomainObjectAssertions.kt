package io.bluetape4k.junit5.model

import io.bluetape4k.junit5.random.RandomValue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull


fun DomainObject?.shouldFullyPopulated() {
    this.shouldNotBeNull()
    assertThatDomainObjectIsFullyPopulated(this)
}

fun DomainObject?.shouldPartiallyPopulated() {
    this.shouldNotBeNull()
    assertThatDomainObjectIsPartiallyPopulated(this)
}

fun assertThatDomainObjectIsFullyPopulated(domainObject: DomainObject?) {

    domainObject.shouldNotBeNull()

    domainObject.id.shouldNotBeNull() shouldNotBeEqualTo 0
    domainObject.name.shouldNotBeNull() shouldNotBeEqualTo ""

    domainObject.nestedDomainObject?.shouldNotBeNull()
    domainObject.nestedDomainObject?.address.shouldNotBeNull()
    domainObject.nestedDomainObject?.category.shouldNotBeNull()

    domainObject.wotsits.shouldNotBeNull().shouldNotBeEmpty()

    domainObject.value.shouldNotBeNull() shouldNotBeEqualTo 0L
    domainObject.price.shouldNotBeNull() shouldNotBeEqualTo 0.0

    domainObject.objectLists.shouldNotBeNull()
    domainObject.objectLists.shouldNotBeEmpty()
}

fun assertThatDomainObjectIsPartiallyPopulated(domainObject: DomainObject?) {
    domainObject.shouldNotBeNull()

    domainObject.id.shouldNotBeNull() shouldBeEqualTo 0
    domainObject.name.shouldNotBeNull() shouldNotBeEqualTo ""

    domainObject.nestedDomainObject?.shouldNotBeNull()
    domainObject.nestedDomainObject?.address.shouldBeNull()
    domainObject.nestedDomainObject?.category.shouldNotBeNull()

    domainObject.wotsits.shouldBeNull()

    domainObject.value.shouldNotBeNull() shouldNotBeEqualTo 0L
    domainObject.price.shouldNotBeNull() shouldNotBeEqualTo 0.0
}

fun getDefaultSizeOfRandom(): Int {
    val clazz = RandomValue::class.java
    val method = clazz.getDeclaredMethod("size")
    return method.defaultValue as Int
}
