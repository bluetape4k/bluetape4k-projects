# Core·Coroutines Manual First Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-core`와 `bluetape4k-coroutines`를 repository-owned chapter와 diagram을 갖춘 교과서형 매뉴얼로 만들고, site가 이를 검증 가능한 deterministic snapshot으로 게시하게 한다.

**Architecture:** `bluetape4k-projects/docs/manual`이 문서와 diagram의 유일한 기술 원본이다. Manifest schema v2가 module landing, bilingual chapter, paired asset inventory를 선언하고 Ruby validator가 source tree를 검증하며, `bluetape4k.github.io`의 Node sync가 chapter와 asset을 함께 변환·복사·digest한다. 콘텐츠는 현재 Kotlin source와 representative test를 먼저 확인한 뒤 Coroutines, Core 순서로 작성하고 blog는 마지막에 manual route와 canonical asset을 참조하도록 정렬한다.

**Tech
Stack:** Ruby 3 + Minitest + YAML, Node.js ESM + `node:test`, Astro/Starlight MDX, Markdown, Kotlin source/tests, SVG 1.1, CairoSVG, `xmllint`, Playwright/browser smoke verification

---

## 실행 경계와 저장소

- Projects source worktree: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals`
- Site snapshot worktree: `/Users/debop/work/bluetape4k/bluetape4k.github.io/.worktrees/feature-ecosystem-atlas-manual`
- Projects branch: `feature/all-module-manuals`
- Site branch: `feature/ecosystem-atlas-manual`
- Production Kotlin API와 runtime behavior는 변경하지 않는다.
- 각 task는 표시된 저장소에서 검증하고 Lore commit 하나로 닫는다.
- Projects source commit이 GitHub에 게시되기 전에는 site commit을 배포하거나 merge하지 않는다.
- Diagram 작업마다 `bluetape-diagram` skill을 다시 적용하고 SVG 한 개씩 검증·렌더·육안 검사한다.

## 파일 책임 지도

### `bluetape4k-projects`

- `scripts/manual/manual_contract.rb`: schema v2, chapter, asset, Markdown reference, orphan 검증의 단일 구현.
- `scripts/manual/validate_manuals_test.rb`: validator의 성공·실패 계약.
- `scripts/manual/test-fixtures/valid/**`: chapter와 paired asset이 포함된 최소 유효 fixture.
- `scripts/manual/export_manifest_test.rb`: optional nested manifest data가 JSON export에서 보존됨을 검증.
- `docs/manual/manifest.yaml`: Core·Coroutines chapter와 canonical asset inventory.
- `docs/manual/{ko,en}/modules/bluetape4k-coroutines/**`: Coroutines landing과 7개 chapter.
- `docs/manual/{ko,en}/modules/bluetape4k-core/**`: Core landing과 6개 chapter.
- `docs/manual/assets/coroutines/**`: Coroutines canonical SVG와 rendered PNG.
- `docs/manual/assets/core/**`: Core canonical SVG와 rendered PNG.

### `bluetape4k.github.io`

- `scripts/manual/lib/paths.mjs`: localized chapter destination과 public asset destination 계산.
- `scripts/manual/lib/frontmatter.mjs`: landing/chapter metadata 삽입, repository link와 manual asset URL 변환.
- `scripts/manual/sync-manual.mjs`: manifest inventory 기반 document/asset snapshot 생성과 stale tree 교체.
- `scripts/manual/validate-snapshot.mjs`: schema v2, file/digest/count consistency 검사.
- `tests/manual/{paths,frontmatter,snapshot,sync}.test.mjs`: transformation과 snapshot regression 계약.
- `src/content/docs/{ko/,}blog/*.mdx`: narrative는 유지하되 기술 계약과 diagram ownership을 manual로 정렬.
- `public/manual-assets/bluetape4k-projects/**`: sync가 생성하는 게시용 asset tree; 직접 편집하지 않는다.

## 공통 chapter frontmatter와 본문 계약

모든 신규 chapter는 다음 frontmatter를 사용한다. `manualId`와 `chapterId` 이외의 제목·설명은 언어별로 자연스럽게 작성한다.

```yaml
---
title: Lifecycle & Cancellation
description: Scope ownership, cancellation propagation, and deterministic shutdown
manualId: bluetape4k-coroutines
chapterId: lifecycle
---
```

본문은 주제에 맞게 다음 순서를 유지한다.

```markdown
# Lifecycle & Cancellation

## 해결할 문제
## Mental model
## 최소 API surface
## 완전한 예제
## 선택 기준
## 실패·취소·수명주기 계약
## 운영과 문제 진단
## Source와 representative test
## 이어 읽기와 runnable workshop
```

영문은 동일한 의미와 순서를 사용한다. 각 chapter의 코드 예제는 import, scope, cleanup을 포함해 독립 실행 가능한 형태로 제시하고, source/test 링크는 repository-relative path로 작성한다.

### Task 1: Manifest schema v2 validator를 TDD로 확장

**Files:**

- Modify: `scripts/manual/manual_contract.rb`
- Modify: `scripts/manual/validate_manuals_test.rb`
- Modify: `scripts/manual/export_manifest_test.rb`
- Modify: `scripts/manual/test-fixtures/valid/docs/manual/manifest.yaml`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/en/modules/sample/chapter-one.md`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/ko/modules/sample/chapter-one.md`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/assets/sample/model.svg`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/assets/sample/model.png`
- Modify: `scripts/manual/test-fixtures/missing-ko/docs/manual/manifest.yaml`

- [ ] **Step 1: 유효 fixture를 schema v2 chapter·asset 계약으로 확장한다**

`scripts/manual/test-fixtures/valid/docs/manual/manifest.yaml`의 module에 다음 값을 추가한다.

```yaml
schemaVersion: 2
modules:
  - id: sample
    gradlePath: ":sample"
    sourceDir: io/sample
    kind: library
    group: sample
    artifact: io.sample:sample
    en: en/modules/sample.md
    ko: ko/modules/sample.md
    sourcePaths: [io/sample/src/main]
    testPaths: [io/sample/src/test]
    workshops: []
    chapters:
      - id: chapter-one
        en: en/modules/sample/chapter-one.md
        ko: ko/modules/sample/chapter-one.md
    assets:
      - assets/sample/model.svg
      - assets/sample/model.png
```

두 chapter에는 각각 `manualId: sample`, `chapterId: chapter-one` frontmatter와 `![Model](../../../assets/sample/model.svg)`를 넣는다. SVG는 120×80 크기의 유효한 단일 diagram으로 만들고 PNG는 같은 SVG를 CairoSVG로 렌더한다.

- [ ] **Step 2: schema v2의 실패 계약을 먼저 테스트로 고정한다**

`scripts/manual/validate_manuals_test.rb`에 다음 test 이름과 assertion을 추가한다.

```ruby
def test_accepts_complete_bilingual_chapters_and_paired_assets
  assert_empty validator_for("valid").errors
end

def test_reports_duplicate_chapter_ids_and_frontmatter_mismatch
  with_fixture("valid") do |root|
    manifest = load_manifest(root)
    chapter = manifest["modules"].first["chapters"].first
    manifest["modules"].first["chapters"] << deep_copy(chapter)
    write_manifest(root, manifest)
    english = File.join(root, "docs/manual/en/modules/sample/chapter-one.md")
    File.write(english, File.read(english).sub("chapterId: chapter-one", "chapterId: wrong"))

    errors = validator(root).errors
    assert_includes errors, "sample: duplicate chapter id chapter-one"
    assert_includes errors, "sample/chapter-one: English chapterId must be chapter-one"
  end
end

def test_reports_missing_chapter_asset_pair_and_orphan_asset
  with_fixture("valid") do |root|
    FileUtils.rm(File.join(root, "docs/manual/ko/modules/sample/chapter-one.md"))
    FileUtils.rm(File.join(root, "docs/manual/assets/sample/model.png"))
    File.write(File.join(root, "docs/manual/assets/sample/orphan.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"/>")

    errors = validator(root).errors
    assert_includes errors, "sample/chapter-one: missing Korean document"
    assert_includes errors, "sample: missing paired asset assets/sample/model.png"
    assert_includes errors, "manual assets: orphan asset assets/sample/orphan.svg"
  end
end

def test_reports_unsafe_and_missing_manual_references
  with_fixture("valid") do |root|
    english = File.join(root, "docs/manual/en/modules/sample/chapter-one.md")
    File.write(english, File.read(english) + "\n![Escape](../../../../../../outside.png)\n[Missing](missing.md)\n")

    errors = validator(root).errors
    assert errors.any? { |error| error.include?("unsafe Markdown reference") }
    assert errors.any? { |error| error.include?("missing Markdown reference") }
  end
end
```

- [ ] **Step 3: 실패를 확인한다**

Run: `ruby scripts/manual/validate_manuals_test.rb`

Expected: FAIL. 현재 validator가 schema 2를 거부하고 chapter, asset, orphan, Markdown reference를 검사하지 않는 assertion이 보인다.

- [ ] **Step 4: validator를 최소 책임 단위로 구현한다**

`ManualDocs::Validator`에 다음 계약을 구현한다.

```ruby
SUPPORTED_SCHEMA_VERSION = 2
CHAPTER_FIELDS = %w[id en ko].freeze
MANUAL_ASSET_EXTENSIONS = %w[.svg .png].freeze

def validate_chapters(entry)
  chapters = entry.fetch("chapters", [])
  return ["#{entry_label(entry)}: chapters must be an array"] unless chapters.is_a?(Array)

  errors = duplicate_values(chapters, "id").map do |id|
    "#{entry_label(entry)}: duplicate chapter id #{id}"
  end
  chapters.each do |chapter|
    errors.concat(validate_chapter(entry, chapter))
  end
  errors
end

def validate_assets(entry)
  assets = entry.fetch("assets", [])
  return ["#{entry_label(entry)}: assets must be an array"] unless assets.is_a?(Array)

  assets.each_with_object([]) do |relative_path, errors|
    unless safe_relative_path?(relative_path) && relative_path.start_with?("assets/")
      errors << "#{entry_label(entry)}: unsafe asset path #{relative_path.inspect}"
      next
    end
    absolute_path = File.expand_path(relative_path, File.dirname(@manifest_path))
    errors << "#{entry_label(entry)}: missing asset #{relative_path}" unless File.file?(absolute_path)
  end.concat(validate_asset_pairs(entry, assets))
end
```

`validate_chapter`는 mapping 여부, `id/en/ko`, safe path, file 존재, `manualId`, `chapterId`를 검사한다. `validate_asset_pairs`는 `.svg` 또는 `.png`가 등록되면 동일 basename의 반대 확장자가 inventory와 filesystem에 모두 있는지 검사한다. `validate_markdown_references`는 fenced code block을 제거한 Markdown에서 inline link/image target을 추출하고 `http:`, `https:`, `mailto:`, `#` target을 건너뛴 뒤 현재 문서 기준으로 repository boundary와 file 존재를 검사한다. `validate_orphan_assets`는 `docs/manual/assets/**/*.{svg,png}`와 모든 module의 `assets` 합집합을 비교한다.

- [ ] **Step 5: exporter가 optional nested data를 보존하는 테스트를 추가한다**

`scripts/manual/export_manifest_test.rb` fixture module에 다음 값을 넣고 JSON parse 결과가 같음을 assertion한다.

```ruby
"chapters" => [
  { "id" => "chapter-one", "en" => "en/modules/sample/chapter-one.md", "ko" => "ko/modules/sample/chapter-one.md" },
],
"assets" => ["assets/sample/model.png", "assets/sample/model.svg"],
```

Run: `ruby scripts/manual/export_manifest_test.rb`

Expected: PASS. Exporter가 nested key를 제거하거나 path를 변형하지 않는다.

- [ ] **Step 6: 전체 manual script test를 통과시킨다**

Run: `ruby scripts/manual/validate_manuals_test.rb && ruby scripts/manual/export_manifest_test.rb && ruby scripts/manual/generate_manuals_test.rb`

Expected: 세 test process 모두 0 failures, 0 errors.

- [ ] **Step 7: validator 계약을 커밋한다**

```bash
git add scripts/manual
git commit -m "Validate chaptered manuals as one repository contract" \
  -m "Constraint: Existing modules may omit chapters and assets while Core and Coroutines adopt schema v2." \
  -m "Rejected: Site-only validation | repository docs must fail before publication." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: ruby scripts/manual/validate_manuals_test.rb; ruby scripts/manual/export_manifest_test.rb; ruby scripts/manual/generate_manuals_test.rb"
