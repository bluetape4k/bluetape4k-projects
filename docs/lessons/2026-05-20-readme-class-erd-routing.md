# README Class/ERD Routing

## 배경

README diagram 변환 중 class diagram과 ERD는 일반 flowchart layout으로 처리하면 구조가 망가질 수 있었다.

## 결정

Diagram type을 먼저 판별하고, class/ERD에는 전용 renderer와 layout rule을 적용한다. Flowchart용
후처리를 무조건 적용하지 않는다.

## 결과

Class hierarchy와 entity relationship diagram이 README에서 의도한 구조를 유지했다.

## 검증

- README diagram block type 분류 확인.
- 생성된 asset link 확인.
- `git diff --check`.

## 향후 가이드

README diagram 작업은 content type routing을 먼저 확정한 뒤 render한다. 단일 renderer로 모든
Mermaid dialect를 처리하려고 하지 않는다.
