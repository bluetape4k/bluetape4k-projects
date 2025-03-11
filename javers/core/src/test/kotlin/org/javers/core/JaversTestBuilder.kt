package org.javers.core

import org.javers.common.date.DateProvider
import org.javers.repository.api.JaversRepository

class JaversTestBuilder(val builder: JaversBuilder = JaversBuilder()) {

    fun javers(): Javers = builder.getContainerComponent(Javers::class.java)

    companion object {
        fun javersTestAssembly() = JaversTestBuilder().apply {
            builder.withMappingStyle(MappingStyle.FIELD).build()
        }

        fun javersTestAssembly(packageToScan: String) = JaversTestBuilder().apply {
            builder.withPackagesToScan(packageToScan).build()
        }

        fun javersTestAssembly(classToScan: Class<*>) = JaversTestBuilder().apply {
            builder.scanTypeName(classToScan).build()
        }

        fun javersTestAssembly(mappingStyle: MappingStyle) = JaversTestBuilder().apply {
            builder.withMappingStyle(mappingStyle).build()
        }

        fun javersTestAssembly(dateProvider: DateProvider) = JaversTestBuilder().apply {
            builder.withDateTimeProvider(dateProvider).build()
        }

        fun javersTestAssembly(repository: JaversRepository) = JaversTestBuilder().apply {
            builder.registerJaversRepository(repository).build()
        }

        fun newInstance(): Javers = javersTestAssembly().javers()
    }

//    internal inline fun <reified T: Any> getComponent(): T = builder.getContainerComponent(T::class.java)

//    val snapshotFactory: SnapshotFactory by lazy { getComponent<SnapshotFactory>() }
//    val javersRepository: JaversRepository by lazy { getComponent<JaversRepository>() }
//    val typeMapper: TypeMapper by lazy { getComponent<TypeMapper>() }
//    val queryRunner: QueryRunner by lazy { getComponent<QueryRunner>() }
//    val globalIdFactory: GlobalIdFactory by lazy { getComponent<GlobalIdFactory>() }
//    val liveCdoSnapshot: LiveCdoFactory by lazy { getComponent<LiveCdoFactory>() }
//    val commitFactory: CommitFactory by lazy { getComponent<CommitFactory>() }
//    val jsonConverter: JsonConverter by lazy { getComponent<JsonConverter>() }
//    val shadowFactory: ShadowFactory by lazy { getComponent<ShadowFactory>() }

//    inline fun <reified T> getManagedType(): ManagedType =
//        typeMapper.getJaversManagedType(T::class.java)
//
//    inline fun <reified T> getProperty(propertyName: String): Property =
//        getManagedType<T>().getProperty(propertyName)
//
//    fun getProperty(type: KClass<*>, propertyName: String): Property =
//        typeMapper.getJaversManagedType(type.java).getProperty(propertyName)

//    fun getJsonConvertBuilder(): JsonConverterBuilder = getComponent<JsonConverterBuilder>()

//    fun hash(obj: Any): String {
//        val jsonState = jsonConverter.toJson(javers().commit("", obj).snapshots.first().state)
//        return ShaDigest.longDigest(jsonState)
//    }

//    fun addressHash(city: String): String = hash(DummyAddress(city = city))
//
//    fun instanceId(instance: Any): InstanceId =
//        globalIdFactory.createIdFromInstance(instance)
//
//    fun <T: Any> instanceId(localId: Any, entityKClass: KClass<T>): InstanceId =
//        globalIdFactory.createInstanceId(localId, entityKClass.java)
//
//    inline fun <reified T: Any> instanceId(localId: Any): InstanceId =
//        globalIdFactory.createInstanceId(localId, T::class.java)
//
//    inline fun <reified V: Any> unboundedValueObjectId(): UnboundedValueObjectId =
//        globalIdFactory.createUnboundedValueObjectId(V::class.java)
//
//    fun <V: Any> unboundedValueObjectId(valueObjectKClass: KClass<V>): UnboundedValueObjectId =
//        globalIdFactory.createUnboundedValueObjectId(valueObjectKClass.java)
}
