require "minitest/autorun"
require "open3"
require "rbconfig"
require "tmpdir"

class PublicationPomIntegrationTest < Minitest::Test
  def test_maven_rejects_a_dependency_not_managed_by_the_imported_bom
    with_pom(<<~XML) do |path|
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.example</groupId>
        <artifactId>unmanaged-dependency</artifactId>
        <version>1</version>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.junit</groupId><artifactId>junit-bom</artifactId><version>5.10.2</version>
          <type>pom</type><scope>import</scope>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId>
        </dependency></dependencies>
      </project>
    XML
      output, status = validate(path)
      refute status.success?
      assert_includes output, "'dependencies.dependency.version' for org.slf4j:slf4j-api:jar is missing"
    end
  end

  def test_maven_accepts_a_dependency_managed_by_the_imported_bom
    with_pom(<<~XML) do |path|
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.example</groupId>
        <artifactId>managed-dependency</artifactId>
        <version>1</version>
        <dependencyManagement><dependencies><dependency>
          <groupId>org.junit</groupId><artifactId>junit-bom</artifactId><version>5.10.2</version>
          <type>pom</type><scope>import</scope>
        </dependency></dependencies></dependencyManagement>
        <dependencies><dependency>
          <groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter-api</artifactId>
        </dependency></dependencies>
      </project>
    XML
      output, status = validate(path)
      assert status.success?, output
      assert_includes output, "maven_models=1"
    end
  end

  private

  def validate(path)
    stdout, stderr, status = Open3.capture3(
      RbConfig.ruby,
      File.expand_path("validate_poms.rb", __dir__),
      path,
    )
    [stdout + stderr, status]
  end

  def with_pom(content)
    Dir.mktmpdir("publication-pom-integration") do |root|
      path = File.join(root, "pom.xml")
      File.write(path, content)
      yield path
    end
  end
end
