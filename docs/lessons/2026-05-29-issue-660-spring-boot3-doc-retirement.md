# 이슈 660: Spring Boot 3 documentation retirement

## 배경

repository는 이제 Spring Boot 4.x를 유일한 current support line으로 제시한다. 하지만
search에서는 historical plan, spec, security review, changelog entry의 Spring Boot 3
reference가 여전히 노출됐다.

## 결정

historical evidence는 그대로 보존하되, current-facing README file과 archive entrypoint에
active support boundary를 명시한다.

## 결과

- root `README.md`와 `README.ko.md`는 이제 Spring Boot 4.x only를 명시한다.
- `docs/superpowers/README.md`는 spec, plan, research note를 historical internal
  artifact로 표시한다.
- `docs/security-review/README.md`는 security review file을 point-in-time evidence로
  표시한다.

## 검증

- root README file이 이미 Spring Boot 4.x를 사용하고 있음을 확인하고, 두 언어 모두에
  retired-line clarification을 추가했다.
- repo-local `AGENTS.md`가 이미 Spring Boot 4.x module과 active `spring-boot3/*` line이
  없음을 명시하고 있음을 확인했다.

## 향후 지침

파일이 current-facing documentation으로 다시 publish되는 경우가 아니라면 historical
Spring Boot 3 reference를 rewrite하지 않는다. broad regex replacement보다 local archive
note 또는 directory-level context를 우선한다.
