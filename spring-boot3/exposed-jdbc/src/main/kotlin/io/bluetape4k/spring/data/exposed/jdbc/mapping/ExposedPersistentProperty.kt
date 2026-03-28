package io.bluetape4k.spring.data.exposed.jdbc.mapping

import org.jetbrains.exposed.v1.core.Column
import org.springframework.data.mapping.PersistentProperty

/**
 * Exposed Column을 Spring Data PersistentProperty로 표현합니다.
 */
interface ExposedPersistentProperty : PersistentProperty<ExposedPersistentProperty> {

    /**
     * 이 프로퍼티에 대응하는 Exposed [Column] 인스턴스 (없으면 null)
     */
    fun getColumn(): Column<*>?
}
