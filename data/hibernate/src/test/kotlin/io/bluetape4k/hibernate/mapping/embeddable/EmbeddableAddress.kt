package io.bluetape4k.hibernate.mapping.embeddable

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class EmbeddableAddress(
    var street: String,
    var city: String,
    var zipcode: String,
): Serializable
