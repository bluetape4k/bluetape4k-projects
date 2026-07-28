# 이슈 662: Module documentation drift checklist

## 배경

module lifecycle 변경은 Gradle registration, README locale pair, CI filter,
Nightly/examples workflow, BOM/catalog publication, agent reference 사이에서 drift될 수
있다.

## 결정

`docs/process/` 아래에 contributor-facing checklist를 만들고 module group reference에서
연결한다. 상세 process guidance는 transient agent instruction에만 두지 않는다.

## 결과

- `docs/process/module-documentation-checklist.md`를 추가했다.
- `.codex/references/module-groups.md`에서 checklist를 연결했다.
- module group reference를 현재 Spring Boot 4.x-only `spring-boot/` group으로
  업데이트하고, 오래된 Spring Boot path를 historical로 표시했다.

## 검증

- markdown whitespace를 `git diff --check`로 확인했다.
- checklist link target이 존재함을 확인했다.
- 현재 Spring Boot group wording이 더 이상 active `spring-boot3/*` 또는
  `spring-boot4/*` group을 나열하지 않음을 확인했다.

## 향후 지침

모든 module 추가, 이름 변경, 이동, 제거, 분할, repository promotion은 PR body에
checklist evidence를 포함해야 한다.
