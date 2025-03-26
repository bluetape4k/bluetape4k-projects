package io.bluetape4k.exposed.repository

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Exposed 를 사용하는 Repository 의 기본 추상 클래스입니다.
 */
@Deprecated("Use ExposedRepository instead", replaceWith = ReplaceWith("ExposedRepository"))
abstract class AbstractExposedRepository<T: Entity<ID>, ID: Any>(
    override val table: IdTable<ID>,
): ExposedRepository<T, ID> {

    companion object: KLogging()

}
