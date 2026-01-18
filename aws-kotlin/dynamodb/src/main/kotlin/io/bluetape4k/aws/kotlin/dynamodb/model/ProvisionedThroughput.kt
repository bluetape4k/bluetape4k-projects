package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ProvisionedThroughput
import io.bluetape4k.support.requirePositiveNumber

fun provisionedThroughputOf(
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
): ProvisionedThroughput {
    readCapacityUnits?.requirePositiveNumber("readCapacity")
    writeCapacityUnits?.requirePositiveNumber("writeCapacity")

    return ProvisionedThroughput {
        this.readCapacityUnits = readCapacityUnits
        this.writeCapacityUnits = writeCapacityUnits
    }
}
