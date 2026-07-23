# Cassandra 학습 경로 문서 복구 교훈

## 결론

canonical Projects manual 변경과 versioned Site snapshot 변경은 한 작업처럼 보여도 병합 순서를 분리해야 한다. 먼저 Projects의 최신 `develop` 기반 PR을 병합하고, 그 merge commit만 Site snapshot의 source로 사용한다.

## 근거

- 복구된 Projects branch는 clean했지만 `origin/develop`보다 61커밋 뒤였다.
- 현재 Site의 Cassandra 1.11 landing에는 학습 경로를 설명하는 새 한영 문구가 없었다.
- 기존 계획의 Site 완료 진술만으로 현재 snapshot의 내용·source provenance를 증명할 수 없었다.

## 적용 규칙

1. 오래된 canonical 문서 branch는 PR 전에 최신 base로 rebase하고 manual validator와 manifest check를 다시 실행한다.
2. Site snapshot은 canonical Projects PR이 병합된 뒤에만 refresh한다.
3. Site PR에는 Projects merge SHA, immutable release commit, 생성된 한영 landing의 문구·anchor 검증을 함께 남긴다.

이 순서는 snapshot이 로컬 branch나 이미 사라진 feature ref를 가리키는 상황을 막고, 독자가 보는 versioned 문서와 canonical source의 추적 가능성을 유지한다.
