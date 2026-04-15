package io.bluetape4k.spring.beans

import io.bluetape4k.support.assertNotBlank
import io.bluetape4k.utils.KotlinDelegates
import org.springframework.beans.BeanInstantiationException
import org.springframework.beans.BeanUtils
import org.springframework.core.KotlinDetector
import org.springframework.core.MethodParameter
import java.beans.PropertyDescriptor
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * 수신 클래스의 기본 생성자를 사용해 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.instantiateClass] 호출입니다.
 * - 생성 실패 시 Spring 예외를 전파합니다.
 *
 * ```kotlin
 * val instance = MyType::class.java.instantiateClass()
 * // instance is MyType
 * ```
 */
fun <T: Any> Class<T>.instantiateClass(): T = BeanUtils.instantiateClass(this)

/**
 * 생성자와 인자로 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - Kotlin 타입이면 [KotlinDelegates.instantiateClass], 아니면 [BeanUtils.instantiateClass]를 사용합니다.
 * - 생성 과정 예외는 [BeanInstantiationException]으로 감싸서 던집니다.
 *
 * ```kotlin
 * val ctor = MyType::class.java.getDeclaredConstructor(String::class.java)
 * val instance = ctor.instantiateClass("value")
 * // instance is MyType
 * ```
 */
fun <T: Any> Constructor<T>.instantiateClass(vararg args: Any?): T =
    try {
        when {
            KotlinDetector.isKotlinType(this.declaringClass) -> {
                KotlinDelegates.instantiateClass(this, *args)!!
            }
            else -> {
                BeanUtils.instantiateClass(this, *args)
            }
        }
    } catch (e: Exception) {
        throw BeanInstantiationException(this, "Fail to instantiate", e)
    }

/**
 * 수신 클래스로 인스턴스를 생성한 뒤 지정 타입으로 반환합니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.instantiateClass]의 `assignableTo` 오버로드를 호출합니다.
 * - 타입이 맞지 않으면 Spring 예외를 전파합니다.
 *
 * ```kotlin
 * val bean = ImplType::class.java.instantiateClass(BaseType::class.java)
 * // bean is BaseType
 * ```
 */
fun <T: Any> Class<*>.instantiateClass(assignableTo: Class<T>): T = BeanUtils.instantiateClass(this, assignableTo)

/**
 * 공개 메서드에서 이름과 시그니처가 일치하는 메서드를 찾습니다.
 *
 * ## 동작/계약
 * - [methodName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.findMethod] 호출입니다.
 *
 * ```kotlin
 * val method = MyType::class.java.findMethod("execute")
 * // method?.name == "execute"
 * ```
 */
fun Class<*>.findMethod(
    methodName: String,
    vararg paramTypes: Class<*>,
): Method? {
    methodName.assertNotBlank("methodName")
    return BeanUtils.findMethod(this, methodName, *paramTypes)
}

/**
 * 선언 메서드에서 이름과 시그니처가 일치하는 메서드를 찾습니다.
 *
 * ## 동작/계약
 * - [methodName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.findDeclaredMethod] 호출입니다.
 *
 * ```kotlin
 * val method = MyType::class.java.findDeclaredMethod("execute")
 * // method?.name == "execute"
 * ```
 */
fun Class<*>.findDeclaredMethod(
    methodName: String,
    vararg paramTypes: Class<*>,
): Method? {
    methodName.assertNotBlank("methodName")
    return BeanUtils.findDeclaredMethod(this, methodName, *paramTypes)
}

/**
 * 지정 이름의 공개 메서드 중 파라미터 수가 최소인 메서드를 찾습니다.
 *
 * ## 동작/계약
 * - [methodName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.findMethodWithMinimalParameters] 호출입니다.
 *
 * ```kotlin
 * val method = MyType::class.java.findMethodWithMinimalParameters("execute")
 * // method?.name == "execute"
 * ```
 */
fun Class<*>.findMethodWithMinimalParameters(methodName: String): Method? {
    methodName.assertNotBlank("methodName")
    return BeanUtils.findMethodWithMinimalParameters(this, methodName)
}

/**
 * 지정 이름의 선언 메서드 중 파라미터 수가 최소인 메서드를 찾습니다.
 *
 * ## 동작/계약
 * - [methodName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.findDeclaredMethodWithMinimalParameters] 호출입니다.
 *
 * ```kotlin
 * val method = MyType::class.java.findDeclaredMethodWithMinimalParameters("execute")
 * // method?.name == "execute"
 * ```
 */
fun Class<*>.findDeclaredMethodWithMinimalParameters(methodName: String): Method? {
    methodName.assertNotBlank("methodName")
    return BeanUtils.findDeclaredMethodWithMinimalParameters(this, methodName)
}

/**
 * 메서드 배열에서 지정 이름의 메서드 중 파라미터 수가 최소인 메서드를 찾습니다.
 *
 * ## 동작/계약
 * - [methodName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.findMethodWithMinimalParameters] 배열 오버로드 호출입니다.
 *
 * ```kotlin
 * val method = MyType::class.java.methods.findMethodWithMinimalParameters("execute")
 * // method?.name == "execute"
 * ```
 */
fun Array<out Method>.findMethodWithMinimalParameters(methodName: String): Method? {
    methodName.assertNotBlank("methodName")
    return BeanUtils.findMethodWithMinimalParameters(this, methodName)
}

