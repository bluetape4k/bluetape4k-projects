require "fileutils"
require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "manual_contract"

class ValidateManualsTest < Minitest::Test
  INVENTORY = [
    {
      "gradlePath" => ":sample",
      "projectName" => "sample",
      "sourceDir" => "io/sample",
      "kind" => "library",
    },
  ].freeze

  def test_exposes_the_manual_contract_constants
    assert_equal %w[
      problem when-to-use coordinates concepts quick-start api-by-task
      patterns integrations configuration failures operations testing
      workshops limitations sources
    ], ManualDocs::REQUIRED_SECTIONS
    assert_equal %w[library example benchmark], ManualDocs::VALID_KINDS
    assert_equal %w[
      foundation concurrency io caching data messaging web spring operations
      testing utilities examples
    ], ManualDocs::VALID_GROUPS
  end

  def test_reports_missing_locale_and_required_sections
    validator = validator_for("missing-ko")

    assert_includes validator.errors, "sample: missing Korean document"
    assert validator.errors.any? { |error| error.include?("required section") }
  end

  def test_accepts_complete_bilingual_module
    assert_empty validator_for("valid").errors
  end

  def test_accepts_complete_bilingual_chapters_and_paired_assets
    assert_empty validator_for("valid").errors
  end

  def test_accepts_paired_overview_assets
    with_fixture("valid") do |root|
      asset_root = File.join(root, "docs/manual/assets/overview")
      FileUtils.mkdir_p(asset_root)
      File.write(File.join(asset_root, "map.svg"), '<svg xmlns="http://www.w3.org/2000/svg"/>')
      File.binwrite(File.join(asset_root, "map.png"), "png")
      manifest = load_manifest(root)
      manifest["overview"] = { "assets" => ["assets/overview/map.svg", "assets/overview/map.png"] }
      write_manifest(root, manifest)

      assert_empty validator(root).errors
    end
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
      File.write(
        File.join(root, "docs/manual/assets/sample/orphan.svg"),
        '<svg xmlns="http://www.w3.org/2000/svg"/>',
      )

      errors = validator(root).errors

      assert_includes errors, "sample/chapter-one: missing Korean document"
      assert_includes errors, "sample: missing paired asset assets/sample/model.png"
      assert_includes errors, "manual assets: orphan asset assets/sample/orphan.svg"
    end
  end

  def test_reports_unsafe_and_missing_manual_references
    with_fixture("valid") do |root|
      english = File.join(root, "docs/manual/en/modules/sample/chapter-one.md")
      File.write(
        english,
        File.read(english) + "\n![Escape](../../../../../../outside.png)\n[Missing](missing.md)\n",
      )

      errors = validator(root).errors

      assert errors.any? { |error| error.include?("unsafe Markdown reference") }
      assert errors.any? { |error| error.include?("missing Markdown reference") }
    end
  end

  def test_reports_inventory_drift_duplicates_and_invalid_kind
    with_fixture("valid") do |root|
      manifest = load_manifest(root)
      original = manifest["modules"].first
      manifest["modules"] << deep_copy(original).merge(
        "kind" => "unknown",
        "sourceDir" => "io/other",
      )
      manifest["modules"] << deep_copy(original).merge(
        "id" => "extra",
        "gradlePath" => ":extra",
        "en" => "en/modules/extra.md",
        "ko" => "ko/modules/extra.md",
      )
      write_manifest(root, manifest)

      errors = validator(root).errors

      assert errors.any? { |error| error.include?("duplicate id") }
      assert errors.any? { |error| error.include?("duplicate gradlePath") }
      assert errors.any? { |error| error.include?("invalid kind") }
      assert errors.any? { |error| error.include?("sourceDir does not match inventory") }
      assert errors.any? { |error| error.include?("not present in inventory") }
    end
  end

  def test_reports_manual_id_mismatch
    with_fixture("valid") do |root|
      english = File.join(root, "docs/manual/en/modules/sample.md")
      File.write(english, File.read(english).sub("manualId: sample", "manualId: other"))

      assert_includes validator(root).errors, "sample: English document manualId must be sample"
    end
  end

  def test_reports_navigation_metadata_mismatch
    with_fixture("valid") do |root|
      english = File.join(root, "docs/manual/en/modules/sample.md")
      korean = File.join(root, "docs/manual/ko/modules/sample.md")
      File.write(
        english,
        File.read(english).sub('title: "Sample utilities"', 'title: "Wrong title"'),
      )
      File.write(korean, File.read(korean).sub("learningOrder: 10", "learningOrder: 20"))

      errors = validator(root).errors

      assert_includes errors, "sample: English document title must match manifest title"
      assert_includes errors, "sample: Korean document learningOrder must be 10"
    end
  end

  def test_reports_invalid_group_order_and_duplicate_learning_order
    with_fixture("valid") do |root|
      manifest = load_manifest(root)
      original = manifest["modules"].first
      manifest["modules"].first["group"] = "cache"
      manifest["modules"] << deep_copy(original).merge(
        "id" => "extra",
        "gradlePath" => ":extra",
        "sourceDir" => "io/extra",
        "en" => "en/modules/extra.md",
        "ko" => "ko/modules/extra.md",
      )
      write_manifest(root, manifest)

      errors = validator(root).errors

      assert_includes errors, "sample: invalid group \"cache\""
      assert_includes errors, "manifest: duplicate learningOrder 10"
    end
  end

  def test_rejects_generic_module_titles
    with_fixture("valid") do |root|
      manifest = load_manifest(root)
      manifest["modules"].first["title"] = {
        "en" => "Module sample",
        "ko" => "sample",
      }
      write_manifest(root, manifest)

      errors = validator(root).errors

      assert_includes errors, "sample: en title must describe the module's function"
      assert_includes errors, "sample: ko title must describe the module's function"
    end
  end

  def test_reports_unsafe_and_missing_repository_paths
    with_fixture("valid") do |root|
      manifest = load_manifest(root)
      entry = manifest["modules"].first
      entry["sourcePaths"] = ["../outside", "/absolute"]
      entry["testPaths"] = ["io/sample/src/test/missing"]
      entry["workshops"] = ["workshops/missing"]
      write_manifest(root, manifest)

      errors = validator(root).errors

      assert errors.any? { |error| error.include?("unsafe sourcePaths path") }
      assert errors.any? { |error| error.include?("missing testPaths path") }
      assert errors.any? { |error| error.include?("missing workshops path") }
    end
  end

  def test_rejects_a_relative_symlink_that_escapes_the_repository
    with_fixture("valid") do |root|
      Dir.mktmpdir("outside-manual-repository") do |outside|
        link = File.join(root, "io/sample/outside-link")
        File.symlink(outside, link)
        manifest = load_manifest(root)
        manifest["modules"].first["sourcePaths"] = ["io/sample/outside-link"]
        write_manifest(root, manifest)

        assert validator(root).errors.any? { |error| error.include?("unsafe sourcePaths path") }
      end
    end
  end

  def test_reports_missing_inventory_modules_and_inventory_duplicates
    root = fixture("valid")
    inventory = INVENTORY + [
      INVENTORY.first.merge("projectName" => "duplicate-name"),
      {
        "gradlePath" => ":missing",
        "projectName" => "missing",
        "sourceDir" => "io/missing",
        "kind" => "library",
      },
    ]

    errors = ManualDocs::Validator.new(
      inventory: inventory,
      manifest_path: File.join(root, "docs/manual/manifest.yaml"),
      repository_root: root,
    ).errors

    assert errors.any? { |error| error.include?("duplicate gradlePath") }
    assert_includes errors, "missing: missing from manifest"
  end

  def test_reports_missing_or_invalid_manifest_shape
    Dir.mktmpdir("manual-validator") do |root|
      missing = validator(root).errors
      assert_equal ["manual manifest not found: docs/manual/manifest.yaml"], missing

      FileUtils.mkdir_p(File.join(root, "docs/manual"))
      File.write(File.join(root, "docs/manual/manifest.yaml"), "schemaVersion: 1\nmodules: invalid\n")
      errors = validator(root).errors
      assert_includes errors, "manual manifest schemaVersion must be 2"
      assert_includes errors, "manual manifest modules must be an array"
    end
  end

  def test_returns_errors_in_sorted_order
    errors = validator_for("missing-ko").errors
    assert_equal errors.sort, errors
  end

  private

  def validator_for(name)
    root = fixture(name)
    validator(root)
  end

  def validator(root)
    ManualDocs::Validator.new(
      inventory: INVENTORY,
      manifest_path: File.join(root, "docs/manual/manifest.yaml"),
      repository_root: root,
    )
  end

  def fixture(name)
    File.expand_path("test-fixtures/#{name}", __dir__)
  end

  def with_fixture(name)
    Dir.mktmpdir("manual-validator") do |root|
      FileUtils.cp_r("#{fixture(name)}/.", root)
      yield root
    end
  end

  def load_manifest(root)
    YAML.safe_load(File.read(File.join(root, "docs/manual/manifest.yaml")))
  end

  def write_manifest(root, manifest)
    File.write(File.join(root, "docs/manual/manifest.yaml"), YAML.dump(manifest))
  end

  def deep_copy(value)
    Marshal.load(Marshal.dump(value))
  end
end
