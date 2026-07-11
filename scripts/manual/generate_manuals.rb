#!/usr/bin/env ruby

require "fileutils"
require "json"
require "yaml"

module ManualDocs
  class ManualGenerator
    SECTION_IDS = %w[
      problem when-to-use coordinates concepts quick-start api-by-task
      patterns integrations configuration failures operations testing
      workshops limitations sources
    ].freeze

    GROUP_GUIDANCE = {
      "caching" => [
        "cache key design, consistency, invalidation, and backend ownership",
        "Track hit ratio, load latency, eviction, stale reads, backend errors, and reconnect behavior.",
        "cache key, consistency, invalidation, backend ownership",
        "hit ratio, load latency, eviction, stale read, backend 오류, reconnect 동작을 관찰합니다.",
      ],
      "data" => [
        "transaction boundaries, connection ownership, query behavior, and serialization",
        "Track pool saturation, query latency, retries, transaction rollbacks, and schema compatibility.",
        "transaction boundary, connection ownership, query 동작, serialization",
        "pool 포화, query latency, retry, transaction rollback, schema 호환성을 관찰합니다.",
      ],
      "infrastructure" => [
        "client lifecycle, reconnect policy, backpressure, retries, and observability",
        "Track connection state, queue depth, retries, timeouts, remote errors, and graceful shutdown.",
        "client lifecycle, reconnect policy, backpressure, retry, observability",
        "connection 상태, queue 깊이, retry, timeout, remote 오류, graceful shutdown을 관찰합니다.",
      ],
      "io" => [
        "encoding boundaries, resource ownership, streaming, compatibility, and malformed input",
        "Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors.",
        "encoding boundary, resource ownership, streaming, 호환성, malformed input",
        "payload 크기, allocation, latency, malformed input 비율, resource close, protocol 오류를 관찰합니다.",
      ],
      "web" => [
        "request lifecycle, cancellation, routing, context propagation, and test boundaries",
        "Track request latency, status codes, cancellation, queueing, dependency failures, and shutdown.",
        "request lifecycle, cancellation, routing, context propagation, test boundary",
        "request latency, status code, cancellation, queueing, dependency failure, shutdown을 관찰합니다.",
      ],
      "spring" => [
        "auto-configuration conditions, bean ownership, property binding, and application lifecycle",
        "Track condition reports, startup failures, pool/client health, request latency, and graceful shutdown.",
        "auto-configuration condition, bean ownership, property binding, application lifecycle",
        "condition report, startup failure, pool/client health, request latency, graceful shutdown을 관찰합니다.",
      ],
      "testing" => [
        "fixture ownership, isolation, deterministic cleanup, and failure diagnostics",
        "Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically.",
        "fixture ownership, isolation, deterministic cleanup, failure diagnostic",
        "fixture를 격리하고 resource 사용량을 제한하며 diagnostic을 남기고 shared service를 확실히 닫습니다.",
      ],
      "utilities" => [
        "input contracts, value semantics, algorithmic cost, and deterministic output",
        "Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility.",
        "입력 계약, value semantics, algorithm cost, deterministic output",
        "hot path를 측정하고 입력 크기를 제한하며 utility를 호출하는 application boundary에서 failure를 관찰합니다.",
      ],
      "concurrency" => [
        "scope ownership, cancellation, executor lifecycle, blocking boundaries, and shutdown",
        "Track active work, queue depth, cancellation, timeout, executor saturation, and shutdown completion.",
        "scope ownership, cancellation, executor lifecycle, blocking boundary, shutdown",
        "active work, queue 깊이, cancellation, timeout, executor 포화, shutdown 완료를 관찰합니다.",
      ],
      "learning" => [
        "the runnable entry point, required services, expected behavior, and the production pattern demonstrated",
        "Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown.",
        "실행 entry point, 필요한 service, 기대 동작, 예제가 보여 주는 production pattern",
        "격리된 환경에서 example을 실행하고 startup, dependency health, request, shutdown을 확인합니다.",
      ],
      "experiments" => [
        "the hypothesis, benchmark task, environment, metric direction, and comparison limits",
        "Record environment and raw results; compare only equivalent runs and watch variance and allocation.",
        "가설, benchmark task, 환경, metric 방향, 비교 범위",
        "환경과 raw result를 기록하고 같은 조건의 run만 비교하며 variance와 allocation을 확인합니다.",
      ],
    }.freeze

    DEFAULT_GUIDANCE = [
      "public contracts, ownership, failures, configuration, and source-backed tests",
      "Observe latency, failures, resource use, and shutdown at the owning application boundary.",
      "public contract, ownership, failure, configuration, source 기반 test",
      "component를 소유한 application boundary에서 latency, failure, resource 사용량, shutdown을 관찰합니다.",
    ].freeze

    def initialize(repository_root:, manifest_path:)
      @repository_root = File.expand_path(repository_root)
      @manifest_path = File.expand_path(manifest_path)
    end

    def generate(missing_only: true)
      manifest = YAML.safe_load(File.read(@manifest_path))
      generated = []
      manifest.fetch("modules").sort_by { |entry| entry.fetch("id") }.each do |entry|
        {"en" => :en, "ko" => :ko}.each do |field, locale|
          output = File.expand_path(entry.fetch(field), File.dirname(@manifest_path))
          next if missing_only && File.exist?(output)
          FileUtils.mkdir_p(File.dirname(output))
          File.write(output, render(entry, locale))
          generated << output
        end
      end
      generated
    end

    private

    def render(entry, locale)
      facts = facts_for(entry, locale)
      locale == :en ? render_english(entry, facts) : render_korean(entry, facts)
    end

    def facts_for(entry, locale)
      source_dir = entry.fetch("sourceDir")
      readme_path = locale == :ko ? File.join(source_dir, "README.ko.md") : File.join(source_dir, "README.md")
      readme_path = File.join(source_dir, "README.md") unless File.file?(absolute(readme_path))
      readme = File.file?(absolute(readme_path)) ? File.read(absolute(readme_path)) : ""
      build_path = File.join(source_dir, "build.gradle.kts")
      source_files = code_files(entry.fetch("sourcePaths", []), /\.(?:kt|java)$/).first(10)
      test_files = code_files(entry.fetch("testPaths", []), /\.(?:kt|java)$/).first(8)
      resources = resource_files(source_dir).first(8)
      dependencies = dependency_lines(build_path).first(12)
      headings = readme.scan(/^##+\s+(.+?)\s*$/).flatten.map { |heading| clean_text(heading) }.reject(&:empty?).first(10)
      {
        title: readme_title(readme, entry.fetch("id")),
        summary: readme_summary(readme, entry.fetch("id"), locale),
        readme_path: readme_path,
        build_path: build_path,
        source_files: source_files,
        test_files: test_files,
        resources: resources,
        dependencies: dependencies,
        headings: headings,
      }
    end

    def render_english(entry, facts)
      guidance = GROUP_GUIDANCE.fetch(entry.fetch("group"), DEFAULT_GUIDANCE)
      <<~MD
        ---
        manualId: #{entry.fetch("id")}
        title: #{yaml_string(facts[:title])}
        description: #{yaml_string(facts[:summary])}
        kind: #{entry.fetch("kind")}
        group: #{entry.fetch("group")}
        ---

        # #{facts[:title]}

        ## Problem {#problem}

        #{facts[:summary]} This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

        ## When to use {#when-to-use}

        Use `#{entry.fetch('id')}` when the application needs #{guidance[0]}. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

        ## Coordinates {#coordinates}

        #{english_coordinates(entry)}

        Gradle project path: `#{entry.fetch('gradlePath')}`. Source directory: `#{entry.fetch('sourceDir')}`.

        ## Concepts {#concepts}

        #{english_concepts(facts)}

        ## Quick start {#quick-start}

        #{english_quick_start(entry, facts)}

        ## API by task {#api-by-task}

        #{english_api_table(facts[:source_files])}

        ## Patterns {#patterns}

        #{english_patterns(facts)}

        ## Integrations {#integrations}

        #{english_integrations(facts)}

        ## Configuration {#configuration}

        #{english_configuration(facts)}

        ## Failures {#failures}

        Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

        ## Operations {#operations}

        #{guidance[1]} Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

        ## Testing {#testing}

        Run the module test task:

        ```bash
        ./gradlew #{entry.fetch('gradlePath')}:test --no-configuration-cache
        ```

        #{english_test_list(facts[:test_files])}

        ## Workshops {#workshops}

        #{english_workshops(entry, facts)}

        ## Limitations {#limitations}

        This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

        ## Sources {#sources}

        - [Module README](#{root_link(facts[:readme_path])})
        - [Module build](#{root_link(facts[:build_path])})
        #{source_links(facts[:source_files] + facts[:test_files])}
      MD
    end

    def render_korean(entry, facts)
      guidance = GROUP_GUIDANCE.fetch(entry.fetch("group"), DEFAULT_GUIDANCE)
      <<~MD
        ---
        manualId: #{entry.fetch("id")}
        title: #{yaml_string(facts[:title])}
        description: #{yaml_string(facts[:summary])}
        kind: #{entry.fetch("kind")}
        group: #{entry.fetch("group")}
        ---

        # #{facts[:title]}

        ## 해결하는 문제 {#problem}

        #{facts[:summary]} 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

        ## 사용 시점 {#when-to-use}

        애플리케이션에 #{guidance[2]}이 필요할 때 `#{entry.fetch('id')}`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

        ## 의존성 좌표 {#coordinates}

        #{korean_coordinates(entry)}

        Gradle project path는 `#{entry.fetch('gradlePath')}`, source directory는 `#{entry.fetch('sourceDir')}`입니다.

        ## 핵심 개념 {#concepts}

        #{korean_concepts(facts)}

        ## 빠른 시작 {#quick-start}

        #{korean_quick_start(entry, facts)}

        ## 작업별 API {#api-by-task}

        #{korean_api_table(facts[:source_files])}

        ## 권장 패턴 {#patterns}

        #{korean_patterns(facts)}

        ## 연동 {#integrations}

        #{korean_integrations(facts)}

        ## 설정 {#configuration}

        #{korean_configuration(facts)}

        ## 실패 동작 {#failures}

        failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

        ## 운영 {#operations}

        #{guidance[3]} capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

        ## 테스트 {#testing}

        모듈 test task는 다음과 같습니다.

        ```bash
        ./gradlew #{entry.fetch('gradlePath')}:test --no-configuration-cache
        ```

        #{korean_test_list(facts[:test_files])}

        ## 워크숍 {#workshops}

        #{korean_workshops(entry, facts)}

        ## 제한 사항 {#limitations}

        이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

        ## 근거 {#sources}

        - [모듈 README](#{root_link(facts[:readme_path])})
        - [모듈 build](#{root_link(facts[:build_path])})
        #{source_links(facts[:source_files] + facts[:test_files])}
      MD
    end

    def english_coordinates(entry)
      if entry["artifact"]
        <<~MD.chomp
          ```kotlin
          dependencies {
              implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
              implementation("#{entry.fetch('artifact')}")
          }
          ```
        MD
      else
        "This #{entry.fetch('kind')} project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command."
      end
    end

    def korean_coordinates(entry)
      if entry["artifact"]
        english_coordinates(entry)
      else
        "이 #{entry.fetch('kind')} project는 Maven artifact로 게시하지 않습니다. 저장소에서 실행하고 명령을 선택하기 전에 Gradle task를 확인합니다."
      end
    end

    def english_quick_start(entry, facts)
      if entry["kind"] == "library"
        type = facts[:source_files].first
        type_hint = type ? "Open [`#{file_label(type)}`](#{root_link(type)}) first; it is a concrete source entry point for the module." : "The module has no Kotlin/Java source entry point; inspect its Gradle model and README."
        "Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. #{type_hint}"
      else
        "List the project tasks before running the example or benchmark:\n\n```bash\n./gradlew #{entry.fetch('gradlePath')}:tasks --all\n```\n\nThen use the command documented by the module README and keep required external services isolated."
      end
    end

    def korean_quick_start(entry, facts)
      if entry["kind"] == "library"
        type = facts[:source_files].first
        type_hint = type ? "먼저 [`#{file_label(type)}`](#{root_link(type)})를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다." : "Kotlin/Java source entry point가 없는 모듈이므로 Gradle model과 README를 확인합니다."
        "위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. #{type_hint}"
      else
        "example이나 benchmark를 실행하기 전에 project task를 확인합니다.\n\n```bash\n./gradlew #{entry.fetch('gradlePath')}:tasks --all\n```\n\n그다음 모듈 README에 기록된 명령을 사용하고 필요한 외부 service는 격리합니다."
      end
    end

    def english_concepts(facts)
      names = facts[:source_files].map { |path| "`#{file_label(path)}`" }
      return "The module is configuration or platform metadata and has no Kotlin/Java source type to index." if names.empty?
      "The first source-level concepts to inspect are #{join_words(names.first(8))}. File names are navigation anchors; read each declaration and its tests before treating it as a public contract."
    end

    def korean_concepts(facts)
      names = facts[:source_files].map { |path| "`#{file_label(path)}`" }
      return "이 모듈은 설정 또는 platform metadata이며 index할 Kotlin/Java source type이 없습니다." if names.empty?
      "먼저 확인할 source 개념은 #{join_korean(names.first(8))}입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다."
    end

    def english_api_table(files)
      return "No Kotlin/Java source file is registered for this module. Use the build model and README as its public surface." if files.empty?
      rows = files.map { |path| "| [`#{file_label(path)}`](#{root_link(path)}) | Inspect this declaration's constructors, functions, and ownership contract. |" }
      (["| Entry point | What to verify |", "| --- | --- |"] + rows).join("\n")
    end

    def korean_api_table(files)
      return "이 모듈에는 등록된 Kotlin/Java source file이 없습니다. build model과 README가 public surface입니다." if files.empty?
      rows = files.map { |path| "| [`#{file_label(path)}`](#{root_link(path)}) | constructor, function, ownership 계약을 확인합니다. |" }
      (["| Entry point | 확인할 내용 |", "| --- | --- |"] + rows).join("\n")
    end

    def english_patterns(facts)
      if facts[:headings].empty?
        "Keep adoption narrow: choose one entry point, lock its behavior with a focused test, and connect any owned resource to the caller lifecycle."
      else
        "The README evidence is organized around #{join_words(facts[:headings].map { |heading| "**#{heading}**" })}. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle."
      end
    end

    def korean_patterns(facts)
      if facts[:headings].empty?
        "entry point 하나를 선택하고 focused test로 동작을 고정한 뒤 소유한 resource를 caller lifecycle에 연결합니다."
      else
        "README 근거는 #{join_korean(facts[:headings].map { |heading| "**#{heading}**" })} 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다."
      end
    end

    def english_integrations(facts)
      return "The module build declares no direct `api`, `implementation`, `compileOnly`, or `runtimeOnly` dependency line. Inspect plugins and generated metadata in the build file." if facts[:dependencies].empty?
      "The current build declares these integration edges:\n\n```kotlin\n#{facts[:dependencies].join("\n")}\n```\n\nTreat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs."
    end

    def korean_integrations(facts)
      return "모듈 build에 직접적인 `api`, `implementation`, `compileOnly`, `runtimeOnly` dependency line이 없습니다. build file의 plugin과 generated metadata를 확인합니다." if facts[:dependencies].empty?
      "현재 build에 선언된 integration edge는 다음과 같습니다.\n\n```kotlin\n#{facts[:dependencies].join("\n")}\n```\n\n`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다."
    end

    def english_configuration(facts)
      return "No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source." if facts[:resources].empty?
      "Configuration resources found in the module:\n\n#{facts[:resources].map { |path| "- [`#{File.basename(path)}`](#{root_link(path)})" }.join("\n")}\n\nRead property names and defaults from these resources and the binding source before overriding them."
    end

    def korean_configuration(facts)
      return "`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다." if facts[:resources].empty?
      "모듈에서 찾은 설정 resource는 다음과 같습니다.\n\n#{facts[:resources].map { |path| "- [`#{File.basename(path)}`](#{root_link(path)})" }.join("\n")}\n\noverride하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다."
    end

    def english_test_list(files)
      return "No Kotlin/Java test file was found in the manifest's test paths. Verify the module build and add a focused contract test when adopting behavior not covered elsewhere." if files.empty?
      "Representative test anchors:\n\n#{files.map { |path| "- [`#{file_label(path)}`](#{root_link(path)})" }.join("\n")}"
    end

    def korean_test_list(files)
      return "manifest의 test path에서 Kotlin/Java test file을 찾지 못했습니다. module build를 확인하고 다른 곳에서 검증하지 않는 동작을 도입할 때 focused contract test를 추가합니다." if files.empty?
      "대표 test anchor는 다음과 같습니다.\n\n#{files.map { |path| "- [`#{file_label(path)}`](#{root_link(path)})" }.join("\n")}"
    end

    def english_workshops(entry, facts)
      workshops = entry.fetch("workshops", [])
      return "No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence." if workshops.empty?
      workshops.map { |path| "- [#{path}](#{root_link(path)})" }.join("\n")
    end

    def korean_workshops(entry, facts)
      workshops = entry.fetch("workshops", [])
      return "manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다." if workshops.empty?
      workshops.map { |path| "- [#{path}](#{root_link(path)})" }.join("\n")
    end

    def source_links(files)
      files.first(12).map { |path| "- [`#{file_label(path)}`](#{root_link(path)})" }.join("\n")
    end

    def code_files(paths, pattern)
      paths.flat_map do |relative|
        root = absolute(relative)
        next [] unless File.directory?(root)
        Dir[File.join(root, "**", "*")]
          .select { |path| File.file?(path) && path.match?(pattern) }
          .map { |path| relative_path(path) }
      end.sort
    end

    def resource_files(source_dir)
      root = absolute(File.join(source_dir, "src/main/resources"))
      return [] unless File.directory?(root)
      Dir[File.join(root, "**", "*")]
        .select { |path| File.file?(path) }
        .map { |path| relative_path(path) }
        .sort
    end

    def dependency_lines(build_path)
      path = absolute(build_path)
      return [] unless File.file?(path)
      File.readlines(path)
        .map(&:strip)
        .select { |line| line.match?(/\A(?:api|implementation|compileOnly|runtimeOnly)\(/) }
        .uniq
    end

    def readme_title(readme, fallback)
      heading = readme.lines.find { |line| line.match?(/^#\s+/) }
      clean_text(heading ? heading.sub(/^#\s+/, "") : fallback)
    end

    def readme_summary(readme, fallback, locale)
      line = readme.lines.map(&:strip).find do |candidate|
        !candidate.empty? &&
          !candidate.start_with?("#", "[![", "![", "<", "|", "```", "---", "- ") &&
          !candidate.match?(/\A\[?(?:English|한국어)\]?\s*\|/)
      end
      fallback_text = locale == :ko ? "#{fallback} 모듈의 현재 source와 test를 작업 중심으로 설명합니다." : "Task-oriented guide to the current #{fallback} source and tests."
      truncate(clean_text(line || fallback_text), 180)
    end

    def clean_text(value)
      value.to_s
        .gsub(/\[([^\]]+)\]\([^)]+\)/, '\\1')
        .gsub(/[*_`]/, "")
        .gsub(/<[^>]+>/, "")
        .gsub(/\s+/, " ")
        .strip
    end

    def truncate(value, max)
      value.length > max ? value[0, max - 1].rstrip + "…" : value
    end

    def yaml_string(value)
      JSON.generate(value)
    end

    def root_link(path)
      "../../../../#{path}"
    end

    def file_label(path)
      File.basename(path).sub(/\.(?:kt|java)$/, "")
    end

    def join_words(values)
      return values.first.to_s if values.length <= 1
      values[0...-1].join(", ") + ", and " + values[-1]
    end

    def join_korean(values)
      values.join(", ")
    end

    def absolute(path)
      File.expand_path(path, @repository_root)
    end

    def relative_path(path)
      path.sub(@repository_root + File::SEPARATOR, "")
    end
  end
end

if $PROGRAM_NAME == __FILE__
  missing_only = ARGV.delete("--force").nil?
  repository_root = Dir.pwd
  manifest_path = ARGV.fetch(0, "docs/manual/manifest.yaml")
  generator = ManualDocs::ManualGenerator.new(
    repository_root: repository_root,
    manifest_path: manifest_path,
  )
  generated = generator.generate(missing_only: missing_only)
  puts "Generated #{generated.length} manual files."
end
