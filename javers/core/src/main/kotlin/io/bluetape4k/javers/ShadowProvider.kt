package io.bluetape4k.javers

import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.metamodel.type.TypeMapper
import org.javers.shadow.ShadowFactory
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * [Javers] 인스턴스로부터 [ShadowFactory]를 생성·캐싱하여 제공하는 싱글턴 Provider.
 *
 * ## 동작/계약
 * - [Javers] 인스턴스별로 [ShadowFactory]를 [ConcurrentHashMap]에 캐싱한다
 * - 내부 [TypeMapper]는 Reflection으로 추출한다
 *
 * ```kotlin
 * val factory = ShadowProvider.getShadowFactory(javers)
 * val shadow = factory.createShadow(snapshot, metadata, null)
 * ```
 */
object ShadowProvider: KLogging() {

    private val typeMappers = ConcurrentHashMap<Javers, TypeMapper>()
    private val shadowFactories = ConcurrentHashMap<Javers, ShadowFactory>()

    /**
     * 지정한 [javers]에 대응하는 [ShadowFactory]를 반환한다.
     *
     * ## 동작/계약
     * - 동일한 [Javers] 인스턴스에 대해 항상 같은 [ShadowFactory]를 반환한다
     * - 최초 호출 시 [Javers] 내부 [TypeMapper]를 Reflection으로 추출하여 생성한다
     */
    fun getShadowFactory(javers: Javers): ShadowFactory {
        return shadowFactories.computeIfAbsent(javers) {
            ShadowFactory(javers.jsonConverter, getTypeMapper(javers))
        }
    }

    /**
     * [Javers] 내부의 [TypeMapper]를 Reflection을 통해 추출한다.
     */
    private fun getTypeMapper(javers: Javers): TypeMapper {
        return typeMappers.computeIfAbsent(javers) {
            val field: Field = javers.javaClass.declaredFields.find { it.name == "typeMapper" }!!
            field.isAccessible = true
            field.get(javers) as TypeMapper
        }
    }
}
