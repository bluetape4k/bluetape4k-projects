# All-Module Manuals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-projects`의 90개 등록 subproject 모두에 README보다 상세한 영문·한글 사용자 문서를 제공하고 자동 검증한다.

**Architecture:** `settings.gradle.kts`가 등록한 Gradle project 정보를 JSON으로 내보내고, `docs/manual/manifest.yaml`과 양쪽 locale 문서를 Ruby 표준 라이브러리 기반 validator로 대조한다. 라이브러리는 module manual, example과 benchmark는 같은 품질 계약을 따르는 learning/experiment manual로 작성한다.

**Tech Stack:** Gradle Kotlin DSL, Markdown, YAML, Ruby stdlib (`yaml`, `json`, `minitest`), GitHub Actions

---

## 파일 구조

```text
bluetape4k-projects/
├── build.gradle.kts
├── docs/manual/
│   ├── manifest.yaml
│   ├── generated/manifest.json
│   ├── templates/{module,example,benchmark}.md
│   ├── en/{index,getting-started}.md
│   ├── en/architecture/*.md
│   ├── en/guides/*.md
│   ├── en/modules/*.md
│   ├── ko/{index,getting-started}.md
│   ├── ko/architecture/*.md
│   ├── ko/guides/*.md
│   └── ko/modules/*.md
├── scripts/manual/
│   ├── manual_contract.rb
│   ├── export_manifest.rb
│   ├── validate_manuals.rb
│   └── validate_manuals_test.rb
└── .github/workflows/manual-docs.yml
```

`docs/manual/generated/manifest.json`은 YAML을 정규화한 소비자용 산출물이며 커밋한다. 사이트 동기화 도구는 YAML을 직접 해석하지 않고 이 JSON을 사용한다.

### Task 1: Gradle module inventory export

**Files:**
- Modify: `build.gradle.kts`
- Test: `build/manual/module-inventory.json`

- [ ] **Step 1: 실패하는 inventory 계약 검사를 작성한다**

`build.gradle.kts`에 task를 등록하기 전에 다음 명령이 실패하는지 확인한다.

```bash
./gradlew exportManualModuleInventory --no-configuration-cache
```

Expected: `Task 'exportManualModuleInventory' not found`.

- [ ] **Step 2: 실제 Gradle project model에서 inventory를 내보낸다**

`build.gradle.kts`의 root project 영역에 다음 형태의 task를 추가한다. project name을 디렉터리 규칙으로 재계산하지 말고 `subprojects`를 사용한다.

```kotlin
tasks.register("exportManualModuleInventory") {
    val outputFile = layout.buildDirectory.file("manual/module-inventory.json")
    outputs.file(outputFile)

    doLast {
        val rows = subprojects.sortedBy { it.path }.map { project ->
            val relativeDir = rootDir.toPath().relativize(project.projectDir.toPath()).toString()
            val kind = when {
                relativeDir.startsWith("examples/") -> "example"
                relativeDir.startsWith("benchmark/") -> "benchmark"
                else -> "library"
            }
            mapOf(
                "gradlePath" to project.path,
                "projectName" to project.name,
                "sourceDir" to relativeDir,
                "kind" to kind,
            )
        }
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(rows)) + "\n")
        }
    }
}
```

- [ ] **Step 3: inventory가 90개 project를 고유하게 포함하는지 검증한다**

Run:

```bash
./gradlew exportManualModuleInventory --no-configuration-cache
ruby -rjson -e 'rows=JSON.parse(File.read("build/manual/module-inventory.json")); abort "expected 90" unless rows.size == 90; abort "duplicate path" unless rows.map { _1["gradlePath"] }.uniq.size == 90'
```

Expected: exit 0.

- [ ] **Step 4: 커밋한다**

```bash
git add build.gradle.kts
git commit -m "docs: derive manual inventory from the Gradle model" \
  -m "Constraint: Manual coverage must follow settings.gradle.kts rather than a copied module list.\nConfidence: high\nScope-risk: narrow\nTested: ./gradlew exportManualModuleInventory --no-configuration-cache"
```

