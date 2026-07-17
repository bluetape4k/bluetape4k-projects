#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative "release_diagram_contract"

root = File.expand_path("../..", __dir__)
inventory = File.join(root, "docs/manual/release-diagrams.yaml")
contract = ReleaseDiagrams::Contract.new(root: root, inventory_path: inventory)

begin
  case ARGV
  when ["--write"]
    contract.sync!
    puts "release-diagrams: linked entries=#{contract.entries.size} release=#{contract.release_ref} commit=#{contract.release_commit}"
  when ["--check"]
    failures = contract.errors
    if failures.empty?
      puts "release-diagrams: failures=0 entries=#{contract.entries.size} release=#{contract.release_ref}"
    else
      warn failures.join("\n")
      exit 1
    end
  else
    warn "usage: sync_release_diagrams.rb --write|--check"
    exit 2
  end
rescue ReleaseDiagrams::ContractError => error
  warn error.message
  exit 1
end
