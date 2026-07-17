# frozen_string_literal: true

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

    def repository
      value = manifest["repository"]
      return value if value.is_a?(String) && value.match?(%r{\A[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+\z})
      return "bluetape4k/#{value}" if value.is_a?(String) && value.match?(/\A[A-Za-z0-9_.-]+\z/)

      raise ContractError, "manual manifest repository must use owner/name or a repository slug"
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
      failures = inventory_errors + manifest_errors + mirror_errors
      provenance = release_provenance_errors
      failures.concat(provenance)
      failures.concat(release_entry_errors) if provenance.empty?
      failures
    rescue ContractError => error
      [error.message]
    end

    def sync!
      blockers = release_provenance_errors
      blockers.concat(release_entry_errors) if blockers.empty?
      raise ContractError, blockers.join("\n") unless blockers.empty?

      entries.each do |entry|
        entry.manual_pages.each_value { |path| rewrite_page!(path, entry) }
      end
      remove_manifest_mirrors!
      FileUtils.rm_rf(mirror_root)

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
            next
          end
          content = page.read
          failures << "#{entry.id}: #{locale} manual page does not reference release PNG URL" unless content.include?(raw_url(entry, "png"))
          failures << "#{entry.id}: #{locale} manual page does not reference release SVG URL" unless content.include?(blob_url(entry, "svg"))
          failures << "#{entry.id}: #{locale} manual page still references a mirrored asset" if content.include?("assets/readme-diagrams/#{entry.canonical}")
        end
      end
    end

    def manifest_errors
      manifest_path.read.include?("assets/readme-diagrams/") ? ["manual manifest still publishes mirrored release diagrams"] : []
    end

    def mirror_errors
      return [] unless mirror_root.exist?
      ["manual mirror directory still exists: #{MIRROR_ROOT}"]
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
          if !git_object_exists?(path)
            failures << "#{entry.id}: missing release asset #{release_ref}:#{path}"
          else
            failures.concat(release_asset_errors(entry, extension))
          end
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

    def release_asset_errors(entry, extension)
      content = release_asset(entry, extension)
      if extension == "png"
        content.start_with?(PNG_SIGNATURE) ? [] : ["#{entry.id}: release PNG signature is invalid"]
      else
        REXML::Document.new(content)
        []
      end
    rescue REXML::ParseException => error
      ["#{entry.id}: release SVG is invalid XML: #{error.message.lines.first.to_s.strip}"]
    end

    def rewrite_page!(path, entry)
      page = resolved(path)
      raise ContractError, "#{entry.id}: missing manual page #{path}" unless page.file?
      content = page.read
      content = content.gsub(
        /These diagrams are copied byte-for-byte from README assets in the `([^`]+)` release tag\. They describe this manual's released structure and runtime flows, not later Snapshot changes\. Select a preview to open the SVG source\./,
        "These diagrams are loaded directly from README assets published with the `\\1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.",
      )
      content = content.gsub(
        /아래 그림은 현재 개발 브랜치가 아니라 `([^`]+)` 배포 태그의 README 자산을 바이트 단위로 그대로 옮긴 것입니다\. 따라서 이후 SNAPSHOT 변경이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다\. 미리보기를 누르면 SVG 원본이 열립니다\./,
        "아래 그림은 `\\1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.",
      )
      %w[png svg].each do |extension|
        local_reference = %r{(?:\.\./)+assets/readme-diagrams/#{Regexp.escape(entry.canonical)}\.#{extension}}
        replacement = extension == "png" ? raw_url(entry, extension) : blob_url(entry, extension)
        content = content.gsub(local_reference, replacement)
      end
      page.write(content)
    end

    def remove_manifest_mirrors!
      content = manifest_path.read
      content = content.gsub(/^\s*# release-readme-diagrams:start\n.*?^\s*# release-readme-diagrams:end\n?/m, "")
      content = content.lines.reject { |line| line.include?("assets/readme-diagrams/") }.join
      content = content.gsub(/^(\s*)assets:\n(?=\S|\z)/, '\\1assets: []\n')
      manifest_path.write(content)
      @manifest = nil
    end

    def raw_url(entry, extension)
      "https://raw.githubusercontent.com/#{repository}/#{release_commit}/#{release_path(entry, extension)}"
    end

    def blob_url(entry, extension)
      "https://github.com/#{repository}/blob/#{release_commit}/#{release_path(entry, extension)}"
    end

    def release_asset(entry, extension)
      git_capture("show", "#{release_ref}:#{release_path(entry, extension)}")
    end

    def release_path(entry, extension)
      "#{RELEASE_ROOT}/#{entry.canonical}.#{extension}"
    end

    def mirror_root
      @mirror_root ||= resolved(MIRROR_ROOT)
    end

    def manifest_path
      @manifest_path ||= resolved(MANIFEST)
    end

    def manifest
      @manifest ||= load_yaml(manifest_path, "manual manifest")
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