### Task 2: Manual contract validator

**Files:**
- Create: `scripts/manual/manual_contract.rb`
- Create: `scripts/manual/validate_manuals.rb`
- Create: `scripts/manual/validate_manuals_test.rb`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/manifest.yaml`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/en/modules/sample.md`
- Create: `scripts/manual/test-fixtures/valid/docs/manual/ko/modules/sample.md`

- [ ] **Step 1: validator의 실패 사례를 테스트로 고정한다**

`scripts/manual/validate_manuals_test.rb`에 다음 계약을 작성한다.

```ruby
require "minitest/autorun"
require_relative "manual_contract"

class ValidateManualsTest < Minitest::Test
  def test_reports_missing_locale_and_required_sections
    validator = ManualDocs::Validator.new(
      inventory: [{ "gradlePath" => ":sample", "projectName" => "sample", "sourceDir" => "io/sample", "kind" => "library" }],
      manifest_path: fixture("missing-ko/docs/manual/manifest.yaml"),
      repository_root: fixture("missing-ko"),
    )

    assert_includes validator.errors, "sample: missing Korean document"
    assert validator.errors.any? { |error| error.include?("required section") }
  end

  def test_accepts_complete_bilingual_module
    validator = validator_for("valid")
    assert_empty validator.errors
  end

  private

  def fixture(name) = File.expand_path("test-fixtures/#{name}", __dir__)
end
```

- [ ] **Step 2: 테스트가 구현 부재로 실패하는지 확인한다**

Run: `ruby scripts/manual/validate_manuals_test.rb`

Expected: FAIL with missing `ManualDocs::Validator`.

- [ ] **Step 3: 필수 section ID와 manifest schema를 구현한다**

`scripts/manual/manual_contract.rb`는 다음 상수를 공개한다.

```ruby
module ManualDocs
  REQUIRED_SECTIONS = %w[
    problem when-to-use coordinates concepts quick-start api-by-task
    patterns integrations configuration failures operations testing
    workshops limitations sources
  ].freeze

  VALID_KINDS = %w[library example benchmark].freeze
end
```

Validator는 inventory 누락·초과·중복, locale 쌍, frontmatter `manualId`, 15개 section ID, source/test/workshop 상대 경로를 모두 검사하고 오류를 정렬해 반환한다.

- [ ] **Step 4: CLI를 구현한다**

`scripts/manual/validate_manuals.rb`는 다음 인터페이스를 제공한다.

```ruby
#!/usr/bin/env ruby
require "json"
require_relative "manual_contract"

inventory_path = ARGV.fetch(0, "build/manual/module-inventory.json")
manifest_path = ARGV.fetch(1, "docs/manual/manifest.yaml")
errors = ManualDocs::Validator.new(
  inventory: JSON.parse(File.read(inventory_path)),
  manifest_path: manifest_path,
  repository_root: Dir.pwd,
).errors

abort(errors.join("\n")) unless errors.empty?
puts "Manuals are aligned."
```

- [ ] **Step 5: 테스트와 실제 빈 상태 실패를 확인한다**

Run:

```bash
ruby scripts/manual/validate_manuals_test.rb
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals.rb
```

Expected: tests PASS; real validation FAIL because `docs/manual/manifest.yaml` does not exist yet.

- [ ] **Step 6: 커밋한다**

```bash
git add scripts/manual
git commit -m "test: define the all-module manual contract" \
  -m "Constraint: Validation must use only repository-local tools and Ruby standard libraries.\nConfidence: high\nScope-risk: narrow\nTested: ruby scripts/manual/validate_manuals_test.rb"
```

### Task 3: Manifest, templates, and manual landing pages