/**
 * 시그니처 문자열에 해당하는 메서드를 해석합니다.
 *
 * ## 동작/계약
 * - 시그니처 파싱/조회는 [BeanUtils.resolveSignature]에 위임합니다.
 * - 해석 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val method = MyType::class.java.resolveSignature("execute(java.lang.String)")
 * // method?.name == "execute"
 * ```
 */
fun Class<*>.resolveSignature(signature: String): Method? = BeanUtils.resolveSignature(signature, this)

/**
 * 수신 클래스의 모든 JavaBeans [PropertyDescriptor]를 반환합니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.getPropertyDescriptors] 호출입니다.
 * - 반환 배열에는 읽기/쓰기 가능한 프로퍼티 기술자가 포함됩니다.
 *
 * ```kotlin
 * val descriptors = MyType::class.java.getPropertyDescriptors()
 * // descriptors.isNotEmpty() == true
 * ```
 */
fun Class<*>.getPropertyDescriptors(): Array<PropertyDescriptor> = BeanUtils.getPropertyDescriptors(this)

/**
 * 지정 이름의 JavaBeans [PropertyDescriptor]를 반환합니다.
 *
 * ## 동작/계약
 * - [propertyName]이 비어 있으면 `assertNotBlank`에 의해 예외가 발생합니다.
 * - 구현은 [BeanUtils.getPropertyDescriptor] 호출입니다.
 *
 * ```kotlin
 * val descriptor = MyType::class.java.getPropertyDescriptor("name")
 * // descriptor?.name == "name"
 * ```
 */
fun Class<*>.getPropertyDescriptor(propertyName: String): PropertyDescriptor? {
    propertyName.assertNotBlank("requireName")
    return BeanUtils.getPropertyDescriptor(this, propertyName)
}

/**
 * 메서드로부터 대응되는 JavaBeans [PropertyDescriptor]를 찾습니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.findPropertyForMethod] 호출입니다.
 * - 대응 프로퍼티가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val descriptor = method.findPropertyDescriptor()
 * // descriptor == null || descriptor.name.isNotEmpty()
 * ```
 */
fun Method.findPropertyDescriptor(): PropertyDescriptor? = BeanUtils.findPropertyForMethod(this)

/**
 * 메서드와 대상 클래스로 대응되는 JavaBeans [PropertyDescriptor]를 찾습니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.findPropertyForMethod]의 클래스 지정 오버로드 호출입니다.
 * - 대응 프로퍼티가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val descriptor = method.findPropertyDescriptor(MyType::class.java)
 * // descriptor == null || descriptor.name.isNotEmpty()
 * ```
 */
fun Method.findPropertyDescriptor(clazz: Class<*>): PropertyDescriptor? = BeanUtils.findPropertyForMethod(this, clazz)

/**
 * 프로퍼티 쓰기 메서드의 [MethodParameter]를 반환합니다.
 *
 * ## 동작/계약
 * - 구현은 [BeanUtils.getWriteMethodParameter] 호출입니다.
 * - 쓰기 메서드가 없는 경우 Spring 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val parameter = descriptor.getWriteMethodParamter()
 * // parameter.parameterType != null
 * ```
 */
fun PropertyDescriptor.getWriteMethodParamter(): MethodParameter = BeanUtils.getWriteMethodParameter(this)

/**
 * 타입이 simple property인지 확인합니다.
 *
 * ## 동작/계약
 * - simple value type 또는 그 배열이면 `true`를 반환합니다.
 * - 구현은 [BeanUtils.isSimpleProperty] 호출입니다.
 *
 * ```kotlin
 * val simple = String::class.java.isSimpleProperty()
 * // simple == true
 * ```
 */
fun Class<*>.isSimpleProperty(): Boolean = BeanUtils.isSimpleProperty(this)

/**
 * 타입이 simple value type인지 확인합니다.
 *
 * ## 동작/계약
 * - primitive/문자열/숫자 등 Spring이 정의한 단순 값 타입이면 `true`를 반환합니다.
 * - 구현은 [BeanUtils.isSimpleValueType] 호출입니다.
 *
 * ```kotlin
 * val simple = Int::class.javaObjectType.isSimpleValueType()
 * // simple == true
 * ```
 */
fun Class<*>.isSimpleValueType(): Boolean = BeanUtils.isSimpleValueType(this)

/**
 * 소스 빈의 프로퍼티를 대상 빈으로 복사합니다.
 *
 * ## 동작/계약
 * - [ignoreProperties]에 지정한 프로퍼티는 복사하지 않습니다.
 * - 구현은 [BeanUtils.copyProperties] 호출입니다.
 *
 * ```kotlin
 * source.copyProperties(target, "id")
 * // id를 제외한 공통 프로퍼티가 target에 반영
 * ```
 */
fun Any.copyProperties(
    target: Any,
    vararg ignoreProperties: String,
) {
    BeanUtils.copyProperties(this, target, *ignoreProperties)
}

/**
 * 소스 빈의 프로퍼티를 대상 빈으로 복사하되 대상 타입 범위를 제한합니다.
 *
 * ## 동작/계약
 * - [editable] 타입에 선언된 프로퍼티만 복사 대상으로 사용합니다.
 * - 구현은 [BeanUtils.copyProperties]의 `editable` 오버로드 호출입니다.
 *
 * ```kotlin
 * source.copyProperties(target, BaseType::class.java)
 * // BaseType 범위의 프로퍼티만 복사
 * ```
 */
fun Any.copyProperties(
    target: Any,
    editable: Class<*>,
) {
    BeanUtils.copyProperties(this, target, editable)
}
