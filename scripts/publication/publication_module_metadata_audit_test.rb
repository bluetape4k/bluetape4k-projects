require "fileutils"
require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "publication_module_metadata_audit"

class PublicationModuleMetadataAuditTest < Minitest::Test
  def test_accepts_dependencies_with_explicit_versions
    with_metadata(
      variants: [
        variant(
          "apiElements",
          dependencies: [dependency("org.example", "example-core", "1.2.3")],
        ),
      ],
    ) do |path|
      result = Publication::ModuleMetadataAudit.new([path]).validate

      assert_empty result.errors
      assert_equal 1, result.file_count
      assert_equal 1, result.variant_count
      assert_equal 1, result.dependency_count
    end
  end

  def test_rejects_a_versionless_dependency_when_only_another_variant_has_a_platform
    with_metadata(
      variants: [
        variant(
          "apiElements",
          dependencies: [dependency("tools.jackson.core", "jackson-databind")],
        ),
        variant(
          "runtimeElements",
          dependencies: [
            platform_dependency("tools.jackson", "jackson-bom", "3.2.0"),
            dependency("tools.jackson.core", "jackson-databind"),
          ],
        ),
      ],
    ) do |path|
      errors = Publication::ModuleMetadataAudit.new([path]).validate.errors

      assert_equal 1, errors.length
      assert_includes errors.first, "apiElements: missing dependency version: tools.jackson.core:jackson-databind"
    end
  end

  def test_accepts_a_versionless_dependency_with_a_versioned_platform_in_the_same_variant
    with_metadata(
      variants: [
        variant(
          "apiElements",
          dependencies: [
            platform_dependency("tools.jackson", "jackson-bom", "3.2.0"),
            dependency("tools.jackson.core", "jackson-databind"),
          ],
        ),
      ],
    ) do |path|
      assert_empty Publication::ModuleMetadataAudit.new([path]).validate.errors
    end
  end

  def test_accepts_a_versionless_dependency_with_a_matching_constraint
    constrained = dependency("org.example", "example-core", "1.2.3")
    with_metadata(
      variants: [
        variant(
          "apiElements",
          dependencies: [dependency("org.example", "example-core")],
          dependency_constraints: [constrained],
        ),
      ],
    ) do |path|
      assert_empty Publication::ModuleMetadataAudit.new([path]).validate.errors
    end
  end

  def test_rejects_a_versionless_platform_even_when_another_platform_is_versioned
    with_metadata(
      variants: [
        variant(
          "apiElements",
          dependencies: [
            platform_dependency("tools.jackson", "jackson-bom", "3.2.0"),
            dependency("org.springframework.boot", "spring-boot-dependencies").merge(
              "attributes" => { "org.gradle.category" => "platform" },
            ),
          ],
        ),
      ],
    ) do |path|
      errors = Publication::ModuleMetadataAudit.new([path]).validate.errors

      assert_equal 1, errors.length
      assert_includes errors.first,
                      "apiElements: missing dependency version: org.springframework.boot:spring-boot-dependencies"
    end
  end

  def test_fails_closed_when_no_module_metadata_files_exist
    result = Publication::ModuleMetadataAudit.new([]).validate

    assert_equal ["no publication module metadata files found"], result.errors
  end

  private

  def dependency(group, module_name, version = nil)
    value = { "group" => group, "module" => module_name }
    value["version"] = { "requires" => version } if version
    value
  end

  def platform_dependency(group, module_name, version)
    dependency(group, module_name, version).merge(
      "attributes" => { "org.gradle.category" => "platform" },
    )
  end

  def variant(name, dependencies:, dependency_constraints: [])
    {
      "name" => name,
      "attributes" => { "org.gradle.usage" => name == "apiElements" ? "java-api" : "java-runtime" },
      "dependencies" => dependencies,
      "dependencyConstraints" => dependency_constraints,
    }
  end

  def with_metadata(variants:)
    Dir.mktmpdir("publication-module-metadata-audit") do |root|
      path = File.join(root, "module.json")
      File.write(path, JSON.pretty_generate("formatVersion" => "1.1", "variants" => variants))
      yield path
    end
  end
end