**Files:**
- Create: `docs/manual/manifest.yaml`
- Create: `docs/manual/templates/module.md`
- Create: `docs/manual/templates/example.md`
- Create: `docs/manual/templates/benchmark.md`
- Create: `docs/manual/en/index.md`
- Create: `docs/manual/en/getting-started.md`
- Create: `docs/manual/en/architecture/repository-map.md`
- Create: `docs/manual/ko/index.md`
- Create: `docs/manual/ko/getting-started.md`
- Create: `docs/manual/ko/architecture/repository-map.md`
- Create: `scripts/manual/export_manifest.rb`
- Create: `docs/manual/generated/manifest.json`

- [ ] **Step 1: 90개 inventory를 manifest 항목으로 등록한다**

각 항목은 다음 필드를 가진다.

```yaml
schemaVersion: 1
modules:
  - id: bluetape4k-core
    gradlePath: ":bluetape4k-core"
    sourceDir: bluetape4k/core
    kind: library
    group: foundation
    artifact: io.bluetape4k:bluetape4k-core
    en: en/modules/bluetape4k-core.md
    ko: ko/modules/bluetape4k-core.md
    sourcePaths:
      - bluetape4k/core/src/main/kotlin
    testPaths:
      - bluetape4k/core/src/test/kotlin
    workshops: []
```

`example`과 `benchmark` 항목은 `artifact: null`을 명시한다.

- [ ] **Step 2: 세 종류의 문서 template을 만든다**

각 template은 `manualId`, `title`, `description`, `kind`, `group` frontmatter와 15개 고정 section ID를 포함한다. 적용되지 않는 section은 삭제하지 않고 이유를 기록하도록 안내한다.

- [ ] **Step 3: 영문·한글 landing과 architecture 문서를 작성한다**

두 locale은 동일한 heading ID와 링크 구조를 사용한다. `getting-started.md`는 BOM 설치, 모듈 선택, guide → manual → workshop 이동 순서를 설명한다.

- [ ] **Step 4: YAML을 정규화한 JSON snapshot을 생성한다**

`scripts/manual/export_manifest.rb`는 `YAML.safe_load`, key 정렬, module `id` 정렬을 적용해 `docs/manual/generated/manifest.json`을 쓴다.

- [ ] **Step 5: manifest 구조를 검증한다**

Run:

```bash
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/export_manifest.rb --check
ruby scripts/manual/validate_manuals.rb
```

Expected: manifest/inventory checks reach only the expected missing-document failures.

- [ ] **Step 6: 커밋한다**

```bash
git add docs/manual scripts/manual/export_manifest.rb
git commit -m "docs: establish the bilingual manual source contract" \
  -m "Constraint: Every registered subproject needs an explicit English and Korean document.\nConfidence: high\nScope-risk: moderate\nTested: manifest export and inventory validation"
```

## Manual authoring wave contract

Tasks 4-15는 아래 순서를 각 module마다 반복한다.

- [ ] source, tests, build file, 관련 README와 workshop을 읽고 근거 메모를 만든다.
- [ ] `docs/manual/en/modules/<id>.md`를 15개 section 계약에 맞게 작성한다.
- [ ] `docs/manual/ko/modules/<id>.md`를 동일 사실과 heading ID로 자연스럽게 작성한다.
- [ ] 코드 예제의 import, artifact, configuration key, source/test link를 실제 파일로 검증한다.
- [ ] `ruby scripts/manual/validate_manuals.rb`를 실행해 해당 wave 오류를 제거한다.
- [ ] wave 단위로 Lore protocol 커밋을 만든다.

### Task 4: Foundation manuals

**Source modules:** `bluetape4k/annotations`, `bluetape4k/bom`, `bluetape4k/core`, `bluetape4k/coroutines`, `bluetape4k/logging`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{annotations,bom,core,coroutines,logging}.md`

검증: `./gradlew :bluetape4k-core:test :bluetape4k-coroutines:test --no-configuration-cache`와 manual validator.

### Task 5: Cache manuals

**Source modules:** `cache/cache-core`, `cache/cache-hazelcast`, `cache/cache-lettuce`, `cache/cache-redisson`, `cache/hibernate-cache-lettuce`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{cache-core,cache-hazelcast,cache-lettuce,cache-redisson,hibernate-cache-lettuce}.md`