```

### Task 2: Core·Coroutines inventory와 chapter skeleton을 등록

**Files:**

- Modify: `docs/manual/manifest.yaml`
- Modify: `docs/manual/generated/manifest.json`
- Create: `docs/manual/{ko,en}/modules/bluetape4k-coroutines/{lifecycle,deferred,flow,subjects,structured-concurrency,operations,recipes}.md`
- Create: `docs/manual/{ko,en}/modules/bluetape4k-core/{validation,bounded-collections,encoding-data,time-ranges,concurrency-lifecycle,recipes}.md`

- [ ] **Step 1: Manifest에 정확한 chapter inventory를 추가한다**

Coroutines 순서는 `lifecycle`, `deferred`, `flow`, `subjects`, `structured-concurrency`, `operations`, `recipes`로 고정한다. Core 순서는 `validation`, `bounded-collections`, `encoding-data`, `time-ranges`, `concurrency-lifecycle`, `recipes`로 고정한다. 각 항목은 `id`, `en`, `ko`만 가진다.

- [ ] **Step 2: 26개 localized chapter file을 작성 계약으로 생성한다**

각 파일에 실제 localized title/description, 공통 9개 본문 section, 해당 chapter가 검증할 source/test path 목록을 넣는다. 빈 section이나 임시 문구는 두지 않는다. 초기 문장은 각 주제의 선택 결론을 명시한다. 예: Coroutines Flow chapter는 ordered 결과가 필요하면 `flow.async`, throughput 우선이면 `mapParallel`, `parallelism <= 1`이면 sequential map을 선택한다고 시작한다.

- [ ] **Step 3: 생성된 inventory와 frontmatter가 유효한지 확인한다**

Run: `ruby scripts/manual/validate_manuals.rb`

Expected: `Manual contract valid`와 module 수가 출력되고 exit 0.

- [ ] **Step 4: generated manifest를 갱신하고 drift가 없는지 확인한다**

Run: `ruby scripts/manual/export_manifest.rb && git diff --check`

Expected: `docs/manual/generated/manifest.json`에 schemaVersion 2와 두 module의 chapters가 나타나며 whitespace error가 없다.

- [ ] **Step 5: inventory와 chapter structure를 커밋한다**

```bash
git add docs/manual
git commit -m "Give Core and Coroutines stable manual chapter routes" \
  -m "Constraint: Existing module landing routes and all non-target modules remain compatible." \
  -m "Rejected: One long module page | chapter routes reduce reading and maintenance cost." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: ruby scripts/manual/validate_manuals.rb; ruby scripts/manual/export_manifest.rb; git diff --check"
