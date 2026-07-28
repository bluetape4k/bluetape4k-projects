# 이슈 #768 Sequence diagram validation

## 배경

Sequence README asset은 rendered structure가 유효해도 validation에 실패했다. Validator가
generator-specific marker ID와 하나의 exact label class에 의존했고, 몇몇 diagram에는
실제 text overflow와 branch-clarity defect도 있었다.

## 결정

- Fixed-size filled marker와 visible numbered pill label은 특정 generator 이름이 아니라 SVG structure로 검증한다.
- Generic message pill은 footer element가 아니라 message label로 다룬다.
- 실패한 각 asset을 개별적으로 고치고 render한다.
- Branch outcome이 하나의 sequence frame을 공유하면 시각적으로 구분되게 유지한다.

## 결과

Sequence family는 이제 validator failure가 0개다. 이미 유효한 sequence semantics를
다시 쓰지 않고 5개 SVG/PNG pair를 수정했다.

## 검증

- README diagram validator: `total=268 failed=137`, sequence failures `0`
- Sequence style audit: `PASS sequence_files=5`
- Geometry, endpoint, and mixed-corner audits: zero failures
- Connector audit: PASS for four assets
- `cache-cache-core-sequence-02`: fallback invariant `messages=6`,
  `direct_solid_heads=6`, `participants=4`
- CairoSVG render and PNG visual inspection completed for every changed asset

## 향후 지침

Structural validation을 만족시키기 위해 전체 diagram family를 regenerate하지 않는다.
먼저 validator false positive와 visible defect를 분리한 뒤 asset 하나씩 편집하고
검사한다. Generic audit가 `connectors=0`을 보고하면 모든 visible message에 rendered
arrowhead가 있다는 explicit invariant가 필요하다.
