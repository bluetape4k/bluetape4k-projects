package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AutoScalingSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.CreateReplicaAction
import aws.sdk.kotlin.services.dynamodb.model.DeleteReplicaAction
import aws.sdk.kotlin.services.dynamodb.model.Replica
import aws.sdk.kotlin.services.dynamodb.model.ReplicaGlobalSecondaryIndexSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaUpdate
import aws.sdk.kotlin.services.dynamodb.model.TableClass
import io.bluetape4k.support.requireNotBlank

inline fun replicaOf(
    regionName: String,
    @BuilderInference crossinline builder: Replica.Builder.() -> Unit = {},
): Replica {
    regionName.requireNotBlank("regionName")

    return Replica {
        this.regionName = regionName

        builder()
    }
}

inline fun replicaUpdateOf(
    create: CreateReplicaAction? = null,
    delete: DeleteReplicaAction? = null,
    @BuilderInference crossinline builder: ReplicaUpdate.Builder.() -> Unit = {},
): ReplicaUpdate =
    ReplicaUpdate {
        this.create = create
        this.delete = delete

        builder()
    }

inline fun replicaSettingsUpdateOf(
    regionName: String,
    replicaGlobalSecondaryIndexSettingsUpdate: List<ReplicaGlobalSecondaryIndexSettingsUpdate>? = null,
    replicaProvisionedReadCapacityAutoScalingSettingsUpdate: AutoScalingSettingsUpdate? = null,
    replicaProvisionedReadCapacityUnits: Long? = null,
    replicaTableClass: TableClass? = null,
    @BuilderInference crossinline builder: ReplicaSettingsUpdate.Builder.() -> Unit = {},
): ReplicaSettingsUpdate {
    regionName.requireNotBlank("regionName")

    return ReplicaSettingsUpdate.invoke {
        this.regionName = regionName
        this.replicaGlobalSecondaryIndexSettingsUpdate = replicaGlobalSecondaryIndexSettingsUpdate
        this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate =
            replicaProvisionedReadCapacityAutoScalingSettingsUpdate
        this.replicaProvisionedReadCapacityUnits = replicaProvisionedReadCapacityUnits
        this.replicaTableClass = replicaTableClass

        builder()
    }
}
