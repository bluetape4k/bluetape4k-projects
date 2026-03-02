package io.bluetape4k.exposed.core.dao.id

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * 논리 삭제 플래그(`is_deleted`)를 포함한 `IdTable` 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - 실제 DELETE 대신 `isDeleted=true` 업데이트 방식의 soft delete 모델을 전제로 합니다.
 * - `isDeleted` 컬럼은 nullable이 아니며 기본값은 `false`입니다.
 *
 * ```kotlin
 * abstract class UsersBase: SoftDeletedIdTable<Long>("users")
 * // UsersBase().isDeleted.name == "is_deleted"
 * ```
 */
abstract class SoftDeletedIdTable<T: Any>(name: String = ""): IdTable<T>(name) {

    /**
     * Soft Deleted 여부를 나타내는 필드입니다.
     */
    val isDeleted: Column<Boolean> = bool("is_deleted").default(false)

}
