package io.bluetape4k.idgenerators.uuid.base62

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import java.util.*

class EpochTimebasedUuidBase62Test: AbstractTimebasedUuidBase62Test() {

    override val uuidGenerator: IdGenerator<UUID> = TimebasedUuid.Epoch

}