```

### Task 3: Site sync가 chapter와 asset을 원자적으로 게시하도록 TDD 확장

**Repository:** `/Users/debop/work/bluetape4k/bluetape4k.github.io/.worktrees/feature-ecosystem-atlas-manual`

**Files:**

- Modify: `scripts/manual/lib/paths.mjs`
- Modify: `scripts/manual/lib/frontmatter.mjs`
- Modify: `scripts/manual/sync-manual.mjs`
- Modify: `scripts/manual/validate-snapshot.mjs`
- Create: `tests/manual/paths.test.mjs`
- Modify: `tests/manual/frontmatter.test.mjs`
- Modify: `tests/manual/snapshot.test.mjs`
- Modify: `tests/manual/sync.test.mjs`

- [ ] **Step 1: chapter destination과 asset destination test를 작성한다**

`tests/manual/paths.test.mjs`에 다음 계약을 고정한다.

```js
import assert from 'node:assert/strict';
import test from 'node:test';
import { assetDestinationFor, destinationFor } from '../../scripts/manual/lib/paths.mjs';

test('preserves nested chapter routes', () => {
  assert.equal(
    destinationFor('ko', 'ko/modules/bluetape4k-coroutines/lifecycle.md'),
    'src/content/docs/ko/manual/bluetape4k-projects/modules/bluetape4k-coroutines/lifecycle.md',
  );
});

