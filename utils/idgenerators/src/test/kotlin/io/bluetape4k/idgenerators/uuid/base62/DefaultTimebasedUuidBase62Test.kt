package io.bluetape4k.idgenerators.uuid.base62

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import java.util.*

class DefaultTimebasedUuidBase62Test: AbstractTimebasedUuidBase62Test() {

    override val uuidGenerator: IdGenerator<UUID> = Uuid.V1

}
