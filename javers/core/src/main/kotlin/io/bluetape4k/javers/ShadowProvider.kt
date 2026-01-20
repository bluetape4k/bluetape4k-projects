package io.bluetape4k.javers

import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.metamodel.type.TypeMapper
import org.javers.shadow.ShadowFactory
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * [ShadowFactory]를 제공하는 Provider 입니다.
 */
object ShadowProvider: KLogging() {

    private val typeMappers = ConcurrentHashMap<Javers, TypeMapper>()
    private val shadowFactories = ConcurrentHashMap<Javers, ShadowFactory>()

    /**
     * 지정한 `javers`의 내부 내용을 기반으로 [ShadowFactory]를 제공합니다.
     */
    fun getShadowFactory(javers: Javers): ShadowFactory {
        return shadowFactories.computeIfAbsent(javers) {
            ShadowFactory(javers.jsonConverter, getTypeMapper(javers))
        }
    }

    /**
     * [Javers] 내부의 [TypeMapper]를 Reflection을 통해 제공합니다.
     *
     * @param javers [Javers] 인스턴스
     * @return
     */
    private fun getTypeMapper(javers: Javers): TypeMapper {
        return typeMappers.computeIfAbsent(javers) {
            val field: Field = javers.javaClass.declaredFields.find { it.name == "typeMapper" }!!
            field.isAccessible = true
            field.get(javers) as TypeMapper
        }
    }
}
