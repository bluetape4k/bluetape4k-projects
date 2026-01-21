package io.bluetape4k.junit5.random

import io.bluetape4k.logging.KLogging
import io.github.benas.randombeans.api.EnhancedRandom
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.util.stream.Stream

/**
 * `@RandomizedTest` annotation이 적용된 테스트 클래스나 메소드에
 * `@RandomValue` annotation이 적용된 필드나 메소드 인자에 랜덤 값을 주입해주는 Extension 입니다.
 *
 * ```
 * @RandomizedTest
 * class TestClass {
 *      @RandomValue
 *      private lateinit var text:String
 *
 *      @Test
 *      fun `test with random value`(@RandomValue text:String) {
 *          // text is random string
 *      }
 * }
 * ```
 */
class RandomExtension: TestInstancePostProcessor, ParameterResolver {

    companion object: KLogging() {

        private val randomizer: EnhancedRandom by lazy(LazyThreadSafetyMode.NONE) { DefaultEnhancedRandom }

        private fun resolve(targetType: Class<*>, annotation: RandomValue): Any = when {
            targetType.isAssignableFrom(Set::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList().toSet()

            targetType.isAssignableFrom(List::class.java) || targetType.isAssignableFrom(Collection::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(Stream::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes)

            targetType.isAssignableFrom(Sequence::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(Iterator::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).iterator()

            targetType.isAssignableFrom(Iterable::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(String::class.java) ->
                randomizer.nextObject(String::class.java, *annotation.excludes).toString()
            // Fakers.randomString(2, 256)

            else ->
                randomizer.nextObject(targetType, *annotation.excludes)
        }
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        testInstance.javaClass.declaredFields.forEach { field ->
            field.getAnnotation(RandomValue::class.java)?.let { annotation ->
                field.isAccessible = true
                val randomObj = resolve(field.type, annotation)
                field[testInstance] = randomObj
            }
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.getAnnotation(RandomValue::class.java) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return with(parameterContext.parameter) {
            resolve(type, getAnnotation(RandomValue::class.java))
        }
    }
}
