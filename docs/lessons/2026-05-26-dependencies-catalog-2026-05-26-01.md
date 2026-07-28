# Dependencies Catalog 2026-05-26-01

## 배경

`bluetape4k-dependencies`가 security dependency line을 중앙화한
`catalog/2026-05-26-01`을 발행했다.

## 결정

공유 external library version을 downstream repository에 직접 고정하지 않고,
downstream 기본 `bluetape4kDependenciesCatalogRef`를 새 catalog tag로 갱신한다.

## 결과

이 repository는 이제 기본적으로 `catalog/2026-05-26-01`에서 공유 dependency
version을 해석한다.

## 검증

`settings.gradle.kts`의 catalog ref를 확인했다.

## 향후 메모

공유 external library는 먼저 `bluetape4k-dependencies`를 업데이트하고 catalog를
tag한 다음, downstream repository를 해당 tag로 이동한다.
