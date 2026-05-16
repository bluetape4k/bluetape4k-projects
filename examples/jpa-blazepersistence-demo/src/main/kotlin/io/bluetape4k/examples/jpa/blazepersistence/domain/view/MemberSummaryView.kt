package io.bluetape4k.examples.jpa.blazepersistence.domain.view

import com.blazebit.persistence.view.EntityView
import com.blazebit.persistence.view.IdMapping
import io.bluetape4k.examples.jpa.blazepersistence.domain.model.Member

/**
 * Minimal member entity view.
 */
@EntityView(Member::class)
interface MemberSummaryView {

    @get:IdMapping
    val id: Long?

    val name: String

    val age: Int?
}
