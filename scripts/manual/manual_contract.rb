require "pathname"
require "yaml"

module ManualDocs
  REQUIRED_SECTIONS = %w[
    problem when-to-use coordinates concepts quick-start api-by-task
    patterns integrations configuration failures operations testing
    workshops limitations sources
  ].freeze

  VALID_KINDS = %w[library example benchmark].freeze

  class Validator
    REQUIRED_MODULE_FIELDS = %w[
      id gradlePath sourceDir kind group artifact en ko
      sourcePaths testPaths workshops
    ].freeze
    PATH_FIELDS = %w[sourcePaths testPaths workshops].freeze
    LOCALES = {
      "en" => "English",
      "ko" => "Korean",
    }.freeze

    attr_reader :errors

    def initialize(inventory:, manifest_path:, repository_root:)
      @inventory = inventory
      @manifest_path = File.expand_path(manifest_path)
      @repository_root = File.expand_path(repository_root)
      @errors = validate.sort
    end

    private

    def validate
      return ["manual manifest not found: #{display_path(@manifest_path)}"] unless File.file?(@manifest_path)

      manifest = load_manifest
      return @load_errors unless manifest

      errors = []
      unless manifest.is_a?(Hash)
        return ["manual manifest must be a mapping"]
      end

      errors << "manual manifest schemaVersion must be 1" unless manifest["schemaVersion"] == 1
      modules = manifest["modules"]
      unless modules.is_a?(Array)
        errors << "manual manifest modules must be an array"
        return errors
      end

      entries = modules.each_with_index.each_with_object([]) do |(entry, index), result|
        unless entry.is_a?(Hash)
          errors << "module[#{index}]: entry must be a mapping"
          next
        end
        result << entry
      end

      errors.concat(validate_inventory)
      errors.concat(validate_duplicates(entries))
      errors.concat(validate_inventory_alignment(entries))
      entries.each { |entry| errors.concat(validate_entry(entry)) }
      errors
    end

    def load_manifest
      @load_errors = []
      YAML.safe_load(File.read(@manifest_path))
    rescue Psych::SyntaxError => error
      @load_errors << "manual manifest YAML is invalid: #{error.problem}"
      nil
    rescue StandardError => error
      @load_errors << "manual manifest could not be read: #{error.message}"
      nil
    end

    def validate_inventory
      return ["module inventory must be an array"] unless @inventory.is_a?(Array)

      errors = []
      duplicate_values(@inventory, "gradlePath").each do |value|
        errors << "inventory: duplicate gradlePath #{value}"
      end
      duplicate_values(@inventory, "projectName").each do |value|
        errors << "inventory: duplicate projectName #{value}"
      end
      errors
    end

    def validate_duplicates(entries)
      duplicate_values(entries, "id").map { |value| "manifest: duplicate id #{value}" } +
        duplicate_values(entries, "gradlePath").map { |value| "manifest: duplicate gradlePath #{value}" }
    end

    def duplicate_values(entries, key)
      entries
        .select { |entry| entry.is_a?(Hash) && present?(entry[key]) }
        .group_by { |entry| entry[key] }
        .select { |_value, matches| matches.length > 1 }
        .keys
        .sort
    end

    def validate_inventory_alignment(entries)
      return [] unless @inventory.is_a?(Array)

      errors = []
      inventory_by_path = @inventory
        .select { |row| row.is_a?(Hash) && present?(row["gradlePath"]) }
        .each_with_object({}) { |row, result| result[row["gradlePath"]] ||= row }
      manifest_by_path = entries
        .select { |entry| present?(entry["gradlePath"]) }
        .each_with_object({}) { |entry, result| result[entry["gradlePath"]] ||= entry }

      (inventory_by_path.keys - manifest_by_path.keys).sort.each do |path|
        errors << "#{inventory_label(inventory_by_path[path])}: missing from manifest"
      end
      (manifest_by_path.keys - inventory_by_path.keys).sort.each do |path|
        errors << "#{entry_label(manifest_by_path[path])}: gradlePath #{path} is not present in inventory"
      end

      entries.each do |entry|
        path = entry["gradlePath"]
        inventory = inventory_by_path[path]
        next unless inventory

        %w[sourceDir kind].each do |field|
          next if entry[field] == inventory[field]
          errors << "#{entry_label(entry)}: #{field} does not match inventory"
        end
      end
      errors
    end

    def validate_entry(entry)
      errors = []
      label = entry_label(entry)
      REQUIRED_MODULE_FIELDS.each do |field|
        errors << "#{label}: missing manifest field #{field}" unless entry.key?(field)
      end

      unless VALID_KINDS.include?(entry["kind"])
        errors << "#{label}: invalid kind #{entry['kind'].inspect}"
      end
      if %w[example benchmark].include?(entry["kind"]) && !entry["artifact"].nil?
        errors << "#{label}: #{entry['kind']} artifact must be null"
      elsif entry["kind"] == "library" && !present?(entry["artifact"])
        errors << "#{label}: library artifact must be present"
      end

      LOCALES.each do |field, language|
        errors.concat(validate_document(entry, field, language))
      end
      PATH_FIELDS.each do |field|
        errors.concat(validate_repository_paths(entry, field))
      end
      errors
    end

    def validate_document(entry, field, language)
      label = entry_label(entry)
      relative_path = entry[field]
      unless safe_relative_path?(relative_path)
        return ["#{label}: unsafe #{language} document path"]
      end

      document_path = File.expand_path(relative_path, File.dirname(@manifest_path))
      unless within?(document_path, File.dirname(@manifest_path)) && File.file?(document_path)
        return ["#{label}: missing #{language} document"]
      end

      content = File.read(document_path)
      errors = []
      manual_id = frontmatter(content)["manualId"]
      unless manual_id == entry["id"]
        errors << "#{label}: #{language} document manualId must be #{entry['id']}"
      end
      section_ids = content.scan(/^\#{1,6}\s+.*\{#([a-z0-9-]+)\}\s*$/).flatten
      (REQUIRED_SECTIONS - section_ids).each do |section|
        errors << "#{label}: #{language} document missing required section ##{section}"
      end
      errors
    rescue Psych::SyntaxError
      ["#{label}: #{language} document frontmatter is invalid YAML"]
    rescue StandardError => error
      ["#{label}: #{language} document could not be read: #{error.message}"]
    end

    def frontmatter(content)
      match = content.match(/\A---\s*\n(.*?)\n---\s*(?:\n|\z)/m)
      return {} unless match
      value = YAML.safe_load(match[1])
      value.is_a?(Hash) ? value : {}
    end

    def validate_repository_paths(entry, field)
      label = entry_label(entry)
      paths = entry[field]
      return ["#{label}: #{field} must be an array"] unless paths.is_a?(Array)

      paths.each_with_object([]) do |relative_path, errors|
        unless safe_relative_path?(relative_path)
          errors << "#{label}: unsafe #{field} path #{relative_path.inspect}"
          next
        end

        absolute_path = File.expand_path(relative_path, @repository_root)
        if !within?(absolute_path, @repository_root)
          errors << "#{label}: unsafe #{field} path #{relative_path.inspect}"
        elsif !File.exist?(absolute_path)
          errors << "#{label}: missing #{field} path #{relative_path}"
        end
      end
    end

    def safe_relative_path?(value)
      return false unless value.is_a?(String) && !value.empty?
      path = Pathname.new(value)
      !path.absolute? && path.each_filename.none? { |part| part == ".." }
    end

    def within?(path, root)
      expanded_path = File.expand_path(path)
      expanded_root = File.expand_path(root)
      expanded_path == expanded_root || expanded_path.start_with?(expanded_root + File::SEPARATOR)
    end

    def inventory_label(row)
      row["projectName"] || row["gradlePath"] || "inventory module"
    end

    def entry_label(entry)
      entry["id"] || entry["gradlePath"] || "manifest module"
    end

    def present?(value)
      !value.nil? && value != ""
    end

    def display_path(path)
      relative = Pathname.new(path).relative_path_from(Pathname.new(@repository_root)).to_s
      relative.start_with?("..") ? path : relative
    rescue ArgumentError
      path
    end
  end
end
