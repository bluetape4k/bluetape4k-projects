package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

/**
 * Timebased UUID 를 제공하는 object 입니다.
 */
object TimebasedUuid {

    /**
     * UUID v6 형식의 Timebased UUID Identifier를 생성합니다.
     */
    object Default: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedGenerator() }

        override fun nextId(): UUID {
            return generator.generate()
        }

        override fun nextIdAsString(): String {
            return nextId().encodeBase62()
        }
    }

    /**
     * Mac 기반의 Timebased UUID Identifier를 생성합니다.
     */
    object Reordered: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedReorderedGenerator() }

        override fun nextId(): UUID {
            return generator.generate()
        }

        override fun nextIdAsString(): String {
            return nextId().encodeBase62()
        }
    }

    /**
     * UUID v7 형식의 Timebased UUID Identifier를 생성합니다.
     */
    object Epoch: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedEpochGenerator() }

        override fun nextId(): UUID {
            return generator.generate()
        }

        override fun nextIdAsString(): String {
            return nextId().encodeBase62()
        }
    }

    @Deprecated("use TimebasedUuid.Reordered")
    private val generator: TimebasedUuidGenerator by lazy { TimebasedUuidGenerator() }

    @Deprecated(
        "use TimebasedUuid.Reordered.nextId()",
        replaceWith = ReplaceWith("TimebasedUuid.Reordered.nextId()")
    )
    fun nextUUID(): UUID = generator.nextUUID()

    @Deprecated(
        "use TimebasedUuid.Reordered.nextIds(size)",
        replaceWith = ReplaceWith("TimebasedUuid.Reordered.nextIds(size)")
    )
    fun nextUUIDs(size: Int): Sequence<UUID> = generator.nextUUIDs(size)

    @Deprecated(
        "use TimebasedUuid.Reordered.nextIdAsString()",
        replaceWith = ReplaceWith("TimebasedUuid.Reordered.nextIdAsString()")
    )
    fun nextBase62String(): String = generator.nextBase62String()

    @Deprecated(
        "use TimebasedUuid.Reordered.nextIdAsStrings(size)",
        replaceWith = ReplaceWith("TimebasedUuid.Reordered.nextIdAsStrings(size)")
    )
    fun nextBase62Strings(size: Int): Sequence<String> = generator.nextBase62Strings(size)

}
