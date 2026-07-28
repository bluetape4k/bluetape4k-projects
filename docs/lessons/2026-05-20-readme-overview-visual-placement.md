# README Overview Visual Placement

## 배경

README overview 영역은 module identity를 빠르게 전달해야 하지만, diagram을 너무 늦게 배치하면
reader가 구조를 먼저 파악하기 어렵다.

## 결정

Overview text 근처에 핵심 visual을 배치하고, 긴 설명은 뒤로 미룬다. README 자체는 이번 localization
scope에서 제외되지만, 이 lesson 기록은 단일 언어 문서이므로 한국어로 남긴다.

## 결과

Reader가 README 초반에서 module ecosystem과 주요 관계를 먼저 볼 수 있게 되었다.

## 검증

- README link와 image path 확인.
- Diagram asset 존재 여부 확인.
- `git diff --check`.

## 향후 노트

Overview visual은 장식이 아니라 navigation aid다. 첫 화면에서 구조 이해를 돕지 못하면 위치와
크기를 다시 조정한다.
