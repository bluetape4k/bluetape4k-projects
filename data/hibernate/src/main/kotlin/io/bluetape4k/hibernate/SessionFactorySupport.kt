package io.bluetape4k.hibernate

import org.hibernate.SessionFactory
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.internal.SessionFactoryImpl

/**
 * Hibernate [SessionFactory]에 Event listener를 등록합니다.
 *
 * ## 동작/계약
 * - [getEventListenerRegistry]가 `null`이 아니면 각 [eventTypes] 그룹에 `appendListener`를 수행합니다.
 * - registry를 얻지 못한 경우 아무 작업 없이 종료합니다.
 *
 * ```kotlin
 * sessionFactory.registerEventListener(MyListener(), EventType.PRE_INSERT, EventType.POST_INSERT)
 * // 지정 이벤트 타입 그룹에 listener가 추가됨
 * ```
 *
 * @param listener Event listener입니다.
 * @param eventTypes 대상 이벤트 타입 컬렉션입니다.
 */
@Suppress("UNCHECKED_CAST")
fun <T> SessionFactory.registerEventListener(
    listener: T,
    eventTypes: Collection<EventType<*>>,
) {
    getEventListenerRegistry()?.let { registry ->
        eventTypes.forEach { eventType ->
            registry.getEventListenerGroup(eventType as EventType<T>).appendListener(listener)
        }
    }
}

/**
 * 오타가 포함된 이전 API 이름.
 *
 * 유지보수 호환성을 위해 남겨두며, 새 코드에서는 [registerEventListener]를 사용하세요.
 */
@Deprecated(
    message = "Use registerEventListener instead.",
    replaceWith = ReplaceWith("registerEventListener(listener, eventTypes)")
)
fun <T> SessionFactory.registEventListener(
    listener: T,
    eventTypes: Collection<EventType<*>>,
) = registerEventListener(listener, eventTypes)

/**
 * Hibernate [SessionFactory]의 [EventListenerRegistry]를 가져옵니다.
 *
 * ## 동작/계약
 * - `SessionFactoryImpl`로 캐스팅 가능한 경우에만 service registry에서 반환합니다.
 * - Hibernate 구현체가 아니면 `null`을 반환합니다.
 */
fun SessionFactory.getEventListenerRegistry(): EventListenerRegistry? {
    return (this as? SessionFactoryImpl)?.serviceRegistry?.getService(EventListenerRegistry::class.java)
}

/**
 * [entityClass] 수형에 대한 `Entity Name`을 가져옵니다. 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - metamodel 조회 실패 시 `null`을 반환합니다.
 */
fun SessionFactory.getEntityName(entityClass: Class<*>): String? {
    return this.metamodel.entity(entityClass)?.name
}

/**
 * [T] 수형에 대한 `Entity Name`을 가져옵니다. 없으면 null을 반환합니다.
 */
inline fun <reified T> SessionFactory.getEntityName(): String? {
    return getEntityName(T::class.java)
}
