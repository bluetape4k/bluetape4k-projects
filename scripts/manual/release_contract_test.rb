require "fileutils"
require "minitest/autorun"
require "open3"
require "rbconfig"
require "tmpdir"

require_relative "release_contract"

class ReleaseContractTest < Minitest::Test
  RELEASE_SHA = "a" * 40
  VALIDATOR_SCRIPT = File.expand_path("validate_release_manuals.rb", __dir__)

  def test_reports_a_manual_path_that_is_missing_from_the_tag
    with_repository do |root, sha|
      write_manual(root, "[Missing](../../../../src/missing.kt)")

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:1: release path not found: src/missing.kt",
      ], errors
    end
  end

  def test_rejects_a_link_that_traverses_outside_the_repository
    with_repository do |root, sha|
      write_manual(root, "[Unsafe](../../../../../outside.kt)")

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:1: unsafe release path: ../../../../../outside.kt",
      ], errors
    end
  end

  def test_rejects_a_semver_branch_when_no_tag_exists
    with_repository(tag: nil) do |root, sha|
      git(root, "branch", "1.11.0")

      errors = validator(root, sha).errors

      assert_equal ["release tag not found: refs/tags/1.11.0"], errors
    end
  end

  def test_rejects_a_local_tag_that_does_not_match_the_expected_sha
    with_repository do |root, _sha|
      errors = validator(root, "b" * 40).errors

      assert_equal [
        "release tag 1.11.0 resolves to #{git(root, "rev-parse", "refs/tags/1.11.0^{commit}").strip}, expected #{"b" * 40}",
      ], errors
    end
  end

  def test_reports_every_missing_link_on_the_same_line
    with_repository do |root, sha|
      write_manual(
        root,
        "[One](../../../../src/one.kt) and [Two](../../../../src/two.kt)",
      )

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:1: release path not found: src/one.kt",
        "docs/manual/en/modules/sample.md:1: release path not found: src/two.kt",
      ], errors
    end
  end

  def test_reports_a_multiline_missing_link_at_the_links_start_line
    with_repository do |root, sha|
      write_manual(root, <<~MARKDOWN)
        Plain text
        [Split](
        ../../../../src/not-at-release.kt
        )
      MARKDOWN

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:2: release path not found: src/not-at-release.kt",
      ], errors
    end
  end

  def test_reports_a_multiline_unsafe_link_at_the_links_start_line
    with_repository do |root, sha|
      write_manual(root, <<~MARKDOWN)
        Plain text
        Another line
        [Escape](
        ../../../../../outside.kt
        )
      MARKDOWN

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:3: unsafe release path: ../../../../../outside.kt",
      ], errors
    end
  end

  def test_orders_errors_by_file_and_numeric_line
    with_repository do |root, sha|
      write_manual(
        root,
        (["plain", "[Two](../../../../src/two.kt)"] + ["plain"] * 7 +
          ["[Ten](../../../../src/ten.kt)"]).join("\n"),
      )

      errors = validator(root, sha).errors

      assert_equal [
        "docs/manual/en/modules/sample.md:2: release path not found: src/two.kt",
        "docs/manual/en/modules/sample.md:10: release path not found: src/ten.kt",
      ], errors
    end
  end

  def test_inventories_the_release_commit_once_with_a_bounded_git_command
    calls = []
    runner = lambda do |arguments|
      calls << arguments
      case arguments
      when ["rev-parse", "--verify", "refs/tags/v1.11.0^{commit}"]
        [RELEASE_SHA + "\n", true]
      when ["ls-tree", "-r", "--name-only", RELEASE_SHA]
        ["src/present.kt\n", true]
      else
        ["", false]
      end
    end

    Dir.mktmpdir("release-contract") do |root|
      write_manual(root, <<~MARKDOWN)
        [Present](../../../../src/present.kt)
        [Also present](../../../../src/present.kt)
      MARKDOWN

      contract = ManualDocs::ReleaseContract.new(
        repository_root: root,
        tag: "v1.11.0",
        expected_sha: RELEASE_SHA,
        git_runner: runner,
      )

      assert_empty contract.errors
    end

    assert_equal 1, calls.count { |arguments| arguments.first == "ls-tree" }
    assert_includes calls, ["ls-tree", "-r", "--name-only", RELEASE_SHA]
  end

  def test_exposes_the_number_of_checked_links
    with_repository do |root, sha|
      write_manual(root, <<~MARKDOWN)
        [Present](../../../../src/present.kt)
        [Present again](../../../../src/present.kt)
      MARKDOWN
      contract = validator(root, sha)

      assert_respond_to contract, :validate
      result = contract.validate
      assert_empty result.errors
      assert_equal 2, result.checked_count
    end
  end

  def test_rejects_a_manual_set_without_repository_links
    with_repository do |root, sha|
      write_manual(root, "No repository source links.\n")
      contract = validator(root, sha)

      assert_respond_to contract, :validate
      result = contract.validate
      assert_equal ["no repository-relative manual links found"], result.errors
      assert_equal 0, result.checked_count
    end
  end

  def test_cli_reports_checked_and_missing_counts
    with_repository do |root, sha|
      write_manual(root, <<~MARKDOWN)
        [Present](../../../../src/present.kt)
        [Present again](../../../../src/present.kt)
      MARKDOWN

      stdout, stderr, status = Open3.capture3(
        RbConfig.ruby,
        VALIDATOR_SCRIPT,
        "1.11.0",
        sha,
        chdir: root,
      )

      assert status.success?, stderr
      assert_equal "Release manuals are compatible with 1.11.0 (#{sha}): 2 checked, 0 missing.\n", stdout
      assert_empty stderr
    end
  end

  def test_accepts_a_release_directory_when_the_inventory_contains_descendants
    runner = lambda do |arguments|
      case arguments.first
      when "rev-parse"
        [RELEASE_SHA + "\n", true]
      when "ls-tree"
        ["src/package/Present.kt\n", true]
      end
    end

    Dir.mktmpdir("release-contract") do |root|
      write_manual(root, "[Package](../../../../src/package)")

      contract = ManualDocs::ReleaseContract.new(
        repository_root: root,
        tag: "1.11.0",
        expected_sha: RELEASE_SHA,
        git_runner: runner,
      )

      assert_empty contract.errors
    end
  end

  def test_requires_a_semver_tag_and_a_full_sha
    Dir.mktmpdir("release-contract") do |root|
      contract = ManualDocs::ReleaseContract.new(
        repository_root: root,
        tag: "release-1.11",
        expected_sha: "abc123",
      )

      assert_equal [
        "release tag must match v?MAJOR.MINOR.PATCH: release-1.11",
        "expected SHA must be a 40-character hexadecimal commit id: abc123",
      ], contract.errors
    end
  end

  private

  def validator(root, expected_sha)
    ManualDocs::ReleaseContract.new(
      repository_root: root,
      tag: "1.11.0",
      expected_sha: expected_sha,
    )
  end

  def with_repository(tag: "1.11.0")
    Dir.mktmpdir("release-contract-repository") do |root|
      git(root, "init", "--quiet")
      git(root, "config", "user.email", "manuals@example.com")
      git(root, "config", "user.name", "Manual Tests")
      write_file(root, "src/present.kt", "class Present\n")
      git(root, "add", "src/present.kt")
      git(root, "commit", "--quiet", "-m", "fixture")
      sha = git(root, "rev-parse", "HEAD").strip
      git(root, "tag", "--no-sign", tag) if tag
      yield root, sha
    end
  end

  def write_manual(root, content)
    write_file(root, "docs/manual/en/modules/sample.md", content)
  end

  def write_file(root, path, content)
    absolute = File.join(root, path)
    FileUtils.mkdir_p(File.dirname(absolute))
    File.write(absolute, content)
  end

  def git(root, *arguments)
    output = IO.popen(["git", "-C", root, *arguments], err: [:child, :out], &:read)
    raise "git #{arguments.join(" ")} failed: #{output}" unless $?.success?

    output
  end
end
