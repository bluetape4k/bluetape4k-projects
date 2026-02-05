package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination
import io.bluetape4k.collections.eclipse.toFastList

fun destinationOf(
    vararg toAddress: String,
    @BuilderInference configurer: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        this.toAddresses = toAddress.toFastList()

        configurer()
    }

fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    @BuilderInference configurer: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }
    }