test('publishes repository-owned manual assets under one stable namespace', () => {
  assert.equal(
    assetDestinationFor('bluetape4k-projects', 'assets/coroutines/scope-lifecycle.svg'),
    'public/manual-assets/bluetape4k-projects/coroutines/scope-lifecycle.svg',
  );
});
```

- [ ] **Step 2: chapter metadata와 asset URL rewrite test를 작성한다**

`tests/manual/frontmatter.test.mjs`에 `chapter: { id: 'lifecycle', en: 'en/modules/bluetape4k-coroutines/lifecycle.md', ko: 'ko/modules/bluetape4k-coroutines/lifecycle.md' }`를 넘겨 `manual.chapterId: "lifecycle"`가 삽입되고, `../../../assets/coroutines/scope-lifecycle.svg`가 `/manual-assets/bluetape4k-projects/coroutines/scope-lifecycle.svg`로 변환되는 assertion을 추가한다.

- [ ] **Step 3: snapshot count와 stale asset cleanup test를 작성한다**

`tests/manual/sync.test.mjs`에 temporary Git repository와 temporary site root를 만드는 helper를 추가한다. Fixture는 landing 2개 (en/ko), chapter 2개 (en/ko), SVG/PNG 1쌍을 manifest schema v2에 등록하고 initial commit을 만든다. 첫 sync 후 다음을 assertion한다.

```js
const first = await buildSnapshot({ source });
assert.equal(first.snapshot.documentFiles, 4);
assert.equal(first.snapshot.assetFiles, 2);
assert.equal(first.snapshot.contentFiles, 6);
await syncManual({ source, targetRoot });
const assetPath = path.join(targetRoot, 'public/manual-assets/bluetape4k-projects/sample/model.svg');
assert.match(await readFile(assetPath, 'utf8'), /<svg/);
```

그 다음 source에서 asset pair와 manifest 등록을 제거하고 fixture repository에 두 번째 commit을 만든 뒤 재-sync하여 `public/manual-assets/bluetape4k-projects/sample/model.{svg,png}`의 stale pair가 삭제되는지 확인한다. 기존 real-source test의 `186` 고정값은 다음 계산으로 교체한다.

```js
const expectedDocuments = 6 + manifest.modules.reduce(
  (total, module) => total + 2 + 2 * (module.chapters?.length ?? 0),
  0,
);
const expectedAssets = manifest.modules.reduce(
  (total, module) => total + (module.assets?.length ?? 0),
  0,
);
assert.equal(first.snapshot.documentFiles, expectedDocuments);
assert.equal(first.snapshot.assetFiles, expectedAssets);
```

- [ ] **Step 4: test가 현재 구현에서 실패하는지 확인한다**

Run: `node --test --test-name-pattern='manual|snapshot|frontmatter|paths' tests/manual/*.test.mjs`

Expected: FAIL. `assetDestinationFor` 미정의, chapter metadata 누락, asset count 누락 중 하나 이상이 보인다.

- [ ] **Step 5: manifest inventory를 기준으로 document와 asset entry를 만든다**

`buildSnapshot`에서 landing과 chapter를 모두 `byPath`에 넣고 asset은 manifest에 등록된 path만 읽는다. Normalized manifest의 각 chapter에도 localized `routes.en`/`routes.ko`를 추가한다. 반환 snapshot은 다음 shape를 사용한다.

```js
const snapshot = {
  repository,
  sourceCommit: commit,
  sourceDigest: digestEntries(sourceEntries),
  contentDigest: digestEntries(contentEntries),
  assetDigest: digestEntries(assetEntries),
  sourceFiles: sourceEntries.length,
  documentFiles: contentEntries.length,
  assetFiles: assetEntries.length,
  contentFiles: contentEntries.length + assetEntries.length,
};
```

`syncManual` write mode는 두 localized content tree와 `public/manual-assets/${repository}`를 temp tree에서 rename해 stale file을 제거한다. Test가 실제 worktree를 변경하지 않도록 `syncManual({ targetRoot = siteRoot, ...options })` 형태의 optional target root를 받는다. Check mode는 text와 binary를 모두 비교한다. PNG는 UTF-8 string으로 읽지 말고 `Buffer`로 digest와 equality를 계산한다.

`buildSnapshot`은 `{ contentEntries, assetEntries, snapshot }`을 반환한다. `contentEntries`에는 Markdown과 두 data JSON만, `assetEntries`에는 public SVG/PNG만 넣어 text transformation과 binary copy의 type을 섞지 않는다.

- [ ] **Step 6: frontmatter transformation을 landing/chapter 공통 함수로 유지한다**

`transformManual` 입력에 optional `chapter`를 추가하고 metadata에 chapter가 있을 때만 다음 line을 삽입한다.

```js
`  chapterId: ${yamlScalar(chapter.id)}`
```

Repository source link rewrite와 manual asset rewrite는 별도 named function으로 분리해 test한다. Asset target은 항상 `/manual-assets/${repository}/` 아래여야 한다.

- [ ] **Step 7: site manual test와 build를 통과시킨다**

Run: `node --test --test-name-pattern='manual|snapshot|frontmatter|paths' tests/manual/*.test.mjs && npm run build`

Expected: targeted Node tests PASS, Astro build exit 0, broken internal link나 missing asset error 없음.

- [ ] **Step 8: site sync 계약을 커밋한다**

```bash
git add scripts/manual tests/manual
git commit -m "Publish manual chapters and assets as one snapshot" \
  -m "Constraint: Binary assets and localized Markdown must share one source commit and stale-cleanup boundary." \
  -m "Rejected: Copying blog assets by hand | site output must remain reproducible." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: targeted manual Node tests; npm run build"
```

### Task 4: Coroutines canonical diagram inventory를 source 기준으로 재설계

**Repository:** Projects worktree

**Files:**

- Modify: `docs/manual/manifest.yaml`
- Create: `docs/manual/assets/coroutines/{module-foundation,scope-lifecycle,deferred-race-policy,ordered-parallel-flow,subject-contracts,structured-policies,observability-boundaries}.{svg,png}`

- [ ] **Step 1: 각 diagram의 source/test evidence 표를 먼저 작성한다**

`docs/manual/en/modules/bluetape4k-coroutines.md`와 한국어 landing의 Source section에 다음 대응을 기록한다.

```text
module-foundation -> public API groups and the seven chapter decision map
scope-lifecycle -> CloseableCoroutineScope.kt, DefaultCoroutineScope.kt, ThreadPoolCoroutineScope.kt
deferred-race-policy -> DeferredSupport.kt, StructuredConcurrency.kt and their tests
ordered-parallel-flow -> AsyncFlow.kt, mapParallel.kt and their tests
subject-contracts -> PublishSubject.kt, BehaviorSubject.kt, ReplaySubject.kt, MulticastSubject.kt, UnicastWorkSubject.kt
structured-policies -> StructuredConcurrency.kt and representative policy tests
observability-boundaries -> owned scope/channel lifecycle plus observability workshop
```

- [ ] **Step 2: 기존 blog diagram을 각 evidence와 비교해 유지할 정보만 선별한다**

Site의 `public/assets/bluetape4k-projects-part2-flow.*`, `bluetape4k-flow-extensions-*.{svg,png}`, `coroutine-observability-*.{svg,png}`를 참고하되 파일을 그대로 복사하지 않는다. 현재 source에 없는 API 이름, 무제한 concurrency 인상, cancellation을 error로 표현한 흐름은 제거한다.

- [ ] **Step 3: diagram을 한 쌍씩 생성·검증한다**

각 SVG마다 다음 명령을 한 asset씩 순서대로 실행하고, 각 PNG를 생성한 직후 full-size로 연다.

```bash
xmllint --noout docs/manual/assets/coroutines/scope-lifecycle.svg
cairosvg docs/manual/assets/coroutines/scope-lifecycle.svg \
  -o docs/manual/assets/coroutines/scope-lifecycle.png -s 2
xmllint --noout docs/manual/assets/coroutines/module-foundation.svg
cairosvg docs/manual/assets/coroutines/module-foundation.svg \
  -o docs/manual/assets/coroutines/module-foundation.png -s 2
xmllint --noout docs/manual/assets/coroutines/deferred-race-policy.svg
cairosvg docs/manual/assets/coroutines/deferred-race-policy.svg \
  -o docs/manual/assets/coroutines/deferred-race-policy.png -s 2
xmllint --noout docs/manual/assets/coroutines/ordered-parallel-flow.svg
cairosvg docs/manual/assets/coroutines/ordered-parallel-flow.svg \
  -o docs/manual/assets/coroutines/ordered-parallel-flow.png -s 2
xmllint --noout docs/manual/assets/coroutines/subject-contracts.svg
cairosvg docs/manual/assets/coroutines/subject-contracts.svg \
  -o docs/manual/assets/coroutines/subject-contracts.png -s 2
xmllint --noout docs/manual/assets/coroutines/structured-policies.svg
cairosvg docs/manual/assets/coroutines/structured-policies.svg \
  -o docs/manual/assets/coroutines/structured-policies.png -s 2
xmllint --noout docs/manual/assets/coroutines/observability-boundaries.svg
cairosvg docs/manual/assets/coroutines/observability-boundaries.svg \
  -o docs/manual/assets/coroutines/observability-boundaries.png -s 2
```

Expected: 모든 `xmllint`가 exit 0이고 PNG가 SVG의 2배 raster size로 생성된다. Full-size 검사에서 잘린 text, 겹침, 흐릿한 화살표, 의미 없는 장식이 없다.

- [ ] **Step 4: Manifest에 14개 asset path를 basename별 SVG/PNG 순서로 등록한다**

Run: `ruby scripts/manual/validate_manuals.rb && git diff --check`

Expected: orphan/pair/reference error가 없고 exit 0.

- [ ] **Step 5: Coroutines diagram source를 커밋한다**

```bash
git add docs/manual/assets/coroutines docs/manual/manifest.yaml docs/manual/generated/manifest.json docs/manual/{ko,en}/modules/bluetape4k-coroutines.md
git commit -m "Make Coroutines diagrams part of the manual source" \
  -m "Constraint: Every rendered PNG is derived from a repository-owned SVG and current implementation evidence." \
  -m "Rejected: Promoting blog images unchanged | blog diagrams are not authoritative." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: xmllint for 7 SVGs; CairoSVG render for 7 PNGs; ruby scripts/manual/validate_manuals.rb"
```

### Task 5: Coroutines lifecycle·deferred chapter를 교과서 수준으로 완성

**Files:**

- Modify: `docs/manual/{ko,en}/modules/bluetape4k-coroutines.md`
- Modify: `docs/manual/{ko,en}/modules/bluetape4k-coroutines/{lifecycle,deferred}.md`

- [ ] **Step 1: source와 representative test를 다시 읽고 사실 표를 작성한다**

Run:

```bash
rg -n "class DeferredValue|fun awaitAny|awaitAnyAndCancelOthers|firstSuccessTaskScope|CloseableCoroutineScope|ThreadPoolCoroutineScope|CancellationException" \
  bluetape4k/coroutines/src/main bluetape4k/coroutines/src/test
```

Expected: `DeferredValue`의 eager owned scope, `awaitAny`의 first-completion semantics, cancel-others variant, first-success policy, closeable scope의 cleanup test 위치를 확보한다.

- [ ] **Step 2: lifecycle chapter에 ownership decision tree와 완전한 예제를 작성한다**

반드시 caller-owned scope와 component-owned scope를 비교하고, `ThreadPoolCoroutineScope`를 `use`/`close`하는 예제, `CancellationException` 재전파, timeout이 wait만 제한하는 경우와 underlying I/O를 취소하는 경우를 구분한다. `scope-lifecycle` diagram을 본문 mental model 바로 뒤에 배치한다.

- [ ] **Step 3: deferred chapter에 race policy를 의미별로 분리한다**

`DeferredValue.await()`를 기본으로 설명하고 deprecated blocking `value`는 migration note로만 둔다. `awaitAny`, `awaitAnyAndCancelOthers`, `firstSuccessTaskScope`, `zip`의 winner, loser, failure, cancellation을 표로 비교한다. Fastest replica 예제는 loser를 유지하는 버전과 취소하는 버전을 모두 완전한 코드로 제시한다.

- [ ] **Step 4: 양 언어 parity와 source link를 검증한다**

Run: `ruby scripts/manual/validate_manuals.rb && rg -n "blog" docs/manual/{ko,en}/modules/bluetape4k-coroutines/{lifecycle,deferred}.md`

Expected: validator exit 0. Blog가 설명을 대신하는 문장 없음; blog 링크가 있다면 추가 읽기 역할뿐이다.

- [ ] **Step 5: lifecycle·deferred chapter를 커밋한다**

```bash
git add docs/manual/{ko,en}/modules/bluetape4k-coroutines*
git commit -m "Teach Coroutines ownership and race semantics from source" \
  -m "Constraint: The manual must distinguish first completion, first success, and loser cancellation precisely." \
  -m "Rejected: Linking to blog explanations | chapters must stand alone." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: ruby scripts/manual/validate_manuals.rb; source and representative tests cross-checked"
```

### Task 6: Coroutines Flow·Subject chapter를 완성

**Files:**

- Modify: `docs/manual/{ko,en}/modules/bluetape4k-coroutines/{flow,subjects}.md`

- [ ] **Step 1: ordering, capacity, terminal contract를 source/test에서 확인한다**

Run:

```bash
rg -n "fun .*mapParallel|parallelism|async\(|awaitCollector|complete\(|error\(|replay|buffer|capacity" \
  bluetape4k/coroutines/src/main bluetape4k/coroutines/src/test
```

Expected: `flow.async` ordered emission, `mapParallel` parallel result ordering, `parallelism <= 1`, Subject collector startup과 terminal call test를 찾는다.

- [ ] **Step 2: Flow chapter를 선택 지도 중심으로 작성한다**

Sequential map, ordered async, unordered bounded parallel 세 경로를 latency, ordering, downstream capacity로 비교한다. Parallel enrichment, race/fallback, chunk/window 예제 중 현재 library API와 workshop이 증명하는 것만 포함한다. `ordered-parallel-flow` diagram을 비교 표 앞에 둔다.

- [ ] **Step 3: Subject chapter를 event contract 중심으로 작성한다**

Publish, Behavior, Replay, Multicast, UnicastWork의 delivery, retained state/history, fan-out/work-sharing, capacity, terminal semantics를 한 표로 비교한다. `awaitCollector()`가 필요한 startup ordering과 terminal 이후 호출이 무시되는 계약을 코드와 test link로 설명한다. `subject-contracts` diagram을 사용한다.

- [ ] **Step 4: runnable workshop link를 실제 module path와 대조한다**

Run:

```bash
test -d /Users/debop/work/bluetape4k/bluetape4k-workshop/kotlin/flow-extensions-parallel-enrichment
test -d /Users/debop/work/bluetape4k/bluetape4k-workshop/kotlin/flow-extensions-race-fallback
test -d /Users/debop/work/bluetape4k/bluetape4k-workshop/kotlin/flow-extensions-subject-bridge
ruby scripts/manual/validate_manuals.rb
```

Expected: 세 workshop directory와 manual validation 모두 성공.

- [ ] **Step 5: Flow·Subject chapter를 커밋한다**

```bash
git add docs/manual/{ko,en}/modules/bluetape4k-coroutines/{flow,subjects}.md
git commit -m "Document Coroutines stream contracts by ordering and delivery" \
  -m "Constraint: Concurrency guidance is bounded by downstream capacity and current tests." \
  -m "Rejected: API catalog prose | readers need contract-based selection." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: ruby scripts/manual/validate_manuals.rb; workshop paths verified"
```

### Task 7: Coroutines structured·operations·recipes와 landing을 완성

**Files:**

- Modify: `docs/manual/{ko,en}/modules/bluetape4k-coroutines.md`
- Modify: `docs/manual/{ko,en}/modules/bluetape4k-coroutines/{structured-concurrency,operations,recipes}.md`

- [ ] **Step 1: structured policy chapter를 실패 의미로 조직한다**

`taskScope`, `firstSuccessTaskScope`, `supervisedTaskScope`를 fail-fast, first-success, partial-result로 연결한다. JDK structured task/virtual-thread bridge는 boundary와 cleanup 책임만 설명하고 Kotlin scope의 대체물로 표현하지 않는다. `structured-policies` diagram을 배치한다.

- [ ] **Step 2: operations chapter에 관찰 가능한 신호와 shutdown 순서를 적는다**

Active job, queue/buffer growth, latency, timeout, cancellation을 관측 항목으로 나누고 cancellation을 error span으로 기록하지 않는 규칙을 명시한다. Readiness와 request acceptance를 구분하며 owned scope, channel, dispatcher의 shutdown 순서를 설명한다. `observability-boundaries` diagram을 사용한다.

- [ ] **Step 3: recipes chapter를 완전한 작업 단위로 작성한다**

다음 5개 recipe를 import부터 cleanup까지 완전한 코드로 제공한다: 여러 suspend call 조합, fastest replica, first-success replica, ordered transform, throughput-first transform. Subject callback bridge와 aggregation/windowing은 대응 workshop을 실행 경로로 연결하되 핵심 contract를 chapter 안에 남긴다.

- [ ] **Step 4: landing을 chapter 선택 지도와 세 학습 경로로 다시 쓴다**

Landing은 API 상세를 반복하지 않고 초급, HTTP service, stream processing/operations 경로를 chapter link로 제시한다. 표준 Kotlin coroutine API만으로 충분한 경우를 첫 선택 규칙으로 둔다.

- [ ] **Step 5: Coroutines 전체 품질 gate를 실행한다**

Run:

```bash
ruby scripts/manual/validate_manuals.rb
rg -n "DeferredValue|awaitAny|mapParallel|awaitCollector|taskScope|firstSuccessTaskScope" docs/manual/{ko,en}/modules/bluetape4k-coroutines*
git diff --check
```

Expected: validator exit 0, 핵심 API가 양 언어의 적절한 chapter에 존재, whitespace error 없음.

- [ ] **Step 6: Coroutines manual을 커밋한다**

```bash
git add docs/manual/{ko,en}/modules/bluetape4k-coroutines*
git commit -m "Complete the Coroutines manual as an operational guide" \
  -m "Constraint: Readers must make lifecycle, failure, and throughput decisions without blog dependencies." \
  -m "Rejected: Repeating landing-page summaries | chapters own detailed contracts." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: ruby scripts/manual/validate_manuals.rb; git diff --check"
```

### Task 8: Coroutines snapshot을 site에 동기화하고 브라우저 검증

**Repository:** Site worktree

**Files:**

- Generated: `src/content/docs/manual/bluetape4k-projects/modules/bluetape4k-coroutines/**`
- Generated: `src/content/docs/ko/manual/bluetape4k-projects/modules/bluetape4k-coroutines/**`
- Generated: `public/manual-assets/bluetape4k-projects/coroutines/**`
- Generated: `src/data/manual/bluetape4k-projects.{manifest,snapshot}.json`

- [ ] **Step 1: Projects source가 clean commit인지 확인한다**

Run: `git -C /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals status --short && git -C /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals rev-parse HEAD`

Expected: status output 없음, 40-character source commit 출력.

- [ ] **Step 2: Site snapshot을 write mode로 생성한다**

Run:

```bash
npm run sync:manual -- \
  --source /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals \
  --repository bluetape4k-projects
```

Expected: snapshot written message에 증가한 localized document count와 asset count가 출력된다.

- [ ] **Step 3: deterministic check, test, build를 실행한다**

Run:

```bash
npm run check:manual
BLUETAPE4K_PROJECTS_SOURCE=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals \
  node --test --test-name-pattern='manual|snapshot|frontmatter|paths' tests/manual/*.test.mjs
npm run build
```

Expected: snapshot matches, targeted tests PASS, Astro build exit 0.

- [ ] **Step 4: ko/en landing과 7개 chapter를 browser smoke test한다**

Local preview에서 `/ko/manual/bluetape4k-projects/modules/bluetape4k-coroutines/`와 `/manual/bluetape4k-projects/modules/bluetape4k-coroutines/`를 열고, 각 chapter navigation, code block, source link, 7개 diagram의 200 response와 mobile overflow를 확인한다.

- [ ] **Step 5: Coroutines site snapshot을 커밋한다**

```bash
git add src/content/docs/manual src/content/docs/ko/manual public/manual-assets src/data/manual
git commit -m "Publish the chaptered Coroutines manual snapshot" \
  -m "Constraint: Generated content and assets must identify one Projects source commit." \
  -m "Rejected: Partial chapter publication | navigation and diagrams must arrive together." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: manual check; targeted Node tests; Astro build; bilingual browser smoke"
```

### Task 9: Core canonical diagram inventory와 validation·collection chapter를 완성

**Repository:** Projects worktree

**Files:**

- Modify: `docs/manual/manifest.yaml`
- Create: `docs/manual/assets/core/{validation-boundary,bounded-collection-ordering,concurrent-reducer-capacity,shutdown-order}.{svg,png}`
- Modify: `docs/manual/{ko,en}/modules/bluetape4k-core/{validation,bounded-collections}.md`

- [ ] **Step 1: Core 계약을 source/test에서 다시 확인한다**

Run:

```bash
rg -n "fun .*require|class BoundedStack|class RingBuffer|class ConcurrentReducer|class ShutdownQueue|capacity|evict|close" \
  bluetape4k/core/src/main bluetape4k/core/src/test
```

Expected: require helper의 receiver return/exception, stack newest-first, ring oldest-first, capacity eviction, reducer full/closed behavior, shutdown LIFO evidence를 확보한다.

- [ ] **Step 2: 4개 Core diagram을 한 쌍씩 생성·검증한다**

각 SVG는 다음 명령으로 한 asset씩 검사·렌더하고, 생성 직후 full-size PNG를 육안 검사한다.

```bash
xmllint --noout docs/manual/assets/core/validation-boundary.svg
cairosvg docs/manual/assets/core/validation-boundary.svg -o docs/manual/assets/core/validation-boundary.png -s 2
xmllint --noout docs/manual/assets/core/bounded-collection-ordering.svg
cairosvg docs/manual/assets/core/bounded-collection-ordering.svg -o docs/manual/assets/core/bounded-collection-ordering.png -s 2
xmllint --noout docs/manual/assets/core/concurrent-reducer-capacity.svg
cairosvg docs/manual/assets/core/concurrent-reducer-capacity.svg -o docs/manual/assets/core/concurrent-reducer-capacity.png -s 2
xmllint --noout docs/manual/assets/core/shutdown-order.svg
cairosvg docs/manual/assets/core/shutdown-order.svg -o docs/manual/assets/core/shutdown-order.png -s 2
```

`validation-boundary`는 public boundary와 internal invariant를, `bounded-collection-ordering`은 insertion/iteration/eviction을, `concurrent-reducer-capacity`는 running/queued/full/closed 상태를, `shutdown-order`는 LIFO cleanup을 표현한다.

- [ ] **Step 3: validation chapter를 경계 선택 중심으로 작성한다**

Kotlin `require/check`, bluetape `require*`, nullable/blank/collection 조건을 public input validation과 internal invariant로 나눈다. 반환 receiver를 이용한 fluent validation 예제와 정확한 exception 의미를 source/test link로 입증한다.

- [ ] **Step 4: bounded collections chapter를 ordering·capacity 중심으로 작성한다**

`BoundedStack`과 `RingBuffer`의 삽입, 조회 순서, overflow eviction을 같은 입력 sequence로 비교한다. Capacity가 memory limit이지 backpressure가 아님을 명시하고 concurrency가 필요하면 별도 primitive를 선택하게 한다.

- [ ] **Step 5: Core asset과 두 chapter를 검증·커밋한다**

Run: `ruby scripts/manual/export_manifest.rb && ruby scripts/manual/validate_manuals.rb && git diff --check`

Expected: schema/asset/chapter validation exit 0.

```bash
git add docs/manual
git commit -m "Explain Core boundaries and bounded state with verified diagrams" \
  -m "Constraint: Ordering and eviction claims must match representative tests." \
  -m "Rejected: Generic utility catalog | the manual teaches invariant selection." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: xmllint and CairoSVG for 4 diagrams; ruby scripts/manual/validate_manuals.rb"
```

### Task 10: Core encoding·time·concurrency·recipes와 landing을 완성

**Files:**

- Modify: `docs/manual/{ko,en}/modules/bluetape4k-core.md`
- Modify: `docs/manual/{ko,en}/modules/bluetape4k-core/{encoding-data,time-ranges,concurrency-lifecycle,recipes}.md`

- [ ] **Step 1: encoding과 time API surface를 source/test로 분류한다**

Run:

```bash
rg -n "Base64|Hex|encode|decode|Range|Period|Duration|Instant|LocalDate|ConcurrentReducer|ShutdownQueue" \
  bluetape4k/core/src/main bluetape4k/core/src/test
```

Expected: 실제 public helper와 test path 목록을 얻고, 존재하지 않는 범용 codec/time abstraction을 문서에 만들지 않는다.

- [ ] **Step 2: encoding-data chapter를 format과 failure 기준으로 작성한다**

Byte/String boundary, charset 명시, Base64/hex 선택, malformed input failure를 실제 API별로 설명한다. Log/identifier/transport 용도를 구분하고 secret redaction과 대용량 allocation 주의를 operations section에 둔다.

- [ ] **Step 3: time-ranges chapter를 boundary semantics 중심으로 작성한다**

Inclusive/exclusive end, empty range, overlap/containment, timezone이 개입하는 변환을 실제 type과 test가 지원하는 범위에서 설명한다. Business timezone을 암묵적으로 사용하지 않는 완전한 예제를 제공한다.

- [ ] **Step 4: concurrency-lifecycle chapter에 reducer와 shutdown 계약을 작성한다**

`ConcurrentReducer.add`가 full/closed에서 failed future를 반환하는 점, running external stage는 close가 취소하지 않는 점, queued work cancellation을 상태 diagram으로 설명한다. `ShutdownQueue`의 LIFO와 idempotent cleanup을 별도 section으로 둔다.

- [ ] **Step 5: recipes와 landing을 decision map으로 완성한다**

Validation pipeline, bounded recent history, safe aggregation queue, deterministic cleanup recipe를 독립 실행 코드로 제공한다. Landing은 validation, data, time, bounded state, lifecycle의 학습 경로와 표준 Kotlin/JDK API를 먼저 쓸 조건을 명시한다.

- [ ] **Step 6: Core 전체 품질 gate와 commit을 실행한다**

Run:

```bash
ruby scripts/manual/validate_manuals.rb
rg -n "BoundedStack|RingBuffer|ConcurrentReducer|ShutdownQueue" docs/manual/{ko,en}/modules/bluetape4k-core*
git diff --check
```

Expected: validator exit 0, 핵심 계약이 양 언어에 존재, whitespace error 없음.

```bash
git add docs/manual/{ko,en}/modules/bluetape4k-core*
git commit -m "Complete the Core manual around invariants and lifecycle" \
  -m "Constraint: Every technical claim is bounded by current source and representative tests." \
  -m "Rejected: README-style API enumeration | chapters must support implementation decisions." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: ruby scripts/manual/validate_manuals.rb; git diff --check"
```

### Task 11: Core snapshot을 site에 동기화하고 브라우저 검증

**Repository:** Site worktree

**Files:**

- Generated: `src/content/docs/{ko/,}manual/bluetape4k-projects/modules/bluetape4k-core/**`
- Generated: `public/manual-assets/bluetape4k-projects/core/**`
- Generated: `src/data/manual/bluetape4k-projects.{manifest,snapshot}.json`

- [ ] **Step 1: clean Projects source commit에서 sync한다**

Run:

```bash
npm run sync:manual -- \
  --source /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals \
  --repository bluetape4k-projects
```

Expected: snapshot의 documentFiles가 Core 12 localized chapter만큼, assetFiles가 8만큼 증가한다.

- [ ] **Step 2: deterministic validation과 build를 실행한다**

Run: `npm run check:manual && BLUETAPE4K_PROJECTS_SOURCE=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals node --test --test-name-pattern='manual|snapshot|frontmatter|paths' tests/manual/*.test.mjs && npm run build`

Expected: 모든 command exit 0.

- [ ] **Step 3: ko/en Core landing과 6개 chapter를 browser smoke test한다**

Chapter navigation, source/test link, 4개 diagram, code overflow, dark/light contrast, mobile width를 확인한다. Broken image, 404 route, horizontal page overflow가 없어야 한다.

- [ ] **Step 4: Core site snapshot을 커밋한다**

```bash
git add src/content/docs/manual src/content/docs/ko/manual public/manual-assets src/data/manual
git commit -m "Publish the chaptered Core manual snapshot" \
  -m "Constraint: Core pages and diagrams must resolve from the same Projects source commit." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: manual check; targeted Node tests; Astro build; bilingual browser smoke"
```

### Task 12: 기존 blog를 manual-first 파생 콘텐츠로 정렬

**Repository:** Site worktree

**Files:**

- Modify: `src/content/docs/{ko/,}blog/bluetape4k-projects-part1-shared-foundation.mdx`
- Modify: `src/content/docs/{ko/,}blog/bluetape4k-projects-part2-core-coroutines-tests.mdx`
- Modify: `src/content/docs/{ko/,}blog/bluetape4k-flow-extensions-workshop.mdx`
- Modify: `src/content/docs/{ko/,}blog/coroutine-observability-micrometer-readiness.mdx`

- [ ] **Step 1: blog에만 남은 기술 계약을 찾아 manual coverage와 비교한다**

Run:

```bash
rg -n "awaitAny|mapParallel|Subject|ConcurrentReducer|ShutdownQueue|cancellation|readiness" \
  src/content/docs/blog src/content/docs/ko/blog
```

Expected: 각 기술 문장이 Core/Coroutines manual의 해당 chapter에 존재하는지 대응표를 만든다. Manual에 없는 유효한 계약은 먼저 Projects manual에 추가하고 sync한 뒤 blog를 편집한다.

- [ ] **Step 2: blog의 역할을 narrative와 workshop walkthrough로 제한한다**

각 글 서두 또는 기술 section 끝에 bilingual manual link를 추가한다. Exhaustive API contract 표는 manual link로 대체하되, 글의 문제 상황, 경험적 설명, workshop 실행 흐름은 유지한다.

- [ ] **Step 3: diagram reference를 canonical manual asset URL로 교체한다**

Manual로 승격된 diagram은 `/manual-assets/bluetape4k-projects/coroutines/ordered-parallel-flow.svg`, `/manual-assets/bluetape4k-projects/coroutines/observability-boundaries.svg`, `/manual-assets/bluetape4k-projects/core/concurrent-reducer-capacity.svg`처럼 canonical path를 사용한다. Blog 전용 screenshot만 기존 `/assets/`에 남긴다.

- [ ] **Step 4: blog와 manual의 bilingual route를 검증한다**

Run: `npm test && npm run build`

Expected: Node tests PASS, Astro build exit 0, broken internal links 없음.

- [ ] **Step 5: 4개 blog의 ko/en page를 browser smoke test하고 commit한다**

Manual link가 같은 locale route로 이동하고 canonical diagram이 200으로 응답하는지 확인한다.

```bash
git add src/content/docs/blog src/content/docs/ko/blog
git commit -m "Align technical blog narratives with the manual source of truth" \
  -m "Constraint: Blogs may explain and motivate but may not own unique API contracts." \
  -m "Rejected: Deleting the articles | their narrative and workshop context remain valuable." \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: npm test; npm run build; bilingual browser smoke"
```

### Task 13: 전체 contract, visual, snapshot, publish-order gate를 닫는다

**Repositories:** Projects worktree, Site worktree

- [ ] **Step 1: Projects manual의 최종 contract를 검증한다**

Run:

```bash
cd /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals
ruby scripts/manual/validate_manuals_test.rb
ruby scripts/manual/export_manifest_test.rb
ruby scripts/manual/generate_manuals_test.rb
ruby scripts/manual/validate_manuals.rb
git diff --check
git status --short
```

Expected: 모든 test/validator exit 0, diff check clean, status output 없음.

- [ ] **Step 2: SVG/PNG inventory와 실제 file을 대조한다**

Run:

```bash
find docs/manual/assets -type f \( -name '*.svg' -o -name '*.png' \) | sort
ruby scripts/manual/validate_manuals.rb
```

Expected: Manifest 등록 수와 file 수가 일치하고 orphan/missing pair 없음. 모든 PNG는 대응 SVG의 최종 render다.

- [ ] **Step 3: Site snapshot을 source HEAD에서 마지막으로 재생성·검증한다**

Run:

```bash
cd /Users/debop/work/bluetape4k/bluetape4k.github.io/.worktrees/feature-ecosystem-atlas-manual
npm run sync:manual -- --source /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals
npm run check:manual
npm test
npm run build
git diff --check
```

Expected: sync와 check의 sourceCommit이 Projects HEAD와 같고, tests/build/diff check가 모두 성공한다.

- [ ] **Step 4: 최종 visual QA matrix를 완료한다**

한국어/영어 각각 Core landing+6 chapter, Coroutines landing+7 chapter, 관련 blog 4개를 desktop과 mobile width에서 확인한다. Navigation, typography, code wrapping, table overflow, diagram legibility, dark/light contrast, source link, workshop link, locale route를 확인한다. 실패 항목은 소유 저장소에서 수정하고 Step 1~3을 다시 실행한다.

- [ ] **Step 5: publish 순서를 기록한다**

Projects branch를 먼저 push/PR/merge하고 published source commit을 확인한다. Site snapshot JSON의 `sourceCommit`이 그 published commit인지 확인한 뒤에만 site branch push/PR/merge/deploy를 진행한다. 이 단계는 별도 사용자의 외부 side-effect 승인 없이는 실제 push나 PR을 수행하지 않는다.

## 완료 조건

- `docs/manual`만으로 Core와 Coroutines의 API 선택, failure/cancellation, lifecycle/capacity, operations, testing 결정을 내릴 수 있다.
- Core 6개, Coroutines 7개 chapter가 ko/en parity로 존재한다.
- Coroutines 7쌍, Core 4쌍의 canonical diagram이 Projects repo에 있고 validator가 pairing/orphan/reference를 보장한다.
- Site snapshot은 landing, chapter, binary asset을 한 source commit과 digest로 재현한다.
- 기존 blog는 manual을 기술 source of truth로 참조하고 독자적인 고유 API 계약을 소유하지 않는다.
- Projects validator/tests와 Site tests/build/browser QA가 fresh run으로 성공한다.
- Push, PR, merge, deploy는 이 계획의 로컬 구현 완료와 별도 외부 side-effect 승인 이후에만 수행한다.
