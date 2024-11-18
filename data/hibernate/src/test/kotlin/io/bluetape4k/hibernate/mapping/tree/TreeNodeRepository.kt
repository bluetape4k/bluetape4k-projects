package io.bluetape4k.hibernate.mapping.tree

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TreeNodeRepository: JpaRepository<TreeNode, Long> {

    /**
     * 부모 노드가 null 인 모든 Tree Node (Root) 를 조회합니다.
     */
    @Query("select n from tree_treenode n where n.parent is null")
    fun findRoots(): List<TreeNode>
}
