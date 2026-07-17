require "pathname"
require "yaml"

module ManualDocs
  SUPPORTED_SCHEMA_VERSION = 2
  REQUIRED_SECTIONS = %w[
    problem when-to-use coordinates concepts quick-start api-by-task
    patterns integrations configuration failures operations testing
    workshops limitations sources
  ].freeze

  VALID_KINDS = %w[library example benchmark].freeze
  MANUAL_ASSET_EXTENSIONS = %w[.png .svg].freeze

  class Validator
    REQUIRED_MODULE_FIELDS = %w[
      id gradlePath sourceDir kind group artifact en ko
      sourcePaths testPaths workshops
    ].freeze
    REQUIRED_CHAPTER_FIELDS = %w[id en ko].freeze
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

      unless manifest["schemaVersion"] == SUPPORTED_SCHEMA_VERSION
        errors << "manual manifest schemaVersion must be #{SUPPORTED_SCHEMA_VERSION}"
      end
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
      errors.concat(validate_overview_assets(manifest))
      errors.concat(validate_orphan_assets(entries, manifest))
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
      errors.concat(validate_chapters(entry))
      errors.concat(validate_assets(entry))
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
      errors.concat(validate_markdown_references(document_path, content, label))
      errors
    rescue Psych::SyntaxError
      ["#{label}: #{language} document frontmatter is invalid YAML"]
    rescue StandardError => error
      ["#{label}: #{language} document could not be read: #{error.message}"]
    end

    def validate_chapters(entry)
      label = entry_label(entry)
      chapters = entry.fetch("chapters", [])
      return ["#{label}: chapters must be an array"] unless chapters.is_a?(Array)

      errors = duplicate_values(chapters, "id").map do |id|
        "#{label}: duplicate chapter id #{id}"
      end
      chapters.each_with_index do |chapter, index|
        unless chapter.is_a?(Hash)
          errors << "#{label}: chapter[#{index}] must be a mapping"
          next
        end
        errors.concat(validate_chapter(entry, chapter))
      end
      errors
    end

    def validate_chapter(entry, chapter)
      chapter_id = chapter["id"]
      label = "#{entry_label(entry)}/#{chapter_id || 'chapter'}"
      errors = []
      REQUIRED_CHAPTER_FIELDS.each do |field|
        errors << "#{label}: missing manifest field #{field}" unless chapter.key?(field)
      end
      LOCALES.each do |field, language|
        errors.concat(validate_chapter_document(entry, chapter, field, language))
      end
      errors
    end

    def validate_chapter_document(entry, chapter, field, language)
      chapter_id = chapter["id"]
      label = "#{entry_label(entry)}/#{chapter_id || 'chapter'}"
      relative_path = chapter[field]
      unless safe_relative_path?(relative_path)
        return ["#{label}: unsafe #{language} document path"]
      end

      document_path = File.expand_path(relative_path, File.dirname(@manifest_path))
      unless within?(document_path, File.dirname(@manifest_path)) && File.file?(document_path)
        return ["#{label}: missing #{language} document"]
      end

      content = File.read(document_path)
      metadata = frontmatter(content)
      errors = []
      unless metadata["manualId"] == entry["id"]
        errors << "#{label}: #{language} manualId must be #{entry['id']}"
      end
      unless metadata["chapterId"] == chapter_id
        errors << "#{label}: #{language} chapterId must be #{chapter_id}"
      end
      errors.concat(validate_markdown_references(document_path, content, label))
      errors
    rescue Psych::SyntaxError
      ["#{label}: #{language} document frontmatter is invalid YAML"]
    rescue StandardError => error
      ["#{label}: #{language} document could not be read: #{error.message}"]
    end

    def validate_assets(entry)
      label = entry_label(entry)
      assets = entry.fetch("assets", [])
      return ["#{label}: assets must be an array"] unless assets.is_a?(Array)

      manual_root = File.dirname(@manifest_path)
      errors = duplicate_scalar_values(assets).map { |asset| "#{label}: duplicate asset #{asset}" }
      assets.each do |relative_path|
        unless safe_manual_asset_path?(relative_path)
          errors << "#{label}: unsafe asset path #{relative_path.inspect}"
          next
        end
        absolute_path = File.expand_path(relative_path, manual_root)
        errors << "#{label}: missing asset #{relative_path}" unless File.file?(absolute_path)
      end
      errors.concat(validate_asset_pairs(entry, assets))
      errors
    end

    def validate_overview_assets(manifest)
      overview = manifest["overview"]
      return [] if overview.nil?
      return ["overview: must be a mapping"] unless overview.is_a?(Hash)

      assets = overview.fetch("assets", [])
      return ["overview: assets must be an array"] unless assets.is_a?(Array)

      manual_root = File.dirname(@manifest_path)
      errors = duplicate_scalar_values(assets).map { |asset| "overview: duplicate asset #{asset}" }
      assets.each do |relative_path|
        unless safe_manual_asset_path?(relative_path)
          errors << "overview: unsafe asset path #{relative_path.inspect}"
          next
        end
        errors << "overview: missing asset #{relative_path}" unless File.file?(File.expand_path(relative_path, manual_root))
      end
      errors.concat(validate_asset_pairs({ "id" => "overview" }, assets))
      errors
    end

    def validate_asset_pairs(entry, assets)
      label = entry_label(entry)
      manual_root = File.dirname(@manifest_path)
      registered = assets.select { |asset| asset.is_a?(String) }
      bases = registered.each_with_object([]) do |asset, result|
        extension = File.extname(asset)
        result << asset.delete_suffix(extension) if MANUAL_ASSET_EXTENSIONS.include?(extension)
      end.uniq

      bases.each_with_object([]) do |base, errors|
        MANUAL_ASSET_EXTENSIONS.each do |extension|
          pair = "#{base}#{extension}"
          next if registered.include?(pair) && File.file?(File.expand_path(pair, manual_root))
          errors << "#{label}: missing paired asset #{pair}"
        end
      end
    end

    def validate_orphan_assets(entries, manifest)
      manual_root = File.dirname(@manifest_path)
      assets_root = File.join(manual_root, "assets")
      actual = MANUAL_ASSET_EXTENSIONS.flat_map do |extension|
        Dir.glob(File.join(assets_root, "**", "*#{extension}"))
      end.map { |path| Pathname.new(path).relative_path_from(Pathname.new(manual_root)).to_s }.sort
      registered = entries.flat_map do |entry|
        entry["assets"].is_a?(Array) ? entry["assets"].grep(String) : []
      end.uniq
      overview_assets = manifest.dig("overview", "assets")
      registered.concat(overview_assets.grep(String)) if overview_assets.is_a?(Array)
      registered.uniq!

      (actual - registered).map { |asset| "manual assets: orphan asset #{asset}" }
    end

    def validate_markdown_references(document_path, content, label)
      without_fences = content.gsub(/^```[^\n]*\n.*?^```\s*$/m, "")
      targets = without_fences.scan(/!?\[[^\]]*\]\(([^)]+)\)/).flatten
      targets.each_with_object([]) do |raw_target, errors|
        target = normalize_markdown_target(raw_target)
        next if target.nil?

        absolute_path = File.expand_path(target, File.dirname(document_path))
        unless within?(absolute_path, @repository_root)
          errors << "#{label}: unsafe Markdown reference #{raw_target}"
          next
        end
        unless File.exist?(absolute_path)
          errors << "#{label}: missing Markdown reference #{raw_target}"
          next
        end
        if !within?(File.realpath(absolute_path), File.realpath(@repository_root))
          errors << "#{label}: unsafe Markdown reference #{raw_target}"
        end
      end
    end

    def normalize_markdown_target(raw_target)
      value = raw_target.to_s.strip
      return nil if value.empty? || value.start_with?("#", "/")
      return nil if value.match?(/\A[a-z][a-z0-9+.-]*:/i)

      value = value[1...-1] if value.start_with?("<") && value.end_with?(">")
      value = value.split(/\s+["']/).first
      value = value.split(/[?#]/).first
      value unless value.nil? || value.empty?
    end

    def safe_manual_asset_path?(value)
      safe_relative_path?(value) && value.start_with?("assets/") &&
        MANUAL_ASSET_EXTENSIONS.include?(File.extname(value))
    end

    def duplicate_scalar_values(values)
      values.grep(String).group_by(&:itself).select { |_value, matches| matches.length > 1 }.keys.sort
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
        elsif !within?(File.realpath(absolute_path), File.realpath(@repository_root))
          errors << "#{label}: unsafe #{field} path #{relative_path.inspect}"
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
