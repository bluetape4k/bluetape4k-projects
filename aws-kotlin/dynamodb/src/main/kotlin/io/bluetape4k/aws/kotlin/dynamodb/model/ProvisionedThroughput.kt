package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ProvisionedThroughput
import io.bluetape4k.support.requirePositiveNumber

fun provisionedThroughputOf(
    readCapacity: Long = 100L,
    writeCapacity: Long = 10L,
): ProvisionedThroughput {
    readCapacity.requirePositiveNumber("readCapacity")
    writeCapacity.requirePositiveNumber("writeCapacity")

    return ProvisionedThroughput {
        this.readCapacityUnits = readCapacity
        this.writeCapacityUnits = writeCapacity
    }
}
