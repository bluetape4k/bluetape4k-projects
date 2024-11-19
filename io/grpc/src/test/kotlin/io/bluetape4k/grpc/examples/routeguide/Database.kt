package io.bluetape4k.grpc.examples.routeguide

import com.google.protobuf.util.JsonFormat
import io.bluetape4k.utils.Resourcex
import java.io.StringReader

internal fun defaultFeatureString(): String =
    Resourcex.getString("examples/routeguide/route_guide_db.json")

internal fun String.parseJsonFeatures(): List<Feature> =
    StringReader(this).use { reader ->
        FeatureDatabase.newBuilder()
            .apply {
                JsonFormat.parser().merge(reader, this)
            }
            .build()
            .featureList
    }
