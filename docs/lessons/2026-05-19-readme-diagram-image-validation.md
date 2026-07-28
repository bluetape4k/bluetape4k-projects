# README Diagram Image Validation

## 배경

README Mermaid diagram을 pastel infographic PNG image로 교체하면서 matching SVG source를 보존했다.

## 결정

English-only diagram label, PNG README embed, 보존된 SVG asset, fixed-size Mermaid recolor 대신
content-driven dimension을 사용한다.

큰 label에는 `Architects Daughter`를 사용하고, detail label에는 renderer에서 가장 명확한
Comic-style fallback을 사용한다.

## 결과

Refined renderer는 fixed height constraint 없이 architecture, class, sequence, module-stack image를
생성했다. Grouped architecture diagram은 필요할 때 content-sized section과 masonry placement를 사용한다.

## 검증

- Full regeneration: `rendered=477`, `missing=[]`.
- README image links: `readmes=169`, `missing=0`, `svgLinks=0`.
- Asset counts: `png=415`, `svg=415`.
- Mermaid README blocks: `0`.
- Shape sanity check: `shapeCandidates=0`.
- Whitespace check: `git diff --check`.

## 향후 가이드

Link check만 의존하지 않는다. PR을 열기 전에 visual-shape check를 항상 실행하고 JUnit5,
Testcontainers, 넓은 class hierarchy, sequence diagram처럼 risk가 알려진 diagram을 inspect한다.
