package org.javers.core.model

import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.ShallowReference
import java.io.Serializable

abstract class AbstractCategory @JvmOverloads constructor(
    var name: String? = null,
): Serializable {

    var parent: AbstractCategory? = null
    val categories: MutableList<AbstractCategory> = mutableListOf()

    fun addChild(child: AbstractCategory) {
        child.parent = this
        this.categories.add(child)
    }
}

class CategoryC @JvmOverloads constructor(
    var id: Long,
    name: String = "name",
): AbstractCategory("$name$id")

data class CategoryVo(var name: String? = null) {

    var parent: CategoryVo? = null
    val children: MutableList<CategoryVo> = mutableListOf()

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
    var shallowCategories: MutableSet<CategoryC> = mutableSetOf()

    @ShallowReference
    var shallowCategoryList: MutableList<CategoryC> = mutableListOf()

    @ShallowReference
    var shallowCategoryMap: MutableMap<String, CategoryC> = mutableMapOf()

}