### Task 6: Data manuals

**Source modules:** `data/cassandra`, `data/hibernate-reactive`, `data/hibernate`, `data/jdbc`, `data/mongodb`, `data/r2dbc`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{cassandra,hibernate-reactive,hibernate,jdbc,mongodb,r2dbc}.md`

### Task 7: Infrastructure manuals

**Source modules:** `infra/bucket4j`, `infra/elasticsearch`, `infra/kafka-logback`, `infra/kafka`, `infra/kafka4`, `infra/lettuce`, `infra/micrometer`, `infra/nats`, `infra/opentelemetry`, `infra/pulsar`, `infra/redis`, `infra/redisson`, `infra/resilience4j`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{bucket4j,elasticsearch,kafka-logback,kafka,kafka4,lettuce,micrometer,nats,opentelemetry,pulsar,redis,redisson,resilience4j}.md`

### Task 8: I/O manuals, serialization slice

**Source modules:** `io/avro`, `io/csv`, `io/fastjson2`, `io/io`, `io/jackson2`, `io/jackson3`, `io/json`, `io/okio`, `io/protobuf`, `io/tink`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{avro,csv,fastjson2,io,jackson2,jackson3,json,okio,protobuf,tink}.md`

### Task 9: I/O manuals, transport slice

**Source modules:** `io/feign`, `io/grpc`, `io/http`, `io/netty`, `io/retrofit2`, `io/vertx`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{feign,grpc,http,netty,retrofit2,vertx}.md`

### Task 10: Ktor manuals

**Source modules:** `ktor/core`, `ktor/observability`, `ktor/openapi`, `ktor/resilience4j`, `ktor/testing`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-ktor-{core,observability,openapi,resilience4j,testing}.md`

### Task 11: Spring Boot manuals

**Source modules:** `spring-boot/cassandra-demo`, `spring-boot/cassandra`, `spring-boot/core`, `spring-boot/hibernate-lettuce-demo`, `spring-boot/hibernate-lettuce`, `spring-boot/mongodb`, `spring-boot/r2dbc`, `spring-boot/redis`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-spring-boot-{cassandra-demo,cassandra,core,hibernate-lettuce-demo,hibernate-lettuce,mongodb,r2dbc,redis}.md`

### Task 12: Testing manuals

**Source modules:** `testing/assertions`, `testing/junit5`, `testing/mock-web-server`, `testing/mock-webflux-server`, `testing/testcontainers`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{assertions,junit5,mock-web-server,mock-webflux-server,testcontainers}.md`

### Task 13: Utility manuals

**Source modules:** `utils/geo`, `utils/idgenerators`, `utils/javatimes`, `utils/jwt`, `utils/math`, `utils/measured`, `utils/money`, `utils/mutiny`, `utils/probabilistic`, `utils/rule-engine`, `utils/science`, `utils/states`, `utils/workflow`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-{geo,idgenerators,javatimes,jwt,math,measured,money,mutiny,probabilistic,rule-engine,science,states,workflow}.md`

### Task 14: Virtual-thread manuals

**Source modules:** `virtualthread/api`, `virtualthread/jdk21`, `virtualthread/jdk25`

**Files:**
- Create: `docs/manual/{en,ko}/modules/bluetape4k-virtualthread-{api,jdk21,jdk25}.md`

### Task 15: Example and benchmark manuals

**Source modules:** `examples/coroutines-demo`, `examples/jpa-blazepersistence-demo`, `examples/jpa-querydsl-demo`, `examples/redisson-demo`, `examples/virtualthreads-demo`, `examples/spring-boot/idgenerator-spring-boot-demo`, `examples/spring-boot/observability-spring-boot-demo`, `examples/ktor/idgenerator-ktor-demo`, `examples/ktor/observability-ktor-demo`, `benchmark/protobuf-codec-benchmark`, `benchmark/web-framework-benchmark`

