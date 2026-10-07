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

  def test_rejects_a_versionless_dependency_managed_only_with_a_different_type
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
          <version>1.2.3</version><type>war</type>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
        </dependency></dependencies>
      </project>
    XML
      errors = Publication::PomAudit.new([path]).validate.errors
      assert_equal 1, errors.length
      assert errors.first.end_with?("missing dependency version: org.example:example-core")
    end
  end

  def test_rejects_a_versionless_dependency_managed_only_with_a_classifier
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
          <version>1.2.3</version><type>jar</type><classifier>sources</classifier>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
        </dependency></dependencies>
      </project>
    XML
      errors = Publication::PomAudit.new([path]).validate.errors
      assert_equal 1, errors.length
      assert errors.first.end_with?("missing dependency version: org.example:example-core")
    end
  end

  def test_matches_explicit_jar_and_empty_classifier_to_default_coordinates
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.example</groupId><artifactId>example-core</artifactId>
          <version>1.2.3</version><type>jar</type><classifier></classifier>
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

  def test_rejects_duplicate_dependency_management_coordinates
    with_pom(<<~XML) do |path|
      <project>
        <dependencyManagement><dependencies>
          <dependency>
            <groupId>io.projectreactor</groupId><artifactId>reactor-bom</artifactId>
            <version>2025.0.7</version><type>pom</type><scope>import</scope>
          </dependency>
          <dependency>
            <groupId>io.projectreactor</groupId><artifactId>reactor-bom</artifactId>
            <version>2025.0.7</version><type>pom</type><scope>import</scope>
          </dependency>
        </dependencies></dependencyManagement>
      </project>
    XML
      errors = Publication::PomAudit.new([path]).validate.errors
      assert_equal 1, errors.length
      assert errors.first.end_with?(
        "duplicate dependencyManagement dependency: io.projectreactor:reactor-bom:pom:",
      )
    end
  end

  def test_rejects_testcontainers_netty_versions_that_conflict_with_the_imported_bom
    with_pom(<<~XML) do |path|
      <project>
        <groupId>io.bluetape4k</groupId><artifactId>bluetape4k-testcontainers</artifactId>
        <dependencyManagement><dependencies>
          <dependency>
            <groupId>io.netty</groupId><artifactId>netty-bom</artifactId><version>4.1.136.Final</version>
            <type>pom</type><scope>import</scope>
          </dependency>
          <dependency>
            <groupId>io.netty</groupId><artifactId>netty-resolver</artifactId><version>4.2.17.Final</version>
          </dependency>
        </dependencies></dependencyManagement>
      </project>
    XML
      errors = Publication::PomAudit.new([path]).validate.errors
      assert_equal 1, errors.length
      assert errors.first.end_with?("Netty version 4.2.17.Final conflicts with imported netty-bom 4.1.136.Final: io.netty:netty-resolver")
    end
  end

  def test_accepts_testcontainers_netty_versions_aligned_with_the_imported_bom
    with_pom(<<~XML) do |path|
      <project>
        <groupId>io.bluetape4k</groupId><artifactId>bluetape4k-testcontainers</artifactId>
        <dependencyManagement><dependencies>
          <dependency>
            <groupId>io.netty</groupId><artifactId>netty-bom</artifactId><version>4.1.136.Final</version>
            <type>pom</type><scope>import</scope>
          </dependency>
          <dependency>
            <groupId>io.netty</groupId><artifactId>netty-resolver</artifactId><version>4.1.136.Final</version>
          </dependency>
          <dependency>
            <groupId>io.netty</groupId><artifactId>netty-tcnative-classes</artifactId><version>2.0.83.Final</version>
          </dependency>
        </dependencies></dependencyManagement>
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
