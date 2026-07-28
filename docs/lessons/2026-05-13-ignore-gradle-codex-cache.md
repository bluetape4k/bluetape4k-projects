# Ignore Gradle Codex Cache

## 배경

Local isolated Gradle run 이후 untracked `.gradle-codex/` directory가 생겼다.
이 directory는 Gradle wrapper/native cache를 담고 있었고 source나 build contract 변경은 아니었다.

## 결정

`.gradle-codex/`를 `.gradle/`과 함께 ignore해, future isolated Codex Gradle run이 repository를 dirty하게 만들지 않도록 한다.

## 결과

Local Codex-scoped Gradle cache를 사용한 뒤에도 repository를 clean하게 유지할 수 있다.

## 검증

- 삭제 전에 directory가 Gradle cache/native file만 담고 있음을 확인했다.
- Ignore rule을 추가하기 전에 `git status --short --branch`가 clean임을 확인했다.
