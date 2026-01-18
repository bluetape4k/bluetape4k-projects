package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination

inline fun destinationOf(
    vararg toAddress: String,
    crossinline builder: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        this.toAddresses = toAddress.toList()

        builder()
    }

inline fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    crossinline builder: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }

        builder()
    }
