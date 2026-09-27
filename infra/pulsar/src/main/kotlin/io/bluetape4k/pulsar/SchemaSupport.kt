package io.bluetape4k.pulsar

import io.bluetape4k.ToStringBuilder
import org.apache.pulsar.common.schema.SchemaInfo

fun SchemaInfo.toStringBuilder(): ToStringBuilder =
    ToStringBuilder(this)
        .add("name", name)
        .add("type", type)
        .add("properties", properties)
        .add("schemaDefinition", schemaDefinition)
        .add("timestamp", timestamp)
