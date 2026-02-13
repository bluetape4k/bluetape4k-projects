package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.support.requireNotEmpty

inline fun destinationOf(
    vararg toAddresses: String,
    @BuilderInference crossinline builder: Destination.Builder.() -> Unit = {},
): Destination {
    toAddresses.requireNotEmpty("toAddresses")

    return Destination {
        this.toAddresses = toAddresses.toFastList()

        builder()
    }
}

inline fun destinationOf(
    toAddresses: List<String>? = null,
    ccAddresses: List<String>? = null,
    bccAddresses: List<String>? = null,
    @BuilderInference crossinline builder: Destination.Builder.() -> Unit = {},
): Destination {
    val hasAddress = !toAddresses.isNullOrEmpty() || !ccAddresses.isNullOrEmpty() || !bccAddresses.isNullOrEmpty()
    require(hasAddress) { "At least one address must be provided." }

    return Destination {
        toAddresses?.let { this.toAddresses = it }
        ccAddresses?.let { this.ccAddresses = it }
        bccAddresses?.let { this.bccAddresses = it }

        builder()
    }
}
