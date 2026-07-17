#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "minitest/autorun"
require "open3"
require "pathname"
require "tmpdir"
require "yaml"
require_relative "release_diagram_contract"

class ReleaseDiagramContractTest < Minitest::Test
  PNG = "\x89PNG\r\n\x1A\nfixture".b

  def setup
    @tmp = Dir.mktmpdir("release-diagrams")
    @root = Pathname.new(@tmp)
    write(@root.join("docs/images/readme-diagrams/nested/sample.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"/>")
    write(@root.join("docs/images/readme-diagrams/nested/sample.png"), PNG, binary: true)
    write(@root.join("README.md"), "![Sample](docs/images/readme-diagrams/nested/sample.png)\n")
    git("init", "-q")
    git("config", "user.email", "test@example.com")
    git("config", "user.name", "Release Diagram Test")
    git("config", "tag.gpgSign", "false")
    git("add", ".")
    git("commit", "-qm", "release fixture")
    @release_commit = git("rev-parse", "HEAD")
    git("tag", "1.0.0")

    FileUtils.rm_rf(@root.join("docs/images/readme-diagrams"))
    write_manifest(@release_commit)
    write_pages(with_reference: true)
    write_inventory
  end

  def teardown
    FileUtils.remove_entry(@tmp)
  end

  def test_sync_links_release_asset_when_snapshot_canonical_is_absent
    contract.sync!

    page = File.read(@root.join("docs/manual/en/modules/sample.md"))
    assert_includes page, "https://raw.githubusercontent.com/example/manuals/#{@release_commit}/docs/images/readme-diagrams/nested/sample.png"
    assert_includes page, "https://github.com/example/manuals/blob/#{@release_commit}/docs/images/readme-diagrams/nested/sample.svg"
    refute File.exist?(@root.join("docs/manual/assets/readme-diagrams"))
    refute_includes File.read(@root.join("docs/manual/manifest.yaml")), "assets/readme-diagrams/"
    assert_empty contract.errors
  end

  def test_rejects_release_ref_commit_mismatch
    write_manifest("0" * 40)

    assert_includes contract.errors, "manual releaseRef 1.0.0 resolves to #{@release_commit}, expected #{'0' * 40}"
  end

  def test_reports_manual_page_without_png_reference
    write_pages(with_reference: false)

    assert_includes contract.errors, "sample: en manual page does not reference release PNG URL"
    assert_includes contract.errors, "sample: ko manual page does not reference release PNG URL"
  end

  def test_reports_manual_mirror_directory
    contract.sync!
    write(@root.join("docs/manual/assets/readme-diagrams/orphan.png"), PNG, binary: true)

    assert_includes contract.errors, "manual mirror directory still exists: docs/manual/assets/readme-diagrams"
  end

  def test_rejects_unsafe_canonical_path
    data = inventory_data
    data.fetch("diagrams").first["canonical"] = "../sample"
    write(@root.join("docs/manual/release-diagrams.yaml"), YAML.dump(data))

    assert_includes contract.errors, "sample: unsafe canonical path \"../sample\""
  end

  private

  def contract
    ReleaseDiagrams::Contract.new(root: @root, inventory_path: @root.join("docs/manual/release-diagrams.yaml"))
  end

  def inventory_data
    {
      "schemaVersion" => 1,
      "sourcePolicy" => "release-readme",
      "diagrams" => [{
        "id" => "sample",
        "canonical" => "nested/sample",
        "manualPages" => {
          "en" => "docs/manual/en/modules/sample.md",
          "ko" => "docs/manual/ko/modules/sample.md",
        },
        "releaseReadmes" => ["README.md"],
      }],
    }
  end

  def write_inventory
    write(@root.join("docs/manual/release-diagrams.yaml"), YAML.dump(inventory_data))
  end

  def write_manifest(commit)
    write(
      @root.join("docs/manual/manifest.yaml"),
      YAML.dump({
        "repository" => "example/manuals",
        "releaseRef" => "1.0.0",
        "releaseCommit" => commit,
        "overview" => {
          "assets" => [
            "assets/guide.svg",
            "assets/readme-diagrams/nested/sample.png",
            "assets/readme-diagrams/nested/sample.svg",
          ],
        },
      }),
    )
  end

  def write_pages(with_reference:)
    body = if with_reference
             "[![Sample](../../assets/readme-diagrams/nested/sample.png)](../../assets/readme-diagrams/nested/sample.svg)\n"
           else
             "# Sample\n"
           end
    write(@root.join("docs/manual/en/modules/sample.md"), body)
    write(@root.join("docs/manual/ko/modules/sample.md"), body)
  end

  def write(path, content, binary: false)
    FileUtils.mkdir_p(path.dirname)
    binary ? File.binwrite(path, content) : File.write(path, content)
  end

  def git(*arguments)
    stdout, stderr, status = Open3.capture3("git", "-C", @root.to_s, *arguments)
    raise "git #{arguments.join(' ')} failed: #{stderr}" unless status.success?
    stdout.strip
  end
end
