package org.javers.core.model

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.ShallowReference
import java.io.Serializable

abstract class AbstractCategory(
    var name: String? = null,
): Serializable {

    var parent: AbstractCategory? = null
    val categories: MutableList<AbstractCategory> = fastListOf()

    fun addChild(child: AbstractCategory) {
        child.parent = this
        this.categories.add(child)
    }
}

class CategoryC(
    var id: Long,
    name: String = "name",
): AbstractCategory("$name$id")

data class CategoryVo(var name: String? = null) {

    var parent: CategoryVo? = null
    val children: MutableList<CategoryVo> = fastListOf()

    fun addChild(child: CategoryVo) {
        child.parent = this
        children.add(child)
    }
}

data class PhoneWithShallowCategory(@Id var id: Long): Serializable {

    var number: String = "123"
    var deepCategory: CategoryC? = null

    @ShallowReference
    var shallowCategory: CategoryC? = null

    @ShallowReference
    var shallowCategories: MutableSet<CategoryC> = unifiedSetOf()

    @ShallowReference
    var shallowCategoryList: MutableList<CategoryC> = fastListOf()

    @ShallowReference
    var shallowCategoryMap: MutableMap<String, CategoryC> = unifiedMapOf()

}
