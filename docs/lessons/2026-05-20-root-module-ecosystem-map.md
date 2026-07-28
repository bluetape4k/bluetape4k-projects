# Root Module Ecosystem Map

## 배경

Root README의 module list만으로는 bluetape4k module group 사이의 관계를 파악하기 어려웠다.

## 결정

Root module ecosystem을 하나의 map으로 표현하되, source-verified module group과 Gradle project
registration 결과를 기준으로 한다.

## 결과

Ecosystem map은 core, io, data, infra, cache, ktor, spring-boot, testing, utils, virtualthread,
examples group의 역할과 관계를 빠르게 보여준다.

## 검증

- `settings.gradle.kts` auto-registration과 module group 대조.
- Generated README asset link 확인.
- `git diff --check`.

## 향후 노트

Module map은 marketing graphic이 아니라 navigation artifact다. 새 module group이 생기면 source
registration과 함께 diagram도 갱신한다.
