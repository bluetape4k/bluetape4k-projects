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
      assert_includes english, "SampleClient"
      assert_includes english, "SampleClientTest"
      refute_includes korean, ", and "
      assert_equal REQUIRED_SECTIONS, english.scan(/\{#([a-z0-9-]+)\}/).flatten
      assert_equal REQUIRED_SECTIONS, korean.scan(/\{#([a-z0-9-]+)\}/).flatten

      File.write(File.join(root, "docs/manual/en/modules/sample.md"), "preserve me\n")
      generator.generate(missing_only: true)
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
    File.write(File.join(root, "io/sample/README.md"), "# Sample module\n\nEnglish sample client for remote calls.\n")
    File.write(File.join(root, "io/sample/README.ko.md"), "# Sample 모듈\n\n원격 호출을 위한 sample client입니다.\n")
    File.write(File.join(root, "io/sample/build.gradle.kts"), "dependencies {\n    api(libs.sample.api)\n}\n")
    File.write(File.join(root, "io/sample/src/main/kotlin/io/example/SampleClient.kt"), "class SampleClient\n")
    File.write(File.join(root, "io/sample/src/test/kotlin/io/example/SampleClientTest.kt"), "class SampleClientTest\n")
    manifest = {
      "schemaVersion" => 1,
      "modules" => [
        {
          "id" => "sample",
          "gradlePath" => ":sample",
          "sourceDir" => "io/sample",
          "kind" => "library",
          "group" => "io",
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
