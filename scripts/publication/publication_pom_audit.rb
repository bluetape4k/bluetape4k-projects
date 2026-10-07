require "rexml/document"
require "set"

module Publication
  class PomAudit
    Result = Struct.new(:errors, :file_count, :dependency_count, keyword_init: true)

    def initialize(paths)
      @paths = Array(paths).map { |path| File.expand_path(path) }.sort
    end

    def validate
      return Result.new(errors: ["no publication POM files found"], file_count: 0, dependency_count: 0) if @paths.empty?

      errors = []
      dependency_count = 0

      @paths.each do |path|
        document = REXML::Document.new(File.read(path))
        managed_dependencies = REXML::XPath.match(
          document,
          "/project/dependencyManagement/dependencies/dependency",
        )
        if document.root.elements["artifactId"]&.text.to_s.strip == "bluetape4k-testcontainers"
          errors.concat(testcontainers_netty_version_errors(path, managed_dependencies))
        end

        managed_coordinates = Set.new
        managed_dependencies.each do |dependency|
          managed_coordinate = dependency_management_coordinate(dependency)
          unless managed_coordinates.add?(managed_coordinate)
            errors << "#{path}: duplicate dependencyManagement dependency: #{managed_coordinate}"
          end
        end
        managed_versions = managed_dependencies.each_with_object(Set.new) do |dependency, result|
          version = dependency.elements["version"]&.text.to_s.strip
          result << dependency_management_coordinate(dependency) unless version.empty?
        end
        has_versioned_bom_import = managed_dependencies.any? do |dependency|
          version = dependency.elements["version"]&.text.to_s.strip
          type = dependency.elements["type"]&.text.to_s.strip
          scope = dependency.elements["scope"]&.text.to_s.strip
          !version.empty? && type == "pom" && scope == "import"
        end

        managed_dependencies.each do |dependency|
          dependency_count += 1
          next unless dependency.elements["version"]&.text.to_s.strip.empty?

          errors << "#{path}: missing dependency version: #{coordinate(dependency)}"
        end

        REXML::XPath.each(document, "/project/dependencies/dependency") do |dependency|
          dependency_count += 1
          version = dependency.elements["version"]&.text.to_s.strip
          dependency_coordinate = coordinate(dependency)
          next unless version.empty?
          next if managed_versions.include?(dependency_management_coordinate(dependency))
          next if has_versioned_bom_import

          errors << "#{path}: missing dependency version: #{dependency_coordinate}"
        end
      rescue REXML::ParseException => error
        errors << "#{path}: invalid XML: #{error.message.lines.first.to_s.strip}"
      end

      Result.new(errors: errors.sort, file_count: @paths.length, dependency_count: dependency_count)
    end

    private

    def coordinate(dependency)
      group = dependency.elements["groupId"]&.text.to_s.strip
      artifact = dependency.elements["artifactId"]&.text.to_s.strip
      "#{group}:#{artifact}"
    end

    def dependency_management_coordinate(dependency)
      group = dependency.elements["groupId"]&.text.to_s.strip
      artifact = dependency.elements["artifactId"]&.text.to_s.strip
      type = dependency.elements["type"]&.text.to_s.strip
      type = "jar" if type.empty?
      classifier = dependency.elements["classifier"]&.text.to_s.strip
      [group, artifact, type, classifier].join(":")
    end

    def testcontainers_netty_version_errors(path, managed_dependencies)
      netty_bom = managed_dependencies.find do |dependency|
        dependency.elements["groupId"]&.text.to_s.strip == "io.netty" &&
          dependency.elements["artifactId"]&.text.to_s.strip == "netty-bom" &&
          dependency.elements["type"]&.text.to_s.strip == "pom" &&
          dependency.elements["scope"]&.text.to_s.strip == "import"
      end
      unless netty_bom
        return ["#{path}: Testcontainers POM missing io.netty:netty-bom import"]
      end

      bom_version = netty_bom.elements["version"]&.text.to_s.strip
      managed_dependencies.each_with_object([]) do |dependency, errors|
        group = dependency.elements["groupId"]&.text.to_s.strip
        artifact = dependency.elements["artifactId"]&.text.to_s.strip
        version = dependency.elements["version"]&.text.to_s.strip
        next unless group == "io.netty" && artifact != "netty-bom"
        next if artifact.start_with?("netty-tcnative")
        next if version.empty? || version == bom_version

        errors << "#{path}: Netty version #{version} conflicts with imported netty-bom #{bom_version}: io.netty:#{artifact}"
      end
    end
  end
end
