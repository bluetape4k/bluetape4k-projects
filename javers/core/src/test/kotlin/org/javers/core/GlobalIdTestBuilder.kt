package org.javers.core

object GlobalIdTestBuilder {

    val javersTestBuilder = JaversTestBuilder.javersTestAssembly()

//    fun instanceId(instance: Any): InstanceId = javersTestBuilder.instanceId(instance)
//
//    fun <T: Any> instanceId(localId: Any, kclass: KClass<T>): InstanceId =
//        javersTestBuilder.instanceId(localId, kclass)
//
//    fun <E: Any> valueObjectId(localId: Any, owningEntityKClass: KClass<E>, fragment: String): ValueObjectId =
//        ValueObjectId("?", instanceId(localId, owningEntityKClass), fragment)
//
//    inline fun <reified V: Any> unboundedValueObjectId(): UnboundedValueObjectId =
//        javersTestBuilder.unboundedValueObjectId<V>()
}