**Files:**
- Create: matching `docs/manual/{en,ko}/modules/*.md` paths from the committed manifest

각 문서는 설치 좌표 대신 실행 방법, 학습 목표, 사용 library, 예상 결과, benchmark 환경과 해석 한계를 기록한다.

### Task 16: Problem-oriented guides and workshop links

**Files:**
- Create: `docs/manual/{en,ko}/guides/http-clients.md`
- Create: `docs/manual/{en,ko}/guides/serialization.md`
- Create: `docs/manual/{en,ko}/guides/caching.md`
- Create: `docs/manual/{en,ko}/guides/data-access.md`
- Create: `docs/manual/{en,ko}/guides/messaging.md`
- Create: `docs/manual/{en,ko}/guides/observability.md`
- Create: `docs/manual/{en,ko}/guides/testing.md`
- Create: `docs/manual/{en,ko}/guides/virtual-threads.md`
- Modify: `docs/manual/manifest.yaml`
- Modify: `docs/manual/generated/manifest.json`

- [ ] **Step 1: 각 guide의 선택 표를 작성한다** — 요구사항, 권장 module, 대안, 피해야 할 경우, 관련 manual을 포함한다.
- [ ] **Step 2: workspace의 실제 workshop chapter 경로만 연결한다** — `bluetape4k-workshop`, `exposed-workshop`, `exposed-r2dbc-workshop`, `timefold-workshop`을 우선한다.
- [ ] **Step 3: 역방향 링크를 manifest에 추가한다** — module → guide/workshop 연결이 검색 소비자에게 노출돼야 한다.
- [ ] **Step 4: validator와 링크 검사를 실행한다**.
- [ ] **Step 5: 커밋한다**.

### Task 17: Manual-only CI gate

**Files:**
- Create: `.github/workflows/manual-docs.yml`
- Modify: `docs/process/module-documentation-checklist.md`

- [ ] **Step 1: manual-only 변경에서 기존 CI가 실행되지 않는 것을 workflow path와 비교한다**.
- [ ] **Step 2: `manual-docs.yml`을 추가한다**.

```yaml
name: Manual Documentation
on:
  pull_request:
    paths:
      - "docs/manual/**"
      - "scripts/manual/**"
      - "build.gradle.kts"
      - ".github/workflows/manual-docs.yml"
  push:
    branches: [develop, main]
    paths:
      - "docs/manual/**"
      - "scripts/manual/**"
      - "build.gradle.kts"
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
      - uses: ruby/setup-ruby@v1
        with:
          ruby-version: "3.3"
      - run: ./gradlew exportManualModuleInventory --no-configuration-cache
      - run: ruby scripts/manual/validate_manuals_test.rb
      - run: ruby scripts/manual/export_manifest.rb --check
      - run: ruby scripts/manual/validate_manuals.rb
```

- [ ] **Step 3: checklist에 manual manifest와 locale parity 검사를 추가한다**.
- [ ] **Step 4: `actionlint`, validator, `git diff --check`를 실행한다**.
- [ ] **Step 5: 커밋한다**.

### Task 18: Repository-wide completion proof

**Files:**
- Modify: `docs/manual/generated/manifest.json` only when normalization changes

- [ ] **Step 1: 전체 inventory와 문서 계약을 검증한다**.

```bash
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals_test.rb
ruby scripts/manual/export_manifest.rb --check
ruby scripts/manual/validate_manuals.rb
./gradlew projects --no-configuration-cache
git diff --check
```

Expected: 90 modules, 180 localized module documents, `Manuals are aligned.`

- [ ] **Step 2: 변경된 각 module의 대표 Gradle test를 domain wave별로 실행하고 결과를 기록한다**.
- [ ] **Step 3: manifest의 workshop/source/test 링크를 실제 경로로 재검사한다**.
- [ ] **Step 4: PR의 마지막 Markdown section을 `## DoD Status`로 작성하고 검증 결과를 반영한다**.
