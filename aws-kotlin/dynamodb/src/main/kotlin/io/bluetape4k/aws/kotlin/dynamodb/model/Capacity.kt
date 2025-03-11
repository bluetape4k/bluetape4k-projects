package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Capacity
import io.bluetape4k.support.requirePositiveNumber

inline fun capacityOf(
    capacityUnits: Double? = null,
    readCapacityUnits: Double? = null,
    writeCapacityUnits: Double? = null,
    crossinline configurer: Capacity.Builder.() -> Unit = {},
): Capacity {
    capacityUnits?.requirePositiveNumber("capacityUnits")
    readCapacityUnits?.requirePositiveNumber("readCapacityUnits")
    writeCapacityUnits?.requirePositiveNumber("writeCapacityUnits")

    return Capacity {
        this.capacityUnits = capacityUnits
        this.readCapacityUnits = readCapacityUnits
        this.writeCapacityUnits = writeCapacityUnits

        configurer()
    }
}
