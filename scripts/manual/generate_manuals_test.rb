require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "generate_manuals"

class GenerateManualsTest < Minitest::Test
  REQUIRED_SECTIONS = %w[
    problem when-to-use coordinates concepts quick-start api-by-task
    patterns integrations configuration failures operations testing
    workshops limitations sources
  ].freeze

  def test_generates_bilingual_source_backed_manuals_without_overwriting_existing_files
    Dir.mktmpdir("manual-generator") do |root|
      prepare_repository(root)
      generator = ManualDocs::ManualGenerator.new(
        repository_root: root,
        manifest_path: File.join(root, "docs/manual/manifest.yaml"),
      )

      generated = generator.generate(missing_only: true)

      assert_equal 2, generated.length
      english = File.read(File.join(root, "docs/manual/en/modules/sample.md"))
      korean = File.read(File.join(root, "docs/manual/ko/modules/sample.md"))
      assert_includes english, "manualId: sample"
      assert_includes korean, "manualId: sample"
      assert_includes english, 'title: "Sample cache client"'
      assert_includes korean, 'title: "샘플 캐시 클라이언트"'
      assert_includes english, "learningOrder: 10"
      assert_includes korean, "learningOrder: 10"
      assert_includes english, "SampleClient"
      assert_includes english, "SampleClientTest"
      assert_includes english, "English sample client for remote calls."
      assert_includes english, "cache-aside"
      assert_includes korean, "캐시 어사이드"
      assert_includes english, 'implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))'
      assert_includes korean, 'implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))'
      refute_includes english, "io.github.bluetape4k:bluetape4k-bom"
      refute_includes korean, "io.github.bluetape4k:bluetape4k-bom"
      refute_includes korean, "English | 한국어"
      refute_includes korean, ", and "
      assert_equal REQUIRED_SECTIONS, english.scan(/\{#([a-z0-9-]+)\}/).flatten
      assert_equal REQUIRED_SECTIONS, korean.scan(/\{#([a-z0-9-]+)\}/).flatten

      File.write(File.join(root, "docs/manual/en/modules/sample.md"), "preserve me\n")
      generator.generate(missing_only: true)
      assert_equal "preserve me\n", File.read(File.join(root, "docs/manual/en/modules/sample.md"))

      generator.generate(missing_only: false, preserve_groups: ["caching"])
      assert_equal "preserve me\n", File.read(File.join(root, "docs/manual/en/modules/sample.md"))
    end
  end

  private

  def prepare_repository(root)
    paths = %w[
      docs/manual
      io/sample/src/main/kotlin/io/example
      io/sample/src/test/kotlin/io/example
    ]
    paths.each { |path| FileUtils.mkdir_p(File.join(root, path)) }
    File.write(File.join(root, "io/sample/README.md"), "# Sample module\n\nEnglish sample client for remote\ncalls.\n")
    File.write(File.join(root, "io/sample/README.ko.md"), "# Sample 모듈\n\n[English](./README.md) | 한국어\n\n원격 호출을 위한 sample client입니다.\n")
    File.write(File.join(root, "io/sample/build.gradle.kts"), "dependencies {\n    api(libs.sample.api)\n}\n")
    File.write(File.join(root, "io/sample/src/main/kotlin/io/example/SampleClient.kt"), "class SampleClient\n")
    File.write(File.join(root, "io/sample/src/test/kotlin/io/example/SampleClientTest.kt"), "class SampleClientTest\n")
    manifest = {
      "schemaVersion" => 1,
      "modules" => [
        {
          "id" => "sample",
          "title" => {
            "en" => "Sample cache client",
            "ko" => "샘플 캐시 클라이언트",
          },
          "learningOrder" => 10,
          "gradlePath" => ":sample",
          "sourceDir" => "io/sample",
          "kind" => "library",
          "group" => "caching",
          "artifact" => "io.github.bluetape4k:sample",
          "en" => "en/modules/sample.md",
          "ko" => "ko/modules/sample.md",
          "sourcePaths" => ["io/sample/src/main/kotlin"],
          "testPaths" => ["io/sample/src/test/kotlin"],
          "workshops" => [],
        },
      ],
    }
    File.write(File.join(root, "docs/manual/manifest.yaml"), YAML.dump(manifest))
  end
end
