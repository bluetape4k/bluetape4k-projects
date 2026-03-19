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
}
