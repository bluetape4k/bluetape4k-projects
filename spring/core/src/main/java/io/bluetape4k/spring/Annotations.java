package io.bluetape4k.spring;

import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * Annotation을 위한 Utility class
 * <p>
 * Kotlin에서 {@link AnnotationUtils#findAnnotation(Class, Class)},
 * {@link AnnotationUtils#findAnnotation(Method, Class)}를 호출하려면 ambigous type error이 발생합니다.
 * 이런 경우 annotation 관련 호출은 Java로 구현해서 이 문제를 피한다
 */
public class Annotations {

    private Annotations() {
    }

    /**
     * 지정한 수형에 적용된 annotation을 찾습니다.
     *
     * @param clazz          대상 수형
     * @param annotationType 찾고자하는 annotation 수형
     * @return 찾은 annotation, 없으면 null 반환
     */
    @Nullable
    public static <A extends Annotation> A findAnnotation(
            final Class<?> clazz,
            final Class<A> annotationType
    ) {
        return AnnotationUtils.findAnnotation(clazz, annotationType);
    }

    /**
     * Kotlin에서 {@link AnnotationUtils#findAnnotation(Method, Class)}와
     * {@link AnnotationUtils#findAnnotation(AnnotatedElement, Class)} 를 구분을 못하고 ambigous type error가 발생한다.
     * 이 것을 피하기 위해 Method를 인자로 받는 함수만 따로 Java로 정의하여 제공합니다.
     *
     * @param method         대상 method
     * @param annotationType 찾고자하는 annotation 수형
     * @return 찾은 annotation, 없으면 null 반환
     */
    @Nullable
    public static <A extends Annotation> A findAnnotation(
            final Method method,
            final Class<A> annotationType
    ) {
        return AnnotationUtils.findAnnotation(method, annotationType);
    }
}
