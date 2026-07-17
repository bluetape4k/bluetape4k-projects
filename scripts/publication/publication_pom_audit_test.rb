require "fileutils"
require "minitest/autorun"
require "tmpdir"

require_relative "publication_pom_audit"

class PublicationPomAuditTest < Minitest::Test
  def test_accepts_dependencies_with_explicit_versions
    with_pom(<<~XML) do |path|
      <project><dependencies><dependency>
        <groupId>org.example</groupId><artifactId>example-core</artifactId><version>1.2.3</version>
      </dependency></dependencies></project>
    XML
      result = Publication::PomAudit.new([path]).validate
      assert_empty result.errors
      assert_equal 1, result.file_count
      assert_equal 1, result.dependency_count
    end
  end

  def test_rejects_versionless_regular_and_managed_dependencies
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-bom</artifactId>
          <type>pom</type><scope>import</scope>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
        </dependency></dependencies>
      </project>
    XML
      errors = Publication::PomAudit.new([path]).validate.errors
      assert_equal 2, errors.length
      assert errors.any? { |error| error.end_with?("missing dependency version: org.example:example-bom") }
      assert errors.any? { |error| error.end_with?("missing dependency version: org.example:example-core") }
    end
  end

  def test_accepts_a_versionless_dependency_managed_by_the_same_pom
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId><version>1.2.3</version>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
        </dependency></dependencies>
      </project>
    XML
      assert_empty Publication::PomAudit.new([path]).validate.errors
    end
  end

  def test_accepts_a_versionless_dependency_when_a_versioned_bom_is_imported
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-bom</artifactId><version>1.2.3</version>
          <type>pom</type><scope>import</scope>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
        </dependency></dependencies>
      </project>
    XML
      assert_empty Publication::PomAudit.new([path]).validate.errors
    end
  end

  def test_fails_closed_when_no_publication_poms_exist
    result = Publication::PomAudit.new([]).validate
    assert_equal ["no publication POM files found"], result.errors
  end

  private

  def with_pom(content)
    Dir.mktmpdir("publication-pom-audit") do |root|
      path = File.join(root, "pom-default.xml")
      File.write(path, content)
      yield path
    end
  end
end
