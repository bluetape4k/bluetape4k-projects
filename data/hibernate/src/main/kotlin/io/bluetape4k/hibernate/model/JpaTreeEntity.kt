package io.bluetape4k.hibernate.model

/**
 * Self reference 를 가지는 Tree 구조의 엔티니를 표현합니다.
 */
interface JpaTreeEntity<T>: PersistenceObject where T: JpaTreeEntity<T> {

    /**
     * Parent 엔티티를 나타냅니다. null 이라면 현 엔티티가 root 노드임을 의미합니다.
     */
    var parent: T?

    /**
     * children entities
     */
    val children: MutableSet<T>

    // Node 위치를 나타내도록 합니다. 거의 사용하지 않아 삭제할 예정입니다.
    // val nodePosition: TreeNodePosition

    /**
     * add children entities
     */
    @Suppress("UNCHECKED_CAST")
    fun addChildren(vararg childs: T) {
        childs.forEach {
            if (children.add(it)) {
                it.parent = this as T
            }
        }
    }

    /**
     * remove children entities
     */
    fun removeChildren(vararg childs: T) {
        childs.forEach {
            if (children.remove(it)) {
                it.parent = null
            }
        }
    }
}
