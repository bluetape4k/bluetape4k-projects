# projects README 다이어그램 재작성 QA 회고

## 배경

`bluetape4k-projects`의 README 다이어그램 재작성은 단순한 스타일 교체가
아니었다. 루트 README, cache/data/infra/io/ktor/spring/testing/examples,
benchmark 문서까지 이어지는 장기 작업이었고, 사용자는 다음 조건을 반복해서
강조했다.

- 기존 다이어그램을 그대로 복제하지 말고 최신 README와 소스 코드를 다시 읽을 것
- sequence diagram과 chart처럼 이미 만족한 산출물은 불필요하게 건드리지 말 것
- Graphviz 기반 함수와 산출물은 제거하고 `$fireworks-tech-graph` 스타일을 따를 것
- 폰트는 `bluetape4k-diagram`에서 지정한 `Architects Daughter`와 `Comic Mono`를 쓸 것
- 모듈별로 하나씩 설계하고, SVG/PNG 생성 후 실제 PNG를 눈으로 검증할 것
- 카드 배치와 포트를 조정해서 연결선 교차, 0도 접속, card 관통, layer 침범을 줄일 것
- 화살촉 색상과 라인 색상, label 색상을 같은 의미 계열로 맞출 것
- 점선 관계의 빈 삼각형/open arrow 화살촉은 점선으로 보이면 안 되고 실선으로 보여야 함
- README 문구도 다이어그램 의미에 맞게 고치고, 한국어 README는 자연스럽게 교정할 것

초기 실패는 대부분 "대량 생산" 사고에서 나왔다. 많은 모듈을 한 번에 처리하려고
하면 다이어그램 제목, 카드 내용, 레이어 구조가 서로 비슷해지고, 실제 소스가
말하는 사용 흐름이나 class 관계가 약해진다. 렌더링이 성공하고 XML이 유효해도
사용자가 보기에는 "이게 무엇을 설명하는지 모르겠다"는 결과가 나온다.

## 결정과 발견

### 1. 다이어그램은 source-first로 하나씩 만들어야 한다

품질이 좋아진 지점은 항상 같은 순서를 따른 경우였다.

1. 해당 README에서 다이어그램의 역할을 확인한다.
2. 관련 소스만 다시 읽는다.
3. README의 문장이 architecture, class, flow, state, sequence 중 무엇을 요구하는지 고른다.
4. 기존 SVG를 참고하지 않고, source-backed card와 relationship을 새로 설계한다.
5. SVG와 PNG를 만든다.
6. XML, marker, endpoint, segment crossing, margin 같은 기계 검증을 돌린다.
7. PNG를 실제로 열어 텍스트, 선, 여백, 의미를 확인한다.
8. 문제 없을 때만 commit/push한다.

반대로 실패한 경우는 대부분 기존 다이어그램 형식을 유지한 채 카드만 바꾸거나,
여러 모듈을 같은 템플릿으로 찍어낸 경우였다. class structure라고 써 놓고
layer diagram을 만들거나, architecture title에 flow 내용을 넣거나, sequence에
작업 로그성 subtitle을 넣은 사례가 있었다.

### 2. "보기 좋음"보다 "의미가 맞음"이 먼저다

source-backed relationship을 라우팅하기 어렵다고 삭제하면 안 된다. 사용자가
지적한 여러 사례에서 문제는 관계 자체가 아니라 카드 배치와 포트 선택이었다.

- class diagram에서는 layer가 관계를 구속하면 inheritance/use line이 복잡해진다.
  class card는 레이어보다 관계가 읽히는 위치를 우선해야 한다.
- flow diagram에서는 수직/수평 정렬만 잘해도 불필요한 꺾임과 교차가 사라진다.
- observability 예제는 work-producing path와 scrape/query path를 분리해야 한다.
  Prometheus/Grafana/Actuator가 일을 트리거하는 것처럼 그리면 의미가 틀린다.
- id generator bit layout은 Snowflake처럼 bit width, high/low field, 설명 card 색상을
  같은 의미 계열로 맞춰야 독자가 bit 구성을 오해하지 않는다.
