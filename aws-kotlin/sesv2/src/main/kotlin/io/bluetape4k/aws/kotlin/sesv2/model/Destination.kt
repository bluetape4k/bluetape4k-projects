package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination

inline fun destinationOf(
    vararg toAddress: String,
    crossinline configurer: Destination.Builder.() -> Unit = {},
): Destination {
    return Destination {
        this.toAddresses = toAddress.toList()

        configurer()
    }
}

fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
): Destination {
    return Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }
    }
}
