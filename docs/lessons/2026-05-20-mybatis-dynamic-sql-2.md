# MyBatis Dynamic SQL 2

## 배경

MyBatis Dynamic SQL 2.0은 Vert.x SQL client integration이 사용하던 independent where rendering API를
변경했다.

## 결정

제거된 1.x overload 주변에 compatibility shim을 유지하지 않고, 기존 Vert.x rendering bridge를 2.0 API에
맞게 조정한다.

## 결과

- `WhereModel.renderForVertx()`는 이제 `RenderingContext`를 통해 render하고
  `Optional<FragmentAndParameters>`를 반환한다.
- Insert model extension generic은 2.0 signature에 맞도록 `T : Any`로 제한한다.
- Kotlin count/delete DSL wrapper는 MyBatis Dynamic SQL 2.0이 요구하는 package-level DSL type을 사용한다.
- 관련 Dependabot PR을 중앙 upgrade batch에 접은 뒤 AWS SDK Java, AWS SDK Kotlin, Fory Kotlin을
  central catalog에서 materialize했다.
- Fory 0.17은 single-size `buildThreadSafeForyPool(int)` builder API만 유지하므로 local serializer는
  기존 max-pool size를 pool size로 보존한다.
- Timefold catalog alias 중 2.x에 더 이상 없는 항목은 materialized catalog에서 제거했다.

## 검증

- `./gradlew :bluetape4k-vertx:compileTestKotlin --no-daemon`
- `./gradlew :bluetape4k-io:compileTestKotlin --no-daemon`
- `./gradlew :bluetape4k-io:test --no-daemon`
- `./gradlew build -x test --parallel --no-daemon`

기존 unrelated deprecation warning은 test와 infrastructure code에 남아 있었다.
