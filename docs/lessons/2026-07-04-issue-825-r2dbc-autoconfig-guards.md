# 교훈: 이슈 #825 R2DBC auto-configuration guard

## 배경

`R2dbcClientAutoConfiguration`은 `DatabaseClient`로만 guard되었지만,
auto-configured bean method는 `R2dbcEntityTemplate`과 `MappingR2dbcConverter`도
사용했다. 이 Spring Data R2DBC type은 published module에서 compile-only다.

## 교훈

Spring Boot auto-configuration class는 bean method signature에 나타나는 모든
compile-only type을 guard해야 한다. Condition이 short-circuit되기 전에 class-loading
failure가 발생하지 않도록 guard에는 string 기반 `@ConditionalOnClass` name을 사용한다.

## 결과

R2DBC auto-configuration은 이제 모든 Spring R2DBC signature type을 확인하고,
user-defined `R2dbcClient` bean이 있으면 back off한다.

## 향후 방지책

Compile-only parameter가 있는 auto-configured bean을 추가할 때는 missing classpath
behavior와 custom bean backoff를 모두 검증하는 `ApplicationContextRunner` test를
추가한다.

## 검증

- `:bluetape4k-r2dbc:compileKotlin` and `:bluetape4k-r2dbc:compileTestKotlin`
  passed.
- `:bluetape4k-r2dbc:test` passed with 188 tests.
- `:bluetape4k-r2dbc:koverXmlReport` generated the XML coverage report.
- `git diff --check` passed.
