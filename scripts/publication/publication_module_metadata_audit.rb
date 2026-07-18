require "json"
require "set"

module Publication
  class ModuleMetadataAudit
    Result = Struct.new(:errors, :file_count, :variant_count, :dependency_count, keyword_init: true)

    def initialize(paths)
      @paths = Array(paths).map { |path| File.expand_path(path) }.sort
    end

    def validate
      if @paths.empty?
        return Result.new(
          errors: ["no publication module metadata files found"],
          file_count: 0,
          variant_count: 0,
          dependency_count: 0,
        )
      end

      errors = []
      variant_count = 0
      dependency_count = 0

      @paths.each do |path|
        metadata = JSON.parse(File.read(path))
        Array(metadata["variants"]).each do |variant|
          variant_count += 1
          dependencies = Array(variant["dependencies"])
          dependency_count += dependencies.length
          constrained_coordinates = versioned_coordinates(variant["dependencyConstraints"])
          has_versioned_platform = dependencies.any? do |dependency|
            platform?(dependency) && versioned?(dependency)
          end

          dependencies.each do |dependency|
            next if versioned?(dependency)
            next if constrained_coordinates.include?(coordinate(dependency))
            next if has_versioned_platform && !platform?(dependency)

            errors << "#{path}: #{variant.fetch("name", "<unnamed>")}: " \
                      "missing dependency version: #{coordinate(dependency)}"
          end
        end
      rescue JSON::ParserError => error
        errors << "#{path}: invalid JSON: #{error.message.lines.first.to_s.strip}"
      end

      Result.new(
        errors: errors.sort,
        file_count: @paths.length,
        variant_count: variant_count,
        dependency_count: dependency_count,
      )
    end

    private

    def coordinate(dependency)
      "#{dependency.fetch("group", "")}:#{dependency.fetch("module", "")}"
    end

    def platform?(dependency)
      category = dependency.fetch("attributes", {}).fetch("org.gradle.category", "")
      category == "platform" || category == "enforced-platform"
    end

    def versioned?(dependency)
      dependency.fetch("version", {}).values.any? { |value| !value.to_s.strip.empty? }
    end

    def versioned_coordinates(dependencies)
      Array(dependencies).each_with_object(Set.new) do |dependency, coordinates|
        coordinates << coordinate(dependency) if versioned?(dependency)
      end
    end
  end
end
