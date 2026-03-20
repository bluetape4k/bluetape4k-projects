@file:OptIn(ExperimentalUuidApi::class)

package io.bluetape4k.idgenerators.ulid

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ULID를 Kotlin [Uuid]로 변환합니다.
 */
fun ULID.toUuid(): Uuid = Uuid.fromLongs(this.mostSignificantBits, this.leastSignificantBits)

/**
 * Kotlin [Uuid]를 ULID로 변환합니다.
 */
fun ULID.Companion.fromUuid(uuid: Uuid): ULID = ULID.fromByteArray(uuid.toByteArray())
