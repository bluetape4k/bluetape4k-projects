require "fileutils"
require "minitest/autorun"
require "tmpdir"

require_relative "sync_navigation_metadata"

class SyncNavigationMetadataTest < Minitest::Test
  def test_synchronizes_localized_titles_groups_orders_and_h1
    Dir.mktmpdir("manual-navigation-metadata") do |root|
      fixture = File.expand_path("test-fixtures/valid/docs/manual", __dir__)
      manual_root = File.join(root, "docs/manual")
      FileUtils.mkdir_p(File.dirname(manual_root))
      FileUtils.cp_r(fixture, manual_root)
      english = File.join(manual_root, "en/modules/sample.md")
      File.write(
        english,
        File.read(english)
          .sub("title: Sample utilities", "title: Old title")
          .sub("group: io", "group: cache")
          .sub("learningOrder: 10", "learningOrder: 99")
          .sub("# Sample utilities", "# Old title"),
      )

      sync = ManualDocs::NavigationMetadataSync.new(
        manifest_path: File.join(manual_root, "manifest.yaml"),
      )
      refute sync.current?
      assert_equal 1, sync.changes.length

      sync.write

      assert ManualDocs::NavigationMetadataSync.new(
        manifest_path: File.join(manual_root, "manifest.yaml"),
      ).current?
      content = File.read(english)
      assert_includes content, "title: \"Sample utilities\""
      assert_includes content, "group: io"
      assert_includes content, "learningOrder: 10"
      assert_includes content, "# Sample utilities"
    end
  end
end
