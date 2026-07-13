require "open3"
require "pathname"
require "set"

module ManualDocs
  class ReleaseContract
    TAG_PATTERN = /\Av?\d+\.\d+\.\d+\z/
    SHA_PATTERN = /\A[0-9a-f]{40}\z/i
    REPOSITORY_LINK_PATTERN = /!?\[[^\]]*\]\(\s*<?((?:\.\.\/){4,}[^)\s>]+)>?(?:\s+["'][^)]*["'])?\s*\)/

    def initialize(repository_root:, tag:, expected_sha:, git_runner: nil)
      @repository_root = File.expand_path(repository_root)
      @tag = tag
      @expected_sha = expected_sha
      @git_runner = git_runner || method(:run_git)
    end

    def errors
      input_errors = validate_inputs
      return input_errors unless input_errors.empty?

      resolved_sha = resolve_tag
      return ["release tag not found: refs/tags/#{@tag}"] unless resolved_sha

      unless resolved_sha.casecmp?(@expected_sha)
        return ["release tag #{@tag} resolves to #{resolved_sha}, expected #{@expected_sha}"]
      end

      inventory = release_inventory(resolved_sha)
      return ["release inventory could not be read: #{resolved_sha}"] unless inventory

      missing_path_errors(inventory)
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

    def missing_path_errors(inventory)
      manual_files.flat_map do |absolute_path|
        relative_file = Pathname.new(absolute_path).relative_path_from(Pathname.new(@repository_root)).to_s
        File.readlines(absolute_path, chomp: true).each_with_index.flat_map do |line, index|
          line.scan(REPOSITORY_LINK_PATTERN).map do |match|
            target = match.first
            repository_path = repository_path_for(relative_file, target)
            if repository_path.nil?
              "#{relative_file}:#{index + 1}: unsafe release path: #{target}"
            elsif !inventory.include?(repository_path)
              "#{relative_file}:#{index + 1}: release path not found: #{repository_path}"
            end
          end.compact
        end
      end
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

    def run_git(arguments)
      stdout, _stderr, status = Open3.capture3("git", "-C", @repository_root, *arguments)
      [stdout, status.success?]
    end
  end
end
