package io.bluetape4k.examples.jpa.blazepersistence.domain.view

import com.blazebit.persistence.view.EntityView
import com.blazebit.persistence.view.IdMapping
import com.blazebit.persistence.view.Mapping
import io.bluetape4k.examples.jpa.blazepersistence.domain.model.Member

/**
 * Member entity view with nested team projection fields.
 */
@EntityView(Member::class)
interface MemberTeamView {

    @get:IdMapping
    val id: Long?

    val name: String

    val age: Int?

    @get:Mapping("team.id")
    val teamId: Long?

    @get:Mapping("team.name")
    val teamName: String?
}
