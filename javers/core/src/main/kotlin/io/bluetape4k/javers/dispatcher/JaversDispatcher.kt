package io.bluetape4k.javers.dispatcher

/**
 * Domain Object의 Save, Delete 시에 외부로 event sourcing 을 수행하도록 합니다.
 */
interface JaversDispatcher {

    /**
     * Domain Object 에 대해 Save (Create/Update) 시에 호출되는 메소드입니다. 이를 event sourcing으로 외부에 알립니다.
     *
     * @param domainObject 저장된 domain object
     */
    fun sendSaved(domainObject: Any)

    /**
     * Domain Object 삭제 시에 호출되는 메소드
     *
     * @param domainObject
     */
    fun sendDeleted(domainObject: Any)

    /**
     * Domain Object를 Id로 삭제하는 경우 호출되는 메소드
     *
     * @param domainObjectId 삭제된 domain object의 id
     * @param domainType domain object의 type 정보
     */
    fun sendDeletedById(domainObjectId: Any, domainType: Class<*>)
}

/**
 * Domain Object를 Id로 삭제하는 경우 호출되는 메소드
 *
 * @param domainObjectId 삭제된 domain object의 id
 */
inline fun <reified T> JaversDispatcher.sendDeletedById(domainObjectId: Any) {
    sendDeletedById(domainObjectId, T::class.java)
}
