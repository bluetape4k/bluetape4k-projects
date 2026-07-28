# Release Catalog Guard

## 배경

AWS 0.3.0 release는 공유 release workflow 위험을 드러냈다. operational catalog
override가 stable release에서 checked-in `settings.gradle.kts` default와 다른
`bluetape4k-dependencies` catalog를 사용하게 만들 수 있었다.

## 결정

stable tag release는 checked-in catalog default를 사용한다. manual dispatch는 명시적
`catalogRef` override를 사용할 수 있고, 그 다음 repository variable을 operational
fallback으로 사용한다.

## 결과

release workflow는 선택된 catalog source를 log로 남기고 Maven Central publish 전에
필수 catalog alias를 검증한다.

## 검증

`actionlint`를 실행하고, catalog selection branch를 local에서 검증하며, 현재 release
catalog에 필수 alias가 포함되어 있는지 확인한다.

## 향후 지침

repository catalog variable은 release train source of truth가 아니라 manual release
override로 취급한다.
