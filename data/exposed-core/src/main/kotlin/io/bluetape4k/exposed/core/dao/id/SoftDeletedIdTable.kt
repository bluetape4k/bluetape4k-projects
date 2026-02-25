package io.bluetape4k.exposed.core.dao.id

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Soft Delete 패턴을 지원하는 [org.jetbrains.exposed.v1.core.dao.id.IdTable] 베이스 클래스입니다.
 *
 * 실제 삭제 대신 [isDeleted] 플래그를 `true` 로 업데이트하여 논리 삭제를 표현합니다.
 * 기본값은 `false` 이며 nullable 이 아닙니다.
 */
abstract class SoftDeletedIdTable<T: Any>(name: String = ""): IdTable<T>(name) {

    /**
     * Soft Deleted 여부를 나타내는 필드입니다.
     */
    val isDeleted: Column<Boolean> = bool("is_deleted").default(false)

}
