package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination

fun destinationOf(
    vararg toAddress: String,
    @BuilderInference configurer: Destination.Builder.() -> Unit = {},
): Destination {
    require(toAddress.isNotEmpty()) { "toAddress must not be empty." }

    return Destination {
        this.toAddresses = toAddress.toList()

        configurer()
    }
}

fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    @BuilderInference configurer: Destination.Builder.() -> Unit = {},
): Destination {
    val hasAddress = !toAddresses.isNullOrEmpty() || !ccAddresses.isNullOrEmpty() || !bccAddresses.isNullOrEmpty()
    require(hasAddress) { "At least one address must be provided." }

    return Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }
        configurer()
    }
}
