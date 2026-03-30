package io.bluetape4k.idgenerators.uuid.timebased

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import java.util.*

class ReorderedTimebaseUuidTest: AbstractTimebasedUuidTest() {

    override val uuidGenerator: IdGenerator<UUID> = Uuid.V6

}
