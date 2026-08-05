require "rexml/document"
require "set"

module Publication
  class InventoryAudit
    GROUP_ID = "io.github.bluetape4k"
    BOM_ARTIFACT_ID = "bluetape4k-bom"

    def initialize(paths)
      @paths = Array(paths).map { |path| File.expand_path(path) }.sort
    end

    def errors
      documents = @paths.map { |path| [path, REXML::Document.new(File.read(path))] }
      bom_entry = documents.find { |_, document| artifact_id(document) == BOM_ARTIFACT_ID }
      return ["publication inventory has no #{BOM_ARTIFACT_ID} POM"] unless bom_entry

      published = documents.each_with_object(Set.new) do |(_, document), result|
        next unless group_id(document) == GROUP_ID

        artifact = artifact_id(document)
        result << artifact unless artifact.empty? || artifact == BOM_ARTIFACT_ID
      end
      constrained = REXML::XPath.match(
        bom_entry.last,
        "/project/dependencyManagement/dependencies/dependency",
      ).each_with_object(Set.new) do |dependency, result|
        next unless dependency.elements["groupId"]&.text.to_s.strip == GROUP_ID

        artifact = dependency.elements["artifactId"]&.text.to_s.strip
        result << artifact unless artifact.empty?
      end

      [].tap do |result|
        unpublished = (constrained - published).sort
        missing = (published - constrained).sort
        result << "BOM constrains unpublished artifacts: #{unpublished.join(', ')}" unless unpublished.empty?
        result << "published artifacts missing from BOM: #{missing.join(', ')}" unless missing.empty?
      end
    rescue REXML::ParseException => error
      ["publication inventory contains invalid XML: #{error.message.lines.first.to_s.strip}"]
    end

    private

    def group_id(document)
      REXML::XPath.first(document, "/project/groupId")&.text.to_s.strip
    end

    def artifact_id(document)
      REXML::XPath.first(document, "/project/artifactId")&.text.to_s.strip
    end
  end
end
