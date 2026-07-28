# Source-Verified README Diagrams

## 배경

README diagram은 보기 좋아도 source와 맞지 않으면 유지보수 비용을 만든다.

## 결정

Diagram content는 source tree, Gradle registration, README link, generated asset을 대조해 검증한다.
추정으로 module 관계를 그리지 않는다.

## 검증

- Source file/module 존재 여부 확인.
- README link와 generated image asset 확인.
- `git diff --check`.

## 향후 가이드

README diagram을 수정할 때는 source verification을 먼저 수행한다. 확인하지 않은 관계는 diagram에
추가하지 않는다.
