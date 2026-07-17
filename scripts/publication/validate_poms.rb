#!/usr/bin/env ruby

require "fileutils"
require "open3"
require "tmpdir"

require_relative "publication_pom_audit"

paths = if ARGV.empty?
          Dir.glob("**/build/publications/*/pom-default.xml")
            .reject { |path| path.start_with?(".worktrees/") }
        else
          ARGV
        end

result = Publication::PomAudit.new(paths).validate
unless result.errors.empty?
  warn(result.errors.join("\n"))
  abort("publication-poms: failures=#{result.errors.length} files=#{result.file_count} dependencies=#{result.dependency_count}")
end

settings = File.expand_path("maven-settings.xml", __dir__)

Dir.mktmpdir("publication-pom-reactor") do |reactor|
  modules = paths.sort.each_with_index.map do |path, index|
    module_name = format("module-%03d", index + 1)
    module_dir = File.join(reactor, module_name)
    FileUtils.mkdir_p(module_dir)
    FileUtils.cp(File.expand_path(path), File.join(module_dir, "pom.xml"))
    module_name
  end

  File.write(
    File.join(reactor, "pom.xml"),
    <<~XML,
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>io.github.bluetape4k.validation</groupId>
        <artifactId>publication-pom-reactor</artifactId>
        <version>1</version>
        <packaging>pom</packaging>
        <modules>
      #{modules.map { |name| "    <module>#{name}</module>" }.join("\n")}
        </modules>
      </project>
    XML
  )

  command = [
    ENV.fetch("MAVEN_COMMAND", "mvn"),
    "-U", "-q", "-s", settings,
    "-f", File.join(reactor, "pom.xml"),
    "validate",
  ]
  stdout, stderr, status = Open3.capture3(*command)
  unless status.success?
    diagnostics = (stdout + stderr).lines.grep(/\[ERROR\]/).first(40)
    warn(diagnostics.empty? ? stdout + stderr : diagnostics.join)
    abort("publication-poms: Maven effective-model validation failed")
  end
rescue Errno::ENOENT => error
  abort("publication-poms: Maven executable not found: #{error.message}")
end

puts "publication-poms: failures=0 files=#{result.file_count} dependencies=#{result.dependency_count} maven_models=#{result.file_count}"
