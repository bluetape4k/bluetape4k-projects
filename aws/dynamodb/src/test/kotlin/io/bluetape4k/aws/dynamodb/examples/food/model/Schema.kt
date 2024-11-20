package io.bluetape4k.aws.dynamodb.examples.food.model

object Schema {
    const val TABLE_NAME = "foods"

    const val IDX_SORT_KEY_PARTITION_KEY = "idx_sort_key_partition_key"
    const val IDX_PK_UPDATED_AT = "idx_pk_updated_at"
    const val IDX_SK_UPDATED_AT = "idx_sk_updated_at"

    val LOCAL_INDICES = listOf(
        IDX_PK_UPDATED_AT,
    )

    val GLOBAL_INDICES = listOf(
        IDX_SORT_KEY_PARTITION_KEY,
        IDX_SK_UPDATED_AT,
    )
}
