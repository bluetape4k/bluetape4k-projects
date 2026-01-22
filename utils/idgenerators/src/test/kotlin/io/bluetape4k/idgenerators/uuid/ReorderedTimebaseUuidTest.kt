package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

class ReorderedTimebaseUuidTest: AbstractTimebasedUuidTest() {

    override val uuidGenerator: IdGenerator<UUID> = TimebasedUuid.Reordered

}
