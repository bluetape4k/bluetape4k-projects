# Sequence diagram rendering QA

## 배경

README diagram refresh는 `docs/images/readme-diagrams` 아래의 모든 sequence SVG/PNG를
Spring Boot Cassandra best-practices style로 표준화했다. visual pass는 단순 XML 또는
SVG source error로 드러나지 않는 renderer-specific failure를 여러 개 노출했다.

- text용 CSS selector가 `rect.participant`에도 match해 participant box가 어두워질 수 있었다.
- shared `.label` style이 text와 pill shape 양쪽에 적용되어 PNG의 call label이 흐려질 수 있었다.
- diagram이 old class를 재사용하거나 label group 순서를 바꾸면 numbered badge, label text,
  message line, arrowhead 색상이 서로 drift될 수 있었다.
- raw SVG에서는 arrowhead가 괜찮아 보여도 CairoSVG conversion 후 너무 작아질 수 있었다.
- `?` 같은 placeholder number badge가 broad style pass 뒤에도 남을 수 있었다.
- contact-sheet thumbnail은 dense-label defect를 숨길 수 있으므로 risky diagram은 개별 PNG
  inspection이 계속 필요했다.

## 결정과 발견

rendered PNG를 authoritative README artifact로 취급한다. SVG source check는 필요하지만
sequence diagram에서는 충분하지 않다.

sequence diagram에서는 각 message row를 하나의 단위로 검증해야 한다.

- message path stroke가 message color를 정의한다.
- marker arrowhead는 path stroke와 같은 색을 사용해야 한다.
- call label pill outline, badge circle, label text는 같은 message color를 사용해야 한다.
- badge number text는 흰색이어야 한다.
- label shape와 label text는 CSS가 text를 shape처럼 style하거나 pill 아래에 숨기게 만드는
  class를 공유하면 안 된다.
- participant/header rectangle은 명시적인 light fill을 가져야 하며 text-only selector style을
  상속하면 안 된다.
- dashed `alt` 또는 return line도 PNG conversion 후 readable arrowhead가 필요하다.

같은 규칙은 dashed relationship을 가진 class/UML diagram에도 적용된다. dashed line이
hollow triangle 또는 open arrowhead를 점선, broken, undersized, check-like 형태로 render하게
만들면 안 된다. marker-based arrowhead가 PNG에서 `stroke-dasharray`를 상속하면 marker child
path에 `stroke-dasharray="none"`과 `style="stroke-dasharray:none"`을 hard override한다.
그래도 실패하면 arrowhead를 solid stroke를 가진 direct `polygon` 또는 `polyline` geometry로
그리고 final segment를 target edge에 수직으로 route한다.

## 결과

sequence diagram refresh는 text와 shape selector를 분리하고, 명시적인 light participant
rectangle style을 추가하며, PNG-safe fixed-size sequence arrow marker를 사용하고, 가까운
XML order만이 아니라 message number 기준으로 message color를 동기화해 고쳤다.

이 lesson을 남기게 한 구체 regression은
`examples-spring-boot-observability-spring-boot-demo-sequence-01`이었다. `.participant`
style 범위가 너무 넓어 participant box가 PNG에서 어두운 filled box로 render됐다. fix는
text styling을 `text.participant`로 제한하고 `rect.participant`에는 white fill과 명시적
stroke를 설정하는 것이었다.

## 검증

최종 sequence diagram pass는 다음 verification stack을 사용했다.

- 모든 sequence SVG file을 XML parse한다.
- `>?</text>` 같은 placeholder badge를 거부한다.
- numbered message badge 수를 marker-ended message path와 대조한다.
- 모든 marker color가 message path stroke와 일치하는지 확인한다.
- 모든 numbered badge가 white number text를 쓰는지 확인한다.
- raw SVG source order만이 아니라 message number에서 message color로 badge, label, line,
  arrowhead color를 확인한다.
- 모든 sequence SVG를 다음 명령으로 PNG render한다.
  `~/.local/bin/cairosvg <diagram>.svg -o <diagram>.png -s 2`.
- 전체 sequence set의 contact sheet를 생성하고 검사한다.
- label이 많거나 fallback path가 있거나 이전에 participant box가 깨졌던 dense/high-risk
  PNG는 개별로 연다.
- `git diff --check`를 실행한다.

## 향후 지침

SVG inspection 또는 render success만으로 sequence diagram completion을 주장하지 않는다.
CairoSVG가 marker/text의 apparent size, dash behavior, readability를 바꿀 수 있으므로 항상
PNG를 검사한다.

arrowhead 또는 label rule 하나를 바꿀 때는 commit 전에 diagram set 전체에서 동등한 SVG
pattern을 검색한다. failure mode는 보통 systemic하다. diagram 하나만 보고됐더라도 old
class, shared selector, dashed marker inheritance, marker scaling rule이 여러 파일에 나타날
수 있다.

sequence diagram에는 안정적인 message schema를 우선한다.

- message line: explicit `stroke`, fixed `marker-end`, rounded line join.
- marker: fixed PNG size가 중요하면 `markerUnits="userSpaceOnUse"`.
- label pill: `labelPill` 같은 rect-specific class.
- label text: text-specific class 또는 explicit fill.
- badge circle: message color와 같은 fill/stroke.
- badge number: explicit white fill.
- participant text와 participant rectangle style: selector 분리.

dashed UML/class relationship은 commit 전에 dedicated dashed-arrowhead audit을 실행한다.
marker head가 있는 dashed line, hollow/open marker definition, standalone triangle path,
direct arrowhead geometry를 검색한다. 그 다음 rendered PNG를 확대해 raw SVG가 맞아
보이더라도 dashed, broken, tiny, check-like arrowhead는 거부한다.

contact sheet는 broad sweep일 뿐이다. contact sheet에서 dense area, long fallback branch,
tiny label, recently edited arrowhead가 보이면 해당 PNG를 full size로 열고 확인한 뒤
accept한다.
