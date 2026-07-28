# Issue 832 검토 - findBean failure boundaries

## Scope

- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensions.kt`
- `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensionsTest.kt`

## Review Notes

- `findBean` now maps only absent beans to `null`.
- `NoUniqueBeanDefinitionException` is explicitly rethrown before catching
  `NoSuchBeanDefinitionException`, preserving duplicate-bean ambiguity.
- Bean creation failures from class-based, named, and constructor-argument
  lookups now propagate as Spring failures instead of optional lookup misses.
- Existing missing-bean tests still cover the intended `null` behavior.
- Spring 7.0.8 bytecode confirms `NoUniqueBeanDefinitionException extends
  NoSuchBeanDefinitionException`, so the catch order is required.

## Verification

- RED: `BeanFactoryExtensionsTest` failed because current `findBean` swallowed
  `BeanCreationException` and `NoUniqueBeanDefinitionException`.
- GREEN: `BeanFactoryExtensionsTest` passed with 17 tests.
- Code review: native `code-reviewer` returned APPROVE with 0 blocking
  findings. Residual gap: no direct `BeanNotOfRequiredTypeException` assertion,
  but the implementation does not catch that exception family.
- `javap -classpath spring-beans-7.0.8.jar org.springframework.beans.factory.NoUniqueBeanDefinitionException`
- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.beans.BeanFactoryExtensionsTest' --no-build-cache`
- `./gradlew :bluetape4k-spring-boot-core:compileKotlin :bluetape4k-spring-boot-core:compileTestKotlin :bluetape4k-spring-boot-core:test --no-build-cache`
- `git diff --check`
