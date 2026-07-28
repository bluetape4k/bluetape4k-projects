# 2026-06-01 1.11.0 development 시작

## 배경

`bluetape4k-projects` `1.10.0`이 publish됐고 `bluetape4k-dependencies` `1.2.0`에
포함됐다.

## 결정

committed `baseVersion`을 `1.11.0`으로 이동하되 `snapshotVersion=`은 비워 둔다. 이렇게
release workflow가 snapshot qualifier를 명시적으로 주입할 수 있다.

## 결과

repository가 다음 minor development line을 시작할 준비가 됐다.

## 검증

- `gradle.properties`가 `baseVersion=1.11.0`을 사용한다.
- `snapshotVersion=`은 비어 있다.
