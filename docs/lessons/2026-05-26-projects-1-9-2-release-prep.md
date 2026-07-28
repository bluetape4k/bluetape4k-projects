# 2026-05-26 projects 1.9.2 release prep

## 배경

`bluetape4k-projects` 1.9.2는 IO HTTP performance와 documentation lane의 활성
patch release line이다. milestone은 완료됐지만, tag 전에 release metadata에 날짜가
있는 changelog section이 필요했다.

## 결정

`release/1.9.2`에서 release metadata를 준비하고, `baseVersion=1.9.2`와
`snapshotVersion=`은 그대로 둔다. 또한 release range의 trailing whitespace를
정리해 `git diff --check`를 깨끗한 preflight gate로 사용할 수 있게 한다.

## 결과

release prep branch에는 changelog, lesson, whitespace cleanup 변경만 포함된다.
stable publication은 prep이 merge되고 최종 `develop` commit에 대한 snapshot
validation을 갱신한 뒤에만 진행해야 한다.

## 검증

- `git diff --check`
- `./gradlew help --refresh-dependencies --no-daemon --no-configuration-cache --no-build-cache`

## 향후 가드

대상 changelog section이 없거나 release range가 `git diff --check`에 실패하면
stable release를 tag하지 않는다. publish 전에 먼저 고친다.
