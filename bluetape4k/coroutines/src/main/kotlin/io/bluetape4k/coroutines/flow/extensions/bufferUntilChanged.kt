package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [groupSelector] 로 얻은 값이 이전 값과 같은 경우를 묶어서 Flow로 전달하려고 할 때 사용합니다.
 *
 * 예: One-To-Many 정보의 ResultSet 을 묶을 때 사용합니다.
 *
 * ```
 * // Order - OrderItem 관계를 가정
 * // Order 단위로 one-to-many 관계를 가지도록 묶습니다.
 * val orders = getOrderRows(orderCount, itemCount).log("source")
 *     .bufferUntilChanged { it.orderId }.log("buffer")
 *     .map { rows ->
 *         Order(rows[0].orderId, rows.map { OrderItem(it.itemId, it.itemName, it.itemQuantity) })
 *     }
 *     .toList()
 * ```
 *
 * 참고: [Spring R2DBC OneToMany RowMapping](https://heesutory.tistory.com/33)
 */
fun <T, K> Flow<T>.bufferUntilChanged(groupSelector: (T) -> K): Flow<List<T>> = flow {
    // HINT: groupBy 를 사용할 수도 있다
    //    return this@bufferUntilChanged
    //        .groupBy { groupSelector(it) }
    //        .flatMapMerge { it.toList() }


    val self = this@bufferUntilChanged
    val elements = mutableListOf<T>()
    var prevGroup: K? = null

    self.collect { element ->
        val currentGroup = groupSelector(element)
        prevGroup = prevGroup ?: currentGroup

        if (prevGroup == currentGroup) {
            elements.add(element)
        } else {
            emit(elements.toList())
            elements.clear()
            elements.add(element)
            prevGroup = currentGroup
        }
    }

    if (elements.isNotEmpty()) {
        emit(elements.toList())
        elements.clear()
    }
}
