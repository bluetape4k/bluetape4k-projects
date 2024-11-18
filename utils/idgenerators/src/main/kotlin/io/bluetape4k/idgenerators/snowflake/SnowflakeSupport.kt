package io.bluetape4k.idgenerators.snowflake

internal const val TOTAL_BITS = 64
internal const val TIMESTAMP_BITS = 42
internal const val MACHINE_BITS = 10
internal const val SEQUENCE_BITS = 12
internal const val TIME_STAMP_SHIFT = TOTAL_BITS - TIMESTAMP_BITS
internal const val MACHINE_ID_SHIFT = TOTAL_BITS - TIMESTAMP_BITS - MACHINE_BITS  // 64-42-10

// Custom Epoch (UTC = 2015-01-01T00:00:00Z)
internal const val EPOCH = 1420070400000L

/** Max Machine Id */
internal const val MAX_MACHINE_ID = 1024

/** Max Sequence */
internal const val MAX_SEQUENCE = 4_096

internal const val MAX_MACHINE_ID_BIT = MAX_MACHINE_ID - 1L
internal const val MAX_SEQUENCE_BIT = MAX_SEQUENCE - 1L

/**
 * Snowflake 알고리즘을 사용하여 Long 수형의 Id를 생성합니다.
 *
 * @param timestamp 발급 시각
 * @param machineId 발급자 Id
 * @param sequence 발급 순번
 * @return Snowflake Id
 */
internal fun makeId(timestamp: Long, machineId: Int, sequence: Int): Long {
    return ((timestamp - EPOCH) shl TIME_STAMP_SHIFT) or
            (machineId shl MACHINE_ID_SHIFT).toLong() or
            sequence.toLong()
}

/**
 * Snowflake로 생성된 [id] 값을 파싱하여 각 요소별로 분해하여 [SnowflakeId]를 빌드합니다.
 *
 * @param id Snowflake Id의 Long 값
 * @see makeId
 */
internal fun parseSnowflakeId(id: Long): SnowflakeId {
    val timestamp = (id ushr TIME_STAMP_SHIFT) + EPOCH
    val machineId = ((id ushr MACHINE_ID_SHIFT) and MAX_MACHINE_ID_BIT).toInt()
    val sequence = (id and MAX_SEQUENCE_BIT).toInt()

    return SnowflakeId(timestamp, machineId, sequence)
}
