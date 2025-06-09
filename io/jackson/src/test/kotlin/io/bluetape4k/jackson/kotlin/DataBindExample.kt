package io.bluetape4k.jackson.kotlin

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.io.Serializable

class DataBindExample {

    companion object: KLogging()

    val mapper = Jackson.defaultJsonMapper

    interface InviteTo

    @JsonTypeName("CONTACT")
    data class InviteToContact(val name: String? = null): InviteTo

    @JsonTypeName("USER")
    data class InviteToUser(val user: String): InviteTo

    enum class InviteKind {
        CONTACT,
        USER
    }

    data class Invite(
        val kind: InviteKind,

        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "kind",
            visible = true
        )
        @JsonSubTypes(
            JsonSubTypes.Type(InviteToContact::class),
            JsonSubTypes.Type(InviteToUser::class)
        )
        val to: InviteTo,
    ): Serializable

    @Test
    fun `data bind with polymorphic enum`() {
        val contact = InviteToContact("Foo")
        val invite = Invite(InviteKind.CONTACT, contact)

        val json = mapper.writeValueAsString(invite)
        log.debug { "json=$json" }
        json shouldBeEqualTo """{"kind":"CONTACT","to":{"name":"Foo"}}"""

        val parsed = mapper.readValueOrNull<Invite>(json)!!
        parsed shouldBeEqualTo invite
        parsed.to shouldBeEqualTo contact
    }

    @Test
    fun `parse different types invite`() {
        val contact = InviteToContact("contact")
        val user = InviteToUser("user")

        val invites = listOf(
            Invite(InviteKind.CONTACT, contact),
            Invite(InviteKind.USER, user)
        )
        val expectedJson = """[{"kind":"CONTACT","to":{"name":"contact"}},{"kind":"USER","to":{"user":"user"}}]"""

        val json = mapper.writeValueAsString(invites)
        log.debug { "json=$json" }
        json shouldBeEqualTo expectedJson

        val parsed = mapper.readValueOrNull<List<Invite>>(json)!!
        parsed shouldHaveSize 2
        parsed[0].to shouldBeEqualTo contact
        parsed[1].to shouldBeEqualTo user
    }
}