- benchmark 모듈은 README가 없으면 "없다"고 보고할 일이 아니라 README/README.ko와
  결과 chart를 만들어야 한다.

### 3. PNG가 최종 산출물이다

SVG가 올바르게 보이더라도 CairoSVG 변환 후 PNG가 달라질 수 있었다.

- `context-stroke` marker가 PNG에서 검정 화살촉처럼 렌더링될 수 있다.
- dashed line의 marker child가 `stroke-dasharray`를 상속해 빈 삼각형이나 open arrow가
  점선으로 보일 수 있다.
- open arrow가 target edge에 90도로 들어가지 않으면 V가 아니라 check icon처럼 보인다.
- sequence diagram의 arrowhead 크기는 SVG에서 적당해도 PNG에서는 작아질 수 있다.
- contact sheet는 전체 drift를 보는 데 유용하지만, 작은 label 겹침이나 화살촉 색상
  불일치를 놓칠 수 있다.

따라서 다이어그램 검증은 "XML parse 성공"이나 "SVG render 성공"으로 끝나지 않는다.
PNG를 열어 보고, 특히 수정한 diagram과 dense/high-risk diagram은 full size로 확인해야 한다.

### 4. marker 색상은 별도 QA 대상이다

라인 색상과 화살촉 색상이 다른 문제가 여러 examples 다이어그램에서 반복됐다.
원인은 공통 marker에 `fill="context-stroke"`를 두고 모든 colored route가 같은
marker를 공유한 것이다. SVG에서는 의도대로 보일 수 있지만, PNG에서는 검정 또는
기본색 화살촉으로 보였다.

앞으로는 다음을 실패 조건으로 본다.

- colored connector가 generic marker 하나를 공유한다.
- marker child에 명시적 `fill`/`stroke`가 없다.
- marker child 색상이 connector `stroke`와 다르다.
- dashed connector marker child에 `stroke-dasharray="none"` hard override가 없다.
- 한 diagram에서 발견된 marker failure를 관련 diagram set 전체로 검색하지 않는다.

### 5. routing 실패는 card 배치 실패인 경우가 많다

사용자가 반복해서 잡아낸 문제는 대부분 path string만 바꾸면 해결되지 않았다.

- 연결선이 card와 평행하게 붙거나 0도로 접속한다.
- arrowhead가 card 내부에 들어간다.
- layer label과 card가 너무 붙는다.
- layer 안의 card가 중앙 정렬되지 않는다.
- 좌우/상하 여백이 다르다.
- card 하나를 좌우 또는 상하로 옮기면 직선이 될 수 있는데 꺾은 선으로 둔다.
- 선 교차가 없어도 label이 다른 선 위에 올라가 어떤 선의 label인지 알 수 없다.

이런 문제는 "edge routing"보다 "layout redesign"으로 풀어야 한다. 특히 card 간격,
layer 크기, source/target port, sibling arrow corridor를 함께 봐야 한다.

## 결과

작업 중 재사용 가능한 규칙은 `bluetape4k-diagram` skill로 승격했다.

- Graphviz 금지와 `$fireworks-tech-graph` 우선 사용
- `Architects Daughter` / `Comic Mono` 폰트 고정
- CairoSVG CLI로 SVG→PNG 변환
- PNG를 authoritative artifact로 보는 규칙
- dashed arrowhead solid stroke 규칙
- class diagram UML 관계 규칙
- sequence diagram best-practices 스타일과 alt 영역 반투명 규칙
- layer orientation 선택 규칙
- card 배치로 connector 단순화를 먼저 시도하는 규칙
- examples observability/election/watchdog/id-generator 시나리오별 의미 규칙
- marker color parity audit 규칙

특히 마지막 examples pass에서는 `examples-ktor-idgenerator`,
`examples-ktor-observability`, `examples-spring-boot-observability` 계열의
`context-stroke` marker를 explicit per-color marker로 바꾸고, Ktor observability
architecture의 `/metrics` card를 routes 근처로 옮겨 불필요한 교차를 없앴다.

## 검증

이번 계열 작업에서 효과가 있었던 검증 스택은 다음과 같다.

