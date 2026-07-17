# frozen_string_literal: true

require "digest"
require "fileutils"
require "open3"
require "pathname"
require "rexml/document"
require "yaml"

module ReleaseDiagrams
  class ContractError < StandardError; end

  Entry = Struct.new(:id, :canonical, :manual_pages, :release_readmes, keyword_init: true)

  class Contract
    RELEASE_ROOT = "docs/images/readme-diagrams"
    MIRROR_ROOT = "docs/manual/assets/readme-diagrams"
    MANIFEST = "docs/manual/manifest.yaml"
    PNG_SIGNATURE = "\x89PNG\r\n\x1A\n".b

    attr_reader :root, :inventory_path

    def initialize(root:, inventory_path:)
      @root = Pathname.new(root).expand_path
      @inventory_path = Pathname.new(inventory_path).expand_path
    end

    def entries
      @entries ||= load_entries
    end

    def release_ref
      value = manifest["releaseRef"]
      unless value.is_a?(String) && value.match?(/\A[0-9A-Za-z][0-9A-Za-z._\/-]*\z/) && !value.include?("..")
        raise ContractError, "manual manifest releaseRef is invalid"
      end
      value
    end

    def release_commit
      value = manifest["releaseCommit"]
      raise ContractError, "manual manifest releaseCommit must be a full SHA" unless value.is_a?(String) && value.match?(/\A[0-9a-f]{40}\z/)
      value
    end

    def errors
      failures = inventory_errors
      provenance = release_provenance_errors
      failures.concat(provenance)
      failures.concat(release_entry_errors) if provenance.empty?
      failures.concat(mirror_errors) if provenance.empty?
      failures
    rescue ContractError => error
      [error.message]
    end

    def sync!
      blockers = inventory_errors + release_provenance_errors
      blockers.concat(release_entry_errors) if blockers.empty?
      raise ContractError, blockers.join("\n") unless blockers.empty?

      expected = []
      entries.each do |entry|
        %w[svg png].each do |extension|
          target = mirror_path(entry, extension)
          target.dirname.mkpath
          File.binwrite(target, release_asset(entry, extension))
          expected << target.relative_path_from(mirror_root).to_s
        end
      end
      if mirror_root.directory?
        mirror_root.glob("**/*").select(&:file?).each do |path|
          relative = path.relative_path_from(mirror_root).to_s
          path.delete unless expected.include?(relative)
        end
      end

      failures = errors
      raise ContractError, failures.join("\n") unless failures.empty?
      true
    end

    private

    def load_entries
      data = load_yaml(inventory_path, "release diagram inventory")
      raise ContractError, "release diagram schemaVersion must be 1" unless data["schemaVersion"] == 1
      raise ContractError, "release diagram sourcePolicy must be release-readme" unless data["sourcePolicy"] == "release-readme"
      rows = data["diagrams"]
      raise ContractError, "release diagram inventory diagrams must be an array" unless rows.is_a?(Array)

      parsed = rows.map { |row| parse_entry(row) }
      duplicate_ids = parsed.group_by(&:id).select { |_id, values| values.size > 1 }.keys
      duplicate_canonical = parsed.group_by(&:canonical).select { |_id, values| values.size > 1 }.keys
      raise ContractError, "duplicate diagram ids: #{duplicate_ids.sort.join(', ')}" unless duplicate_ids.empty?
      raise ContractError, "duplicate canonical diagrams: #{duplicate_canonical.sort.join(', ')}" unless duplicate_canonical.empty?
      parsed.freeze
    end

    def parse_entry(row)
      raise ContractError, "diagram entry must be a mapping" unless row.is_a?(Hash)
      id = row["id"]
      canonical = row["canonical"]
      pages = row["manualPages"]
      readmes = row["releaseReadmes"]
      raise ContractError, "diagram id must be a non-empty string" unless id.is_a?(String) && !id.empty?
      raise ContractError, "#{id}: unsafe canonical path #{canonical.inspect}" unless safe_canonical?(canonical)
      unless pages.is_a?(Hash) && pages.keys.sort == %w[en ko] && pages.values.all? { |path| safe_relative?(path) }
        raise ContractError, "#{id}: manualPages must define safe en/ko paths"
      end
      unless readmes.is_a?(Array) && !readmes.empty? && readmes.all? { |path| safe_relative?(path) }
        raise ContractError, "#{id}: releaseReadmes must contain safe paths"
      end
      Entry.new(id: id, canonical: canonical, manual_pages: pages, release_readmes: readmes)
    end

    def inventory_errors
      entries.flat_map do |entry|
        entry.manual_pages.each_with_object([]) do |(locale, path), failures|
          page = resolved(path)
          if !page.file?
            failures << "#{entry.id}: missing #{locale} manual page #{path}"
          elsif !page.read.include?("assets/readme-diagrams/#{entry.canonical}.png")
            failures << "#{entry.id}: #{locale} manual page does not reference release PNG"
          end
        end
      end
    end

    def release_provenance_errors
      actual = git_capture("rev-parse", "#{release_ref}^{commit}").strip
      actual == release_commit ? [] : ["manual releaseRef #{release_ref} resolves to #{actual}, expected #{release_commit}"]
    rescue ContractError => error
      [error.message]
    end

    def release_entry_errors
      entries.flat_map do |entry|
        failures = []
        %w[svg png].each do |extension|
          path = release_path(entry, extension)
          failures << "#{entry.id}: missing release asset #{release_ref}:#{path}" unless git_object_exists?(path)
        end
        entry.release_readmes.each do |path|
          if !git_object_exists?(path)
            failures << "#{entry.id}: missing release README #{release_ref}:#{path}"
          elsif !git_capture("show", "#{release_ref}:#{path}").include?(entry.canonical)
            failures << "#{entry.id}: release README #{path} does not reference #{entry.canonical}"
          end
        end
        failures
      end
    end

    def mirror_errors
      expected = []
      failures = entries.flat_map do |entry|
        %w[svg png].flat_map do |extension|
          mirror = mirror_path(entry, extension)
          expected << mirror.relative_path_from(mirror_root).to_s
          if !mirror.file?
            ["#{entry.id}: missing mirror #{extension.upcase}"]
          elsif Digest::SHA256.file(mirror).hexdigest != Digest::SHA256.hexdigest(release_asset(entry, extension))
            ["#{entry.id}: release and mirror #{extension.upcase} digests differ"]
          else
            visual_asset_errors(entry, mirror, extension)
          end
        end
      end
      if mirror_root.directory?
        mirror_root.glob("**/*").select(&:file?).each do |path|
          relative = path.relative_path_from(mirror_root).to_s
          failures << "orphan mirror asset: #{relative}" unless expected.include?(relative)
        end
      end
      failures
    end

    def visual_asset_errors(entry, path, extension)
      if extension == "png"
        File.binread(path, 8) == PNG_SIGNATURE ? [] : ["#{entry.id}: mirror PNG signature is invalid"]
      else
        REXML::Document.new(path.read)
        []
      end
    rescue REXML::ParseException => error
      ["#{entry.id}: mirror SVG is invalid XML: #{error.message.lines.first.to_s.strip}"]
    end

    def release_asset(entry, extension)
      git_capture("show", "#{release_ref}:#{release_path(entry, extension)}")
    end

    def release_path(entry, extension)
      "#{RELEASE_ROOT}/#{entry.canonical}.#{extension}"
    end

    def mirror_path(entry, extension)
      mirror_root.join("#{entry.canonical}.#{extension}")
    end

    def mirror_root
      @mirror_root ||= resolved(MIRROR_ROOT)
    end

    def manifest
      @manifest ||= load_yaml(resolved(MANIFEST), "manual manifest")
    end

    def load_yaml(path, label)
      raise ContractError, "#{label} not found" unless path.file?
      data = YAML.safe_load(path.read)
      raise ContractError, "#{label} must be a mapping" unless data.is_a?(Hash)
      data
    rescue Psych::SyntaxError => error
      raise ContractError, "#{label} YAML is invalid: #{error.problem}"
    end

    def safe_canonical?(value)
      return false unless value.is_a?(String) && value.match?(/\A[A-Za-z0-9][A-Za-z0-9._-]*(?:\/[A-Za-z0-9][A-Za-z0-9._-]*)*\z/)
      safe_relative?(value)
    end

    def safe_relative?(value)
      return false unless value.is_a?(String) && !value.empty?
      path = Pathname.new(value)
      !path.absolute? && path.cleanpath.to_s == value && !path.each_filename.to_a.include?("..")
    end

    def resolved(path)
      root.join(path).cleanpath
    end

    def git_object_exists?(path)
      _stdout, _stderr, status = Open3.capture3("git", "-C", root.to_s, "cat-file", "-e", "#{release_ref}:#{path}")
      status.success?
    end

    def git_capture(*arguments)
      stdout, stderr, status = Open3.capture3("git", "-C", root.to_s, *arguments, binmode: true)
      raise ContractError, "git #{arguments.first} failed: #{stderr.strip}" unless status.success?
      stdout
    rescue Errno::ENOENT => error
      raise ContractError, "git executable not found: #{error.message}"
    end
  end
end
