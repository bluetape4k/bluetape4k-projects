package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import io.bluetape4k.collections.eclipse.toFastList

fun destinationOf(
    vararg toAddress: String,
    @BuilderInference builder: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        this.toAddresses = toAddress.toFastList()

        builder()
    }

fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    @BuilderInference builder: Destination.Builder.() -> Unit = {},
): Destination =
    Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }

        builder()
    }
