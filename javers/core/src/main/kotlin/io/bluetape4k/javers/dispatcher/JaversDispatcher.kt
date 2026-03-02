package io.bluetape4k.javers.dispatcher

/**
 * 도메인 객체의 저장/삭제 이벤트를 외부에 전파하는 디스패처 인터페이스.
 *
 * ## 동작/계약
 * - [sendSaved]: 객체 생성 또는 수정 시 호출
 * - [sendDeleted]: 객체 인스턴스 기반 삭제 시 호출
 * - [sendDeletedById]: ID 기반 삭제 시 호출
 */
interface JaversDispatcher {

    /**
     * 도메인 객체 저장(생성/수정) 이벤트를 외부에 전파한다.
     *
     * @param domainObject 저장된 도메인 객체
     */
    fun sendSaved(domainObject: Any)

    /**
     * 도메인 객체 삭제 이벤트를 외부에 전파한다.
     *
     * @param domainObject 삭제된 도메인 객체
     */
    fun sendDeleted(domainObject: Any)

    /**
     * ID 기반 도메인 객체 삭제 이벤트를 외부에 전파한다.
     *
     * @param domainObjectId 삭제된 도메인 객체의 ID
     * @param domainType 도메인 객체의 타입 정보
     */
    fun sendDeletedById(domainObjectId: Any, domainType: Class<*>)
}

/**
 * reified 타입 파라미터로 ID 기반 삭제 이벤트를 전파한다.
 *
 * ```kotlin
 * dispatcher.sendDeletedById<User>(userId)
 * ```
 */
inline fun <reified T: Any> JaversDispatcher.sendDeletedById(domainObjectId: Any) {
    sendDeletedById(domainObjectId, T::class.java)
}
