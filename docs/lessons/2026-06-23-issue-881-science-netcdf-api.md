# 이슈 #881 Science NetCDF API migration

issue #881은 deprecated NetCDF-Java bean property를 통한 metadata access 때문에
`NetCdfCatalogService`에서 Java deprecation warning이 나는 문제를 추적했다.

- `Variable.getAttributes()`, Kotlin에서는 `v.attributes`로 노출된다.
- `NetcdfFile.getDimensions()`, Kotlin에서는 `nc.dimensions`로 노출된다.

## 결정

현재 source가 가리키는 NetCDF-Java 5.9.1 replacement API를 사용한다.

- variable attribute는 `Variable.attributes()`에서 가져온다.
- root-file dimension은 `nc.rootGroup.getDimensions()`에서 가져온다.

service는 같은 bluetape4k domain shape를 계속 쓴다.

- `NetCdfVariableInfo.attributes`
- `NetCdfFileRecord.dimensions`
- `NetCdfFileRecord.globalAttrs`

repository, schema, import, Gradle 10 cleanup behavior는 바꾸지 않았다.

## 교훈

- Kotlin bean-property syntax는 deprecated Java getter를 숨길 수 있다. Java library가
  getter를 deprecate하면 명시적인 method call 또는 의도한 API 이름을 드러내는 local
  helper를 우선한다.
- `NetcdfFile.getDimensions()`는 NetCDF-Java가 권장하지 않는 global view다. 현재
  registration behavior에서는 root group dimension이 nested group을 재귀하지 않고 기존
  root-file metadata shape를 보존한다.
- 단순 source grep은 deprecated Kotlin property form의 재도입을 잡는 데 유용하다.

## 검증

- `./gradlew :bluetape4k-science:compileKotlin --warning-mode all`
- `./gradlew :bluetape4k-science:compileTestKotlin --warning-mode all`
- `./gradlew :bluetape4k-science:test --tests '*NetCdfCatalogServiceTest' --tests '*NetCdfTableTest'`
  37 tests로 통과했다.
- `rg "\.attributes\b|\.dimensions\b" utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
  match를 반환하지 않았다.
- `git diff --check`가 통과했다.
