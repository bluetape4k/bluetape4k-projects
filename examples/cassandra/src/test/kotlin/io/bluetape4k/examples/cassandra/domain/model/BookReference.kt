package io.bluetape4k.examples.cassandra.domain.model

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("bookReference")
data class BookReference(
    @field:PrimaryKey
    val isbn: String = "",

    var title: String = "",
    var references: UnifiedSet<String> = unifiedSetOf(),
    var bookmarks: FastList<String> = fastListOf(),
    var credits: UnifiedMap<String, String> = unifiedMapOf(),
): Serializable
