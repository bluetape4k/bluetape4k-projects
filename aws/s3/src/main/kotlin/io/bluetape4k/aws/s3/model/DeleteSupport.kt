package io.bluetape4k.aws.s3.model

import io.bluetape4k.collections.eclipse.toFastList
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

inline fun delete(
    @BuilderInference builder: Delete.Builder.() -> Unit,
): Delete =
    Delete.builder().apply(builder).build()

@JvmName("deleteOfObjectsArray")
fun deleteOf(
    vararg objectIds: ObjectIdentifier,
    quiet: Boolean = false,
): Delete =
    delete {
        objects(objectIds.toFastList())
        quiet(quiet)
    }

@JvmName("deleteOfObjectsCollection")
fun deleteOf(
    objectIds: Collection<ObjectIdentifier>,
    quiet: Boolean = false,
): Delete =
    delete {
        objects(objectIds)
        quiet(quiet)
    }
