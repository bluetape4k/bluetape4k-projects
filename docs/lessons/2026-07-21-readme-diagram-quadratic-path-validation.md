# README 다이어그램의 `Q` 경로와 validator 경계

## 배경

Issue #768의 root README overview 다이어그램은 연결선 끝점과 세로 여백
검증에 실패했다. 생성기는 다이어그램 규칙에 맞는 둥근 직교 연결선을 만들기
위해 SVG `Q` 명령을 사용하지만, `scripts/validate-readme-diagram-assets.mjs`의
경로 해석기는 첫 `Q` 이후의 좌표를 끝점 계산에 포함하지 않는다.

## 원인

validator는 `M`, `L`, `H`, `V`, `C`를 끝점 목록으로 해석하지만 `Q`는
토큰으로만 인식하고 실제 좌표를 소비하지 않는다. 이 때문에 `Q` 앞의 직선
구간 끝이 연결선의 최종 끝점처럼 취급될 수 있다. 그 임시 끝점이 다른 카드
경계에서 64px 이내에 있으면 실제 PNG에서는 정상인 연결선도
`loose connector endpoints`로 보고된다.

## 판단

관계 메타데이터를 바꿔 검사를 우회하지 않는다. 생성기와 validator는 다음
계약을 함께 지킨다.

- 시각적 연결선은 다이어그램 계약에 맞춰 `Q` 기반의 둥근 직교 경로를 유지한다.
- 관계 의미는 repository 표준인 `data-from`, `data-to`와 `aria-label`로 보존한다.
- validator는 절대 `Q`와 상대 `q`의 control point를 건너뛴 뒤 실제 endpoint를
  좌표 목록에 추가한다.
- SVG만 직접 고치지 않고 canonical generator와 SVG/PNG를 함께 갱신한다.
- 동일 asset을 쓰던 상위 생성 진입점은 전용 generator를 호출하도록 위임한다.

`Q/q` 파싱을 추가한 뒤에는 전체 validator 실패 수를 수정 전 baseline과 비교해야
한다. 기존 asset의 숨겨진 실패가 새로 드러나면 parser 변경과 asset 수정을 같은
green 결과로 오해하지 말고 각각 추적한다.

## 결과

root overview 대상 행은 실패 3종에서 0건으로 줄었고, `Q/q` parser 회귀 테스트는
수정 전 RED와 수정 후 GREEN을 증명했다. 전체 validator 실패 수는 `135`에서
`134`로 감소했으며 parser 변경 전후의 134 baseline도 유지됐다. 연결선 7개와
카드 8개는 카드 침범, 교차, 공유 구간, 대각선 구간 없이 유지됐다.

## 검증

- `node --check scripts/generate-root-readme-overview-01.mjs`
- `node scripts/generate-root-readme-overview-01.mjs`
- `node --test scripts/validate-readme-diagram-assets_test.mjs`
- `node scripts/validate-readme-diagram-assets.mjs`
- `diagram-connector-audit.py`
- `diagram-geometry-audit.py --fail-diagonal`
- `diagram-endpoint-audit.py`
- `diagram-mixed-corner-audit.py`
- `xmllint --noout` 및 `cairosvg -s 2`
- 생성 전후 SVG/PNG SHA-256 비교

## 향후 지침

`Q` 연결선을 포함한 README 다이어그램에서 끝점 실패가 발생하면 PNG 좌표만
보고 선을 직선이나 `C`로 바꾸거나 `data-from`/`data-to`를 제거하지 않는다.
먼저 validator가 실제로 소비한 마지막 좌표를 확인하고 parser 회귀 테스트와
전체 baseline 비교를 함께 수행한다. `C`의 control point는 geometry audit에서
대각선 구간으로 해석될 수 있으므로 둥근 직교 코너의 대체재로 사용하지 않는다.
