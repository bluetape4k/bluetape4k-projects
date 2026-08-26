# Issue #563 구현 계획

## 범위

upstream `bluetape4k-projects`에서 Java 21 호환 virtualthread API와 Java 25
core utility의 package ownership을 분리한다. graph 저장소의 후속 검증 PR은
이 지원 PR의 artifact 경계를 기준으로 적층한다.

## 단계

1. API 네 개 source와 test를 `.api` 하위 패키지로 이동한다.
2. core source에 API 타입 import를 명시하고 downstream 모듈·예제·문서의
   API import를 갱신한다.
3. JDK21/JDK25 ServiceLoader 파일명, provider import와 module metadata를
   갱신한다.
4. API package boundary test를 추가해 legacy resource가 다시 생기지 않는지
   고정한다.
5. API/JDK21/JDK25/core test·Detekt를 실행하고 생성 JAR의 module/package 및
   `java --validate-modules`를 확인한다.
6. 한국어 명세·7-Tier review·lesson과 migration README/CHANGELOG/WIP를
   기록하고 upstream 지원 PR을 만든다. merge는 graph train의 최종 승인
   단계까지 보류한다.

## 비범위

- core utility를 API module로 이동
- compatibility bridge 또는 shading 추가
- graph 의존성 좌표 변경
- release/publication 또는 upstream merge

## 완료 조건

P0/P1 review finding이 없고, targeted/full test와 module validation이
통과하며, 지원 PR의 exact head와 graph 후속 train의 base SHA가 기록되어야
한다.
