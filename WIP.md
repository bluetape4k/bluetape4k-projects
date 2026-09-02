# WIP - bluetape4k-projects

- 기준일: 2026-09-02 KST
- 최신 안정 버전: `2.0.0`
- 안정 tag commit: `8165a8989e0075e7c17c489bf3000bf41fef8232`
- 현재 개발선: `2.1.0-SNAPSHOT`
- 현재 milestone: `2.1.0`

## 현재 상태

`2.0.0` artifact와 GitHub Release 배포를 완료했다. `develop`은 minor upgrade인
`2.1.0` 개발선을 사용한다. Apache Ignite 2 runtime 지원 제거는 `2.0.0` 이후
지원 종료 정책에 따른 후속 정리이며, 이번 개발선을 major version으로 올리는
근거로 사용하지 않는다.

## 다음 개발선 규칙

- `gradle.properties`는 `baseVersion=2.1.0`, 빈 `snapshotVersion`을 유지한다.
- SNAPSHOT workflow가 실행할 때만 `-PsnapshotVersion=-SNAPSHOT`을 주입한다.
- 정식 배포의 tag commit과 다음 개발선의 mutable `develop` head를 혼용하지 않는다.
- 의존성 catalog SHA는 중앙 `bluetape4k-dependencies`의 다음 개발선이 병합된 뒤 한 번만 갱신한다.

## 추적

생태계 전체 후속 작업은 [bluetape4k-dependencies #235](https://github.com/bluetape4k/bluetape4k-dependencies/issues/235)에서 추적한다. 이 저장소의 신규 기능과 버그는 `2.1.0` milestone에서 관리한다.