- `repo-status`로 작업 전후 clean 상태 확인
- README image link 확인
- SVG XML parse
- `context-stroke` 잔존 검색
- connector stroke와 marker child fill/stroke 비교
- dashed connector의 marker child `stroke-dasharray:none` 확인
- orthogonal path segment crossing 검사
- endpoint가 실제 target boundary에 붙는지 확인
- CairoSVG CLI 렌더링:
  `~/.local/bin/cairosvg <diagram>.svg -o <diagram>.png -s 2`
- contact sheet로 전체 drift 확인
- 수정한 PNG와 dense/high-risk PNG는 개별 full-size 확인
- `git diff --check`
- module 또는 coherent scope 단위 commit/push

단, contact sheet와 segment crossing check만으로는 충분하지 않다. contact sheet는
검은 화살촉, 작은 점선 화살촉, label-line overlap을 놓칠 수 있고, segment crossing
check는 card 내부 관통, layer text 충돌, 여백 불균형, 의미 오류를 잡지 못한다.

## 향후 지침

다음 agents는 projects README/diagram 작업에서 아래 원칙을 먼저 적용해야 한다.

- "모든 모듈" 요청이어도 bulk generator로 찍지 말고 하나씩 source-first로 끝낸다.
- 다이어그램 종류는 README 문장에 맞춘다. `Class structure`는 class diagram이어야 하고,
  request order는 sequence/flow가 더 적합할 수 있다.
- 기존 다이어그램이 예쁘더라도 최신 소스와 README 계약을 다시 읽는다.
- connection이 지저분하면 path를 길게 돌리기 전에 card 위치, layer 방향, port를 바꾼다.
- 같은 source에서 여러 arrow가 나가면 출발점은 겹쳐도 되지만 target/corridor는 구분한다.
- dashed line의 arrowhead는 반드시 solid로 보이는지 PNG에서 확대 확인한다.
- colored line의 arrowhead와 label은 같은 semantic color family여야 한다.
- `context-stroke` marker를 쓰지 말고 explicit per-color marker를 기본값으로 삼는다.
- 한 다이어그램에서 오류가 나오면 파일 하나만 고치지 말고 같은 SVG pattern을 관련 set에서 검색한다.
- source-backed 관계를 삭제해서 routing 문제를 숨기지 않는다.
- 사용자-facing 다이어그램에 작업 로그, validation note, "generated by" 같은 내부 문구를 넣지 않는다.
- 문서만 바꿀 때는 Gradle build가 필수는 아니지만, README link, SVG/PNG, diff check는 필수다.

최종 보고는 "만들었다"가 아니라 "무엇을 검증했고 무엇을 검증하지 않았는지"까지 포함해야 한다.
특히 시각 QA는 사람이 보는 산출물이므로, 검증 증거에 PNG 직접 확인이 들어가야 한다.

## 2026-06-20 Follow-up: checklist audit must validate the validator

이번 follow-up에서는 skill checklist 자체가 늘어난 뒤 전체 `docs/images/readme-diagrams`
SVG 268개를 다시 검사했다. 첫 번째 감사 스크립트는 절대좌표 `L` 명령을 잘못 누적해
수직 connector를 diagonal connector로 오판했다. 이 실패는 다이어그램 실패가 아니라
검증 도구 실패였고, skill의 "rounded Q/curve path를 올바르게 파싱해야 한다" 규칙을
그대로 적용해야 하는 사례다.

앞으로 전체 다이어그램 감사를 할 때는 다음 순서를 지킨다.

- `M/L/H/V/Q/C/A` path parser가 절대좌표와 상대좌표를 분리해 처리하는지 먼저 확인한다.
- `Q` control point를 terminal endpoint처럼 세지 말고, 마지막 실제 `L/H/V` segment만
  terminal straight segment로 본다.
- sharp `L/H/V` bend를 `Q` rounded corner로 바꾼 뒤에도 마지막 bend 이후 직선 구간이
  충분한지 다시 검사한다.
- marker 색상 감사는 connector `stroke`와 marker child의 visible `fill`/`stroke`를
  비교해야 하며, 하나의 marker를 여러 connector 색상에 공유하지 않는다.
- dashed connector의 line은 dashed여도 arrowhead는 SVG와 PNG 모두에서 실선이어야 한다.
