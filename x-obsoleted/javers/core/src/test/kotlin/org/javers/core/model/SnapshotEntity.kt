package org.javers.core.model

import com.google.common.collect.Multimap
import com.google.common.collect.Multiset
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.ShallowReference
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ShallowReference
data class ShallowPhone(
    @Id var id: Long,
    var number: String? = null,
    var category: CategoryC? = null,
): Serializable

data class SnapshotEntity(
    @Id var id: Int,
    var entityRef: SnapshotEntity? = null,
    var valueObjectRef: DummyAddress? = null,
): Serializable {

    enum class DummyEnum {
        VAL1,
        VAL2,
        VAL3
    }

    var dob: LocalDate? = null

    var intProperty: Int? = null

    var arrayOfIntegers: IntArray? = null
    var arrayOfDates: Array<LocalDate>? = null
    var arrayOfEntities: Array<SnapshotEntity>? = null
    var arrayOfValueObjects: Array<DummyAddress>? = null

    var listOfIntegers: MutableList<Int> = mutableListOf()
    var listOfDates: MutableList<LocalDate> = mutableListOf()
    val listOfEntities: MutableList<SnapshotEntity> = mutableListOf()
    var listOfValueObjects: MutableList<DummyAddress> = mutableListOf()
    var polymorficList: MutableList<Any?> = mutableListOf()

    var setOfIntegers: MutableSet<Int> = mutableSetOf()
    var setOfDates: MutableSet<LocalDate> = mutableSetOf()
    var setOfValueObjects: MutableSet<DummyAddress> = mutableSetOf()
    var polymorficset: MutableSet<Any?> = mutableSetOf()

    var optionalInt: Optional<Int> = Optional.empty()
    var optionalDate: Optional<LocalDate> = Optional.empty()
    var optionalEntity: Optional<SnapshotEntity> = Optional.empty()
    var optionalValueObject: Optional<DummyAddress> = Optional.empty()

    var multiSetOfPrimitives: Multiset<String>? = null
    var multiSetOfValueObject: Multiset<DummyAddress>? = null
    var multiSetOfEntities: Multiset<SnapshotEntity>? = null

    var multiMapOfPrimitives: Multimap<String, String>? = null
    var multiMapPrimitiveToValueObject: Multimap<String, DummyAddress>? = null
    var multiMapPrimitiveToEntity: Multimap<String, SnapshotEntity>? = null
    var multiMapEntityToEntity: Multimap<SnapshotEntity, SnapshotEntity>? = null
    //    var multiMapValueObjectToValueObject: Multimap<DummyAddress, DummyAddress>? = null // not supported

    val mapOfPrimitives: MutableMap<String, Int> = mutableMapOf()
    val mapOfValues: MutableMap<LocalDate, BigDecimal> = mutableMapOf()
    val mapPrimitiveToVO: MutableMap<String, DummyAddress> = mutableMapOf()
    val mapPrimitiveToEntity: MutableMap<String, SnapshotEntity> = mutableMapOf()
    val polymorficMap: MutableMap<Any, Any?> = mutableMapOf()
    val mapOfGenericValues: MutableMap<String, EnumSet<DummyEnum>> = mutableMapOf()

    var shallowPhone: ShallowPhone? = null
    var shallowPhones: MutableSet<ShallowPhone> = mutableSetOf()
    var shallowPhonesList: MutableList<ShallowPhone> = mutableListOf()
    var shallowPhonesMap: MutableMap<String, ShallowPhone> = mutableMapOf()

    //    val mapVoToPrimitive: MutableMap<DummyAddress, String> = mutableMapOf()  // not supported
    //    var nonParameterizedMap: Map<*, *>? = null                                       // not supported

}
