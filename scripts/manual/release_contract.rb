require "open3"
require "pathname"
require "set"
require "yaml"

module ManualDocs
  class ReleaseContract
    ValidationResult = Struct.new(:errors, :checked_count, :skipped_manual_count, keyword_init: true)
    TAG_PATTERN = /\Av?\d+\.\d+\.\d+\z/
    SHA_PATTERN = /\A[0-9a-f]{40}\z/i
    REPOSITORY_LINK_PATTERN = /!?\[[^\]]*\]\(\s*<?((?:\.\.\/){4,}[^)\s>]+)>?(?:\s+["'][^)]*["'])?\s*\)/
    REFERENCE_DEFINITION_PATTERN = /^[ \t]{0,3}\[([^\]]+)\]:[ \t]*(?:\n[ \t]+)?(?:<((?:\.\.\/){4,}[^>\r\n]+)>|((?:\.\.\/){4,}[^ \t\r\n]+))/
    REFERENCE_USAGE_PATTERN = /!?\[([^\]]+)\](?:\[([^\]]*)\])?/

    def initialize(repository_root:, tag:, expected_sha:, git_runner: nil)
      @repository_root = File.expand_path(repository_root)
      @tag = tag
      @expected_sha = expected_sha
      @git_runner = git_runner || method(:run_git)
    end

    def errors
      validate.errors
    end

    def validate
      input_errors = validate_inputs
      return result(input_errors) unless input_errors.empty?

      resolved_sha = resolve_tag
      return result(["release tag not found: refs/tags/#{@tag}"]) unless resolved_sha

      unless resolved_sha.casecmp?(@expected_sha)
        return result(["release tag #{@tag} resolves to #{resolved_sha}, expected #{@expected_sha}"])
      end

      inventory = release_inventory(resolved_sha)
      return result(["release inventory could not be read: #{resolved_sha}"]) unless inventory

      links, skipped_manual_count = repository_links(inventory)
      errors = if links.empty?
                 ["no repository-relative manual links found"]
               else
                 missing_path_errors(inventory, links)
               end
      result(errors, links.length, skipped_manual_count)
    end

    private

    def validate_inputs
      errors = []
      errors << "release tag must match v?MAJOR.MINOR.PATCH: #{@tag}" unless TAG_PATTERN.match?(@tag)
      errors << "expected SHA must be a 40-character hexadecimal commit id: #{@expected_sha}" unless SHA_PATTERN.match?(@expected_sha)
      errors
    end

    def resolve_tag
      output, success = @git_runner.call([
        "rev-parse",
        "--verify",
        "refs/tags/#{@tag}^{commit}",
      ])
      return unless success

      output.strip
    end

    def release_inventory(sha)
      output, success = @git_runner.call(["ls-tree", "-r", "--name-only", sha])
      return unless success

      paths = output.lines(chomp: true).to_set
      paths.each_with_object(paths.dup) do |path, inventory|
        directory = File.dirname(path)
        until directory == "."
          inventory << directory
          directory = File.dirname(directory)
        end
      end
    end

    def repository_links(inventory)
      files, skipped_manual_count = release_manual_files(inventory)
      links = files.flat_map do |absolute_path|
        relative_file = Pathname.new(absolute_path).relative_path_from(Pathname.new(@repository_root)).to_s
        content = File.read(absolute_path)
        link_targets(content).map do |offset, target|
          line = content[0...offset].count("\n") + 1
          [relative_file, line, target]
        end
      end
      [links, skipped_manual_count]
    end

    def link_targets(content)
      inline_targets = content.to_enum(:scan, REPOSITORY_LINK_PATTERN).map do
        match = Regexp.last_match
        [match.begin(0), match[1]]
      end
      (inline_targets + reference_targets(content)).sort_by(&:first)
    end

    def reference_targets(content)
      definitions = reference_definitions(content)
      content.to_enum(:scan, REFERENCE_USAGE_PATTERN).map do
        match = Regexp.last_match
        next if ["(", ":"].include?(content[match.end(0)])

        label = match[2].nil? || match[2].empty? ? match[1] : match[2]
        target = definitions[normalize_reference_label(label)]
        [match.begin(0), target] if target
      end.compact
    end

    def reference_definitions(content)
      content.to_enum(:scan, REFERENCE_DEFINITION_PATTERN).each_with_object({}) do |_captures, definitions|
        match = Regexp.last_match
        label = normalize_reference_label(match[1])
        definitions[label] ||= match[2] || match[3]
      end
    end

    def normalize_reference_label(label)
      label.gsub(/\s+/, " ").strip.downcase
    end

    def missing_path_errors(inventory, links)
      links.map do |relative_file, line, target|
        repository_path = repository_path_for(relative_file, target)
        if repository_path.nil?
          "#{relative_file}:#{line}: unsafe release path: #{target}"
        elsif !inventory.include?(repository_path)
          "#{relative_file}:#{line}: release path not found: #{repository_path}"
        end
      end.compact
    end

    def result(errors, checked_count = 0, skipped_manual_count = 0)
      ValidationResult.new(
        errors: errors,
        checked_count: checked_count,
        skipped_manual_count: skipped_manual_count,
      )
    end

    def repository_path_for(relative_file, target)
      path_without_suffix = target.split(/[?#]/, 2).first
      candidate = Pathname.new(File.dirname(relative_file)).join(path_without_suffix).cleanpath
      normalized = candidate.to_s
      return if candidate.absolute? || normalized == ".." || normalized.start_with?("../")

      normalized
    end

    def manual_files
      Dir.glob(File.join(@repository_root, "docs/manual/**/*.md")).sort
    end

    def release_manual_files(inventory)
      files = manual_files
      skipped = snapshot_only_manual_files(inventory) & files
      [files - skipped, skipped.length]
    end

    def snapshot_only_manual_files(inventory)
      manifest_path = File.join(@repository_root, "docs/manual/manifest.yaml")
      return [] unless File.file?(manifest_path)

      manifest = YAML.safe_load(File.read(manifest_path))
      modules = manifest.is_a?(Hash) ? manifest["modules"] : nil
      return [] unless modules.is_a?(Array)

      manual_root = File.dirname(manifest_path)
      modules.each_with_object([]) do |entry, paths|
        next unless entry.is_a?(Hash)
        source_dir = entry["sourceDir"]
        next unless source_dir.is_a?(String) && !inventory.include?(source_dir)

        document_paths(entry).each do |relative_path|
          next unless relative_path.is_a?(String)
          absolute_path = File.expand_path(relative_path, manual_root)
          paths << absolute_path if File.file?(absolute_path)
        end
      end.uniq.sort
    end

    def document_paths(entry)
      paths = [entry["en"], entry["ko"]]
      chapters = entry["chapters"]
      if chapters.is_a?(Array)
        paths.concat(chapters.filter_map { |chapter| chapter["en"] if chapter.is_a?(Hash) })
        paths.concat(chapters.filter_map { |chapter| chapter["ko"] if chapter.is_a?(Hash) })
      end
      paths
    end

    def run_git(arguments)
      stdout, _stderr, status = Open3.capture3("git", "-C", @repository_root, *arguments)
      [stdout, status.success?]
    end
  end
end
