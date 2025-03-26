package io.bluetape4k.exposed.dao.id

import org.jetbrains.exposed.dao.id.IdTable

/**
 * Soft Deleted 를 지원하는 [IdTable] 입니다.
 */
abstract class SoftDeletedIdTable<T: Any>(name: String = ""): IdTable<T>(name) {

    /**
     * Soft Deleted 여부를 나타내는 필드입니다.
     */
    val isDeleted = bool("is_deleted").default(false)

}
