require "fileutils"
require "minitest/autorun"
require "tmpdir"

require_relative "publication_inventory_audit"

class PublicationInventoryAuditTest < Minitest::Test
  def test_accepts_bom_constraints_matching_published_poms
    with_inventory(%w[bluetape4k-core bluetape4k-cache], %w[bluetape4k-core bluetape4k-cache]) do |paths|
      assert_empty Publication::InventoryAudit.new(paths).errors
    end
  end

  def test_rejects_constraints_for_unpublished_artifacts
    with_inventory(%w[bluetape4k-core example-benchmark], %w[bluetape4k-core]) do |paths|
      errors = Publication::InventoryAudit.new(paths).errors
      assert_equal ["BOM constrains unpublished artifacts: example-benchmark"], errors
    end
  end

  def test_rejects_published_artifacts_missing_from_bom
    with_inventory(%w[bluetape4k-core], %w[bluetape4k-core bluetape4k-cache]) do |paths|
      errors = Publication::InventoryAudit.new(paths).errors
      assert_equal ["published artifacts missing from BOM: bluetape4k-cache"], errors
    end
  end

  private

  def with_inventory(constraints, publications)
    Dir.mktmpdir("publication-inventory-audit") do |root|
      paths = publications.map do |artifact|
        path = File.join(root, artifact, "pom-default.xml")
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, pom(artifact))
        path
      end
      bom_path = File.join(root, "bluetape4k-bom", "pom-default.xml")
      FileUtils.mkdir_p(File.dirname(bom_path))
      File.write(bom_path, bom(constraints))
      yield paths + [bom_path]
    end
  end

  def pom(artifact)
    <<~XML
      <project><groupId>io.github.bluetape4k</groupId><artifactId>#{artifact}</artifactId><version>1.12.1</version></project>
    XML
  end

  def bom(constraints)
    dependencies = constraints.map do |artifact|
      "<dependency><groupId>io.github.bluetape4k</groupId><artifactId>#{artifact}</artifactId><version>1.12.1</version></dependency>"
    end.join
    <<~XML
      <project><groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-bom</artifactId><version>1.12.1</version>
      <dependencyManagement><dependencies>#{dependencies}</dependencies></dependencyManagement></project>
    XML
  end
end
