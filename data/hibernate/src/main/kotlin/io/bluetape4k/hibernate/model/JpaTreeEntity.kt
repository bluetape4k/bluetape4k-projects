package io.bluetape4k.hibernate.model

/**
 * Self reference 를 가지는 Tree 구조의 엔티티를 표현합니다.
 */
interface JpaTreeEntity<T>: PersistenceObject where T: JpaTreeEntity<T> {

    /**
     * 부모 엔티티를 나타냅니다. null 이라면 현 엔티티가 root 노드임을 의미합니다.
     */
    var parent: T?

    /**
     * 자식 엔티티 목록입니다.
     */
    val children: MutableSet<T>

    // Node 위치를 나타내도록 합니다. 거의 사용하지 않아 삭제할 예정입니다.
    // val nodePosition: TreeNodePosition

    /**
     * 자식 엔티티를 추가합니다.
     *
     * 추가된 자식 엔티티의 [parent]를 현재 엔티티로 설정합니다.
     *
     * @param childs 추가할 자식 엔티티들
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
     * 자식 엔티티를 제거합니다.
     *
     * 제거된 자식 엔티티의 [parent]를 null 로 설정합니다.
     *
     * @param childs 제거할 자식 엔티티들
     */
    fun removeChildren(vararg childs: T) {
        childs.forEach {
            if (children.remove(it)) {
                it.parent = null
            }
        }
    }
}
