package io.bluetape4k.hibernate

import org.hibernate.SessionFactory
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.internal.SessionFactoryImpl

/**
 * Hibernate [SessionFactory]에 Event listener를 등록합니다.
 *
 * ```
 * sessionFactory.registEventListener(MyListener(), EventType.PRE_INSERT, EventType.POST_INSERT)
 * ```
 *
 * @param listener Event listener
 * @param eventTypes Event types (e.g. [EventType.PRE_INSERT], [EventType.POST_INSERT])
 */
@Suppress("UNCHECKED_CAST")
fun <T> SessionFactory.registEventListener(
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
 * Hibernate [SessionFactory]의 [EventListenerRegistry]를 가져옵니다.
 */
fun SessionFactory.getEventListenerRegistry(): EventListenerRegistry? {
    return (this as? SessionFactoryImpl)?.serviceRegistry?.getService(EventListenerRegistry::class.java)
}

/**
 * [entityClass] 수형에 대한 `Entity Name`을 가져옵니다. 없으면 null을 반환합니다.
 */
fun SessionFactory.getEntityName(entityClass: Class<*>): String? {
    return this.metamodel.entity(entityClass)?.name
}

/**
 * [T] 수형에 대한 `Entity Name`을 가져옵니다. 없으면 null을 반환합니다.
 */
inline fun <reified T: Any> SessionFactory.getEntityName(): String? {
    return getEntityName(T::class.java)
}
