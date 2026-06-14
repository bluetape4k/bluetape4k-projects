#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import {
  existsSync,
  mkdirSync,
  readdirSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const DOT = findExecutable("dot", ["/opt/homebrew/bin/dot", "/usr/local/bin/dot", "/usr/bin/dot"]);
const RSVG = findExecutable("rsvg-convert", ["/opt/homebrew/bin/rsvg-convert", "/usr/local/bin/rsvg-convert", "/usr/bin/rsvg-convert"]);
const ARCH_FONT = "Architects Daughter";
const DETAIL_FONT = "Comic Mono";
const MIN_CLEARANCE = 12;

const fonts = {
  arch: findFontFile(ARCH_FONT),
  detail: findFontFile(DETAIL_FONT),
};

if (!DOT) throw new Error("Graphviz dot is required");
if (!RSVG) throw new Error("rsvg-convert is required");
if (!fonts.arch || !fonts.detail) throw new Error(`required fonts missing: ${JSON.stringify(fonts)}`);

mkdirSync(OUT, { recursive: true });

const palette = {
  blue: { fill: "#E8F3FF", stroke: "#75A9E8", line: "#4F83BF" },
  green: { fill: "#EAF7EF", stroke: "#69B888", line: "#58A978" },
  teal: { fill: "#E9F7F6", stroke: "#45A7A1", line: "#45A7A1" },
  amber: { fill: "#FFF3D9", stroke: "#D9AA4D", line: "#D9AA4D" },
  pink: { fill: "#FCE7F3", stroke: "#DB7890", line: "#DB7890" },
  purple: { fill: "#F1ECFF", stroke: "#8A72D6", line: "#8A72D6" },
  gray: { fill: "#F6F8FA", stroke: "#AAB7C4", line: "#6B7D90" },
  red: { fill: "#FDECEC", stroke: "#DB7F75", line: "#C85D55" },
};

const sourceEvidence = [
  "cache/cache-core/README.md",
  "cache/cache-core/README.ko.md",
  "NearCacheOperations.kt",
  "SuspendNearCacheOperations.kt",
  "NearCacheStatistics.kt",
  "NearCacheResilienceConfig.kt",
  "ResilientNearCacheDecorator.kt",
  "ResilientSuspendNearCacheDecorator.kt",
  "SuspendJCache.kt",
  "CaffeineSuspendJCache.kt",
  "NearJCache.kt",
  "SuspendNearJCache.kt",
  "NearJCacheConfig.kt",
  "JCacheEntryEventListener.kt",
  "SuspendJCacheEntryEventListener.kt",
];

const diagrams = [
  {
    file: "cache-cache-core-diagram-01",
    kind: "class",
    title: "NearCache Interface Hierarchy",
    subtitle: "Provider-neutral contracts stay above resilience wrappers and backend implementations.",
    intent: "Explain the public NearCache contracts and where decorators, statistics, and backend modules attach.",
    width: 1780,
    height: 1160,
    titleBottom: 138,
    footerTop: 1068,
    groups: [
      group("contracts", "Provider-neutral contracts", 96, 168, 1060, 255),
      group("resilience", "Resilience wrappers", 96, 455, 1060, 245),
      group("backends", "Backend modules implement the contracts", 96, 735, 1060, 195),
      group("support", "Shared support types", 1230, 230, 430, 630),
    ],
    nodes: [
      uml("ops", "interface", "NearCacheOperations<V>", [
        "get / getAll / containsKey",
        "put / replace / remove",
        "clearLocal / clearAll / close",
        "stats(): NearCacheStatistics",
      ], "blue", 155, 250, 425, 144),
      uml("suspendOps", "interface", "SuspendNearCacheOperations<V>", [
        "suspend read/write/delete",
        "suspend clearAll / close",
        "localCacheSize()",
        "stats(): NearCacheStatistics",
      ], "teal", 675, 250, 425, 144),
      uml("decorator", "decorator", "ResilientNearCacheDecorator<V>", [
        "wraps blocking contract",
        "Retry + GetFailureStrategy",
        "write-through preserved",
      ], "green", 155, 535, 425, 126),
      uml("suspendDecorator", "decorator", "ResilientSuspendNearCacheDecorator<V>", [
        "wraps coroutine contract",
        "Retry.executeSuspendFunction",
        "CancellationException propagates",
      ], "green", 675, 535, 425, 126),
      card("nativeBackends", "Native NearCache modules", ["Lettuce CLIENT TRACKING", "Hazelcast IMap listener", "Redisson local cached map"], "amber", 185, 815, 375, 88),
      card("jcacheBackends", "JCache-backed modules", ["NearJCache / SuspendNearJCache", "Lettuce and Redisson JCache", "Hazelcast listener-free factory"], "purple", 690, 815, 375, 88),
      uml("stats", "interface", "NearCacheStatistics", [
        "localHits / localMisses",
        "backHits / backMisses",
        "localSize / localEvictions",
        "hitRate",
      ], "pink", 1265, 310, 355, 144),
      uml("resilienceConfig", "data class", "NearCacheResilienceConfig", [
        "retryMaxAttempts > 0",
        "retryWaitDuration > 0",
        "retryExponentialBackoff",
        "getFailureStrategy",
      ], "amber", 1265, 530, 355, 144),
      uml("failure", "enum", "GetFailureStrategy", [
        "RETURN_FRONT_OR_NULL",
        "PROPAGATE_EXCEPTION",
      ], "red", 1265, 730, 355, 112),
    ],
    routes: [
      route("decorator", "ops", "wraps", "green", [{ x: 368, y: 535 }, { x: 368, y: 394 }], { dashed: true, showLabel: false }),
      route("suspendDecorator", "suspendOps", "wraps", "green", [{ x: 888, y: 535 }, { x: 888, y: 394 }], { dashed: true }),
      route("nativeBackends", "ops", "implements", "amber", [{ x: 185, y: 859 }, { x: 86, y: 859 }, { x: 86, y: 322 }, { x: 155, y: 322 }], { allowDetour: true, showLabel: false }),
      route("jcacheBackends", "suspendOps", "implements", "purple", [{ x: 1065, y: 859 }, { x: 1140, y: 859 }, { x: 1140, y: 322 }, { x: 1100, y: 322 }], { allowDetour: true, showLabel: false }),
      route("resilienceConfig", "failure", "selects", "red", [{ x: 1443, y: 674 }, { x: 1443, y: 730 }]),
    ],
  },
  {
    file: "cache-cache-core-diagram-02",
    kind: "class",
    title: "NearCacheOperations Blocking Contract",
    subtitle: "Blocking callers use one contract for front-first reads, write-through updates, lifecycle, and statistics.",
    intent: "Explain the method families exposed by NearCacheOperations without implying a specific provider.",
    width: 1680,
    height: 1080,
    titleBottom: 138,
    footerTop: 988,
    groups: [
      group("contract", "Central blocking contract", 610, 190, 460, 220),
      group("readwrite", "Data operations", 92, 475, 1495, 205),
      group("lifecycle", "Lifecycle and observability", 250, 720, 1300, 170),
    ],
    nodes: [
      uml("ops", "interface", "NearCacheOperations<V>", [
        "AutoCloseable",
        "cacheName / isClosed",
        "front cache first, back cache fallback",
      ], "blue", 650, 250, 380, 128),
      card("read", "Read path", ["get(key)", "getAll(keys)", "containsKey(key)"], "teal", 145, 540, 300, 88),
      card("write", "Write path", ["put / putAll", "putIfAbsent", "replace"], "green", 520, 540, 300, 88),
      card("delete", "Delete path", ["remove / removeAll", "getAndRemove", "getAndReplace"], "pink", 895, 540, 300, 88),
      card("manage", "Cache management", ["clearLocal()", "clearAll()", "local/back size"], "amber", 1270, 540, 300, 88),
      card("stats", "NearCacheStatistics", ["local/back hit-miss", "evictions and hitRate"], "purple", 355, 785, 315, 82),
      card("close", "Resource lifecycle", ["close()", "isClosed guard"], "gray", 815, 785, 260, 82),
      card("resilience", "Optional resilience", ["withResilience { ... }", "Retry + get failure strategy"], "green", 1215, 785, 300, 82),
    ],
    routes: [
      route("ops", "read", "exposes", "teal", [{ x: 650, y: 310 }, { x: 390, y: 310 }, { x: 390, y: 540 }], { allowDetour: true }),
      route("ops", "write", "exposes", "green", [{ x: 740, y: 378 }, { x: 740, y: 540 }]),
      route("ops", "delete", "exposes", "pink", [{ x: 940, y: 378 }, { x: 940, y: 540 }]),
      route("ops", "manage", "exposes", "amber", [{ x: 1030, y: 310 }, { x: 1420, y: 310 }, { x: 1420, y: 540 }], { allowDetour: true }),
    ],
  },
  {
    file: "cache-cache-core-diagram-03",
    kind: "class",
    title: "SuspendNearCacheOperations Coroutine Contract",
    subtitle: "Suspend APIs keep remote work cancellable while local statistics and front-cache checks stay synchronous.",
    intent: "Explain the coroutine contract and its cancellation-safe resilience wrapper.",
    width: 1780,
    height: 1100,
    titleBottom: 138,
    footerTop: 1008,
    groups: [
      group("contract", "Coroutine NearCache contract", 585, 188, 550, 235),
      group("families", "Operation families", 92, 500, 1570, 205),
      group("support", "Coroutine-specific guarantees", 260, 750, 1325, 170),
    ],
    nodes: [
      uml("ops", "interface", "SuspendNearCacheOperations<V>", [
        "suspend get / put / remove",
        "suspend clearAll / close",
        "clearLocal and stats stay local",
      ], "teal", 640, 250, 440, 126),
      card("read", "Suspend read path", ["suspend get(key)", "suspend getAll(keys)", "suspend containsKey(key)"], "teal", 145, 565, 330, 88),
      card("write", "Suspend write path", ["suspend put / putAll", "suspend putIfAbsent", "suspend replace"], "green", 535, 565, 330, 88),
      card("delete", "Suspend delete path", ["suspend remove / removeAll", "suspend getAndRemove"], "pink", 925, 565, 330, 88),
      card("local", "Local-only controls", ["clearLocal()", "localCacheSize()", "stats() snapshot"], "amber", 1315, 565, 330, 88),
      card("cancel", "Cancellation semantics", ["CancellationException is rethrown", "no retry for coroutine cancel"], "red", 330, 815, 350, 82),
      card("decorator", "ResilientSuspendNearCacheDecorator", ["executeSuspendFunction", "Retry only for normal failures"], "green", 790, 815, 380, 82),
      card("stats", "NearCacheStatistics", ["front/back counters", "immutable snapshot"], "purple", 1265, 815, 300, 82),
    ],
    routes: [
      route("ops", "read", "exposes", "teal", [{ x: 640, y: 310 }, { x: 390, y: 310 }, { x: 390, y: 565 }], { allowDetour: true }),
      route("ops", "write", "exposes", "green", [{ x: 720, y: 376 }, { x: 720, y: 565 }]),
      route("ops", "delete", "exposes", "pink", [{ x: 1080, y: 336 }, { x: 1090, y: 336 }, { x: 1090, y: 565 }]),
      route("ops", "local", "exposes", "amber", [{ x: 1080, y: 310 }, { x: 1480, y: 310 }, { x: 1480, y: 565 }], { allowDetour: true }),
      route("decorator", "ops", "decorates", "green", [{ x: 895, y: 815 }, { x: 895, y: 445 }, { x: 980, y: 445 }, { x: 980, y: 376 }], { dashed: true, allowDetour: true, showLabel: false }),
      route("decorator", "cancel", "preserves", "red", [{ x: 790, y: 856 }, { x: 680, y: 856 }], { dashed: true }),
    ],
  },
  {
    file: "cache-cache-core-diagram-04",
    kind: "class",
    title: "SuspendJCache Interface Contract",
    subtitle: "The coroutine JCache contract adds Flow entries and suspend mutations over listener-compatible cache operations.",
    intent: "Explain SuspendJCache as the back/front cache contract used by SuspendNearJCache and CaffeineSuspendJCache.",
    width: 1760,
    height: 1080,
    titleBottom: 138,
    footerTop: 988,
    groups: [
      group("api", "SuspendJCache core API", 545, 182, 610, 250),
      group("implementations", "Implementations", 170, 500, 1385, 225),
      group("listeners", "Event listener boundary", 270, 760, 1160, 165),
    ],
    nodes: [
      uml("suspendJCache", "interface", "SuspendJCache<K,V>", [
        "entries(): Flow<SuspendJCacheEntry<K,V>>",
        "suspend get / put / remove / replace",
        "registerCacheEntryListener(...)",
        "unwrap(clazz)",
      ], "teal", 620, 245, 460, 148),
      uml("entry", "data class", "SuspendJCacheEntry<K,V>", ["key", "value"], "purple", 210, 575, 330, 112),
      uml("caffeine", "class", "CaffeineSuspendJCache<K,V>", ["AsyncCache<K,V>", "CompletableFuture.await()", "local listener no-op"], "green", 635, 570, 430, 126),
      uml("near", "class", "SuspendNearJCache<K,V>", ["frontCache + backCache", "front miss -> back fill", "listener-backed invalidation"], "amber", 1170, 570, 350, 126),
      card("listener", "SuspendJCacheEntryEventListener", ["created/updated -> putAll", "removed/expired -> removeAll", "SupervisorJob + Dispatchers.IO"], "pink", 395, 825, 420, 82),
      card("jcacheEvents", "JCache event source", ["CacheEntryCreated/Updated", "CacheEntryRemoved/Expired"], "blue", 940, 825, 360, 82),
    ],
    routes: [
      route("caffeine", "suspendJCache", "implements", "green", [{ x: 850, y: 570 }, { x: 850, y: 393 }]),
      route("near", "suspendJCache", "implements", "amber", [{ x: 1170, y: 628 }, { x: 1140, y: 628 }, { x: 1140, y: 342 }, { x: 1080, y: 342 }], { allowDetour: true }),
      route("suspendJCache", "entry", "emits Flow entries", "purple", [{ x: 620, y: 318 }, { x: 150, y: 318 }, { x: 150, y: 623 }, { x: 210, y: 623 }], { allowDetour: true }),
      route("jcacheEvents", "listener", "dispatches", "pink", [{ x: 940, y: 866 }, { x: 815, y: 866 }]),
      route("listener", "suspendJCache", "updates target", "pink", [{ x: 610, y: 825 }, { x: 610, y: 470 }, { x: 700, y: 470 }, { x: 700, y: 393 }], { dashed: true, allowDetour: true, showLabel: false }),
    ],
  },
  {
    file: "cache-cache-core-diagram-05",
    kind: "architecture",
    title: "NearJCache Two-Tier Write-Through",
    subtitle: "Blocking JCache callers hit the local front cache first while writes update front and synchronize the back cache.",
    intent: "Explain the blocking NearJCache runtime flow, listener invalidation, and configuration boundary.",
    width: 1780,
    height: 1160,
    titleBottom: 138,
    footerTop: 1056,
    groups: [
      group("caller", "Blocking caller", 100, 185, 360, 175),
      group("facade", "NearJCache facade", 555, 165, 620, 300),
      group("tiers", "Two cache tiers", 165, 555, 1450, 210),
      group("events", "Back-cache event synchronization", 245, 830, 1285, 170),
    ],
    nodes: [
      card("caller", "Application / JCache client", ["javax.cache.Cache<K,V>", "blocking API"], "blue", 140, 250, 285, 82),
      card("near", "NearJCache<K,V>", ["JCache<K,V> by backCache", "front-first getDeeply()", "write-through operations"], "green", 670, 235, 390, 104),
      card("config", "NearJCacheConfig DSL", ["Caffeine front factory", "isSynchronous", "syncRemoteTimeout"], "purple", 690, 365, 350, 82),
      card("front", "Front JCache", ["local provider", "contains/get are front-only", "clear() clears front"], "teal", 255, 615, 360, 100),
      card("back", "Back JCache", ["distributed provider", "source of invalidation events", "removeAll chunking"], "amber", 1110, 615, 360, 100),
      card("listener", "JCacheEntryEventListener", ["created/updated -> putAll", "removed/expired -> removeAll"], "pink", 625, 895, 390, 72),
      card("sync", "syncBackCache", ["put/remove/replace", "remote timeout guard"], "gray", 735, 615, 270, 86),
    ],
    routes: [
      route("caller", "near", "JCache calls", "blue", [{ x: 425, y: 291 }, { x: 670, y: 291 }]),
      route("near", "front", "read first / fill", "teal", [{ x: 670, y: 300 }, { x: 620, y: 300 }, { x: 620, y: 500 }, { x: 435, y: 500 }, { x: 435, y: 615 }], { allowDetour: true }),
      route("front", "back", "front miss lookup", "amber", [{ x: 615, y: 685 }, { x: 650, y: 685 }, { x: 650, y: 745 }, { x: 1290, y: 745 }, { x: 1290, y: 715 }], { allowDetour: true, showLabel: false }),
      route("sync", "back", "write-through", "gray", [{ x: 1005, y: 658 }, { x: 1110, y: 658 }], { showLabel: false }),
      route("near", "sync", "mutations", "green", [{ x: 1060, y: 286 }, { x: 1095, y: 286 }, { x: 1095, y: 570 }, { x: 870, y: 570 }, { x: 870, y: 615 }], { allowDetour: true }),
      route("back", "listener", "entry events", "pink", [{ x: 1380, y: 715 }, { x: 1380, y: 805 }, { x: 950, y: 805 }, { x: 950, y: 895 }], { allowDetour: true }),
      route("listener", "front", "invalidate/update", "pink", [{ x: 625, y: 931 }, { x: 200, y: 931 }, { x: 200, y: 665 }, { x: 255, y: 665 }], { allowDetour: true, showLabel: false }),
      route("config", "near", "configures", "purple", [{ x: 865, y: 365 }, { x: 865, y: 339 }], { showLabel: false }),
    ],
  },
  {
    file: "cache-cache-core-diagram-06",
    kind: "architecture",
    title: "SuspendNearJCache Coroutine Two-Tier Cache",
    subtitle: "Coroutine callers use SuspendJCache while listener work is launched off the JCache callback thread.",
    intent: "Explain SuspendNearJCache runtime flow, Flow entries, coroutine listener synchronization, and listener-free construction option.",
    width: 1820,
    height: 1180,
    titleBottom: 138,
    footerTop: 1076,
    groups: [
      group("caller", "Coroutine caller", 105, 185, 370, 175),
      group("facade", "SuspendNearJCache facade and listener setup", 555, 165, 1075, 315),
      group("tiers", "Suspend cache tiers", 155, 565, 1500, 210),
      group("events", "Listener and compatibility boundary", 235, 840, 1350, 165),
    ],
    nodes: [
      card("caller", "Coroutine service code", ["suspend calls", "Flow collection"], "blue", 145, 250, 295, 82),
      card("near", "SuspendNearJCache<K,V>", ["SuspendJCache<K,V> by backCache", "getDeeply fills front", "put/remove update both tiers"], "green", 675, 230, 430, 108),
      card("entries", "entries() / getAll()", ["front Flow", "SuspendJCacheEntry<K,V>"], "teal", 700, 370, 380, 78),
      card("front", "Front SuspendJCache", ["CaffeineSuspendJCache", "local fast path", "clear() is local"], "teal", 245, 625, 370, 96),
      card("back", "Back SuspendJCache", ["Lettuce / Redisson / Hazelcast", "distributed storage", "listener source when supported"], "amber", 1160, 625, 390, 96),
      card("listener", "SuspendJCacheEntryEventListener", ["SupervisorJob + Dispatchers.IO", "callback thread is not blocked"], "pink", 520, 900, 430, 72),
      card("without", "withoutListener(...)", ["Hazelcast client JCache", "non-serializable listener fallback"], "purple", 1065, 900, 390, 72),
      card("factory", "Factory registration", ["registerCacheEntryListener", "front cache target"], "gray", 1265, 370, 335, 78),
    ],
    routes: [
      route("caller", "near", "suspend API", "blue", [{ x: 440, y: 291 }, { x: 675, y: 291 }]),
      route("near", "entries", "Flow entries", "teal", [{ x: 890, y: 338 }, { x: 890, y: 370 }], { showLabel: false }),
      route("near", "front", "front fast path", "teal", [{ x: 680, y: 338 }, { x: 680, y: 500 }, { x: 430, y: 500 }, { x: 430, y: 625 }], { allowDetour: true }),
      route("front", "back", "miss lookup / fill", "amber", [{ x: 615, y: 673 }, { x: 1160, y: 673 }]),
      route("near", "back", "write-through", "green", [{ x: 1105, y: 286 }, { x: 1125, y: 286 }, { x: 1125, y: 520 }, { x: 1355, y: 520 }, { x: 1355, y: 625 }], { allowDetour: true }),
      route("factory", "back", "registers listener", "gray", [{ x: 1432, y: 448 }, { x: 1432, y: 625 }]),
      route("back", "listener", "entry events", "pink", [{ x: 1355, y: 721 }, { x: 1355, y: 805 }, { x: 900, y: 805 }, { x: 900, y: 900 }], { allowDetour: true }),
      route("listener", "front", "updates front", "pink", [{ x: 520, y: 936 }, { x: 200, y: 936 }, { x: 200, y: 673 }, { x: 245, y: 673 }], { allowDetour: true, showLabel: false }),
      route("without", "back", "listener-free option", "purple", [{ x: 1420, y: 900 }, { x: 1420, y: 721 }], { dashed: true }),
    ],
  },
  {
    file: "cache-cache-hazelcast-diagram-01",
    kind: "class",
    title: "Hazelcast NearCache Class Structure",
    subtitle: "Native NearCache implementations share the core contracts while IMap listener invalidation stays in the Hazelcast module.",
    intent: "Explain the Hazelcast native NearCache class structure, factory boundary, front cache, IMap backend, and listener invalidation.",
    width: 1880,
    height: 1240,
    titleBottom: 138,
    footerTop: 1136,
    groups: [
      group("contracts", "Core contracts", 92, 170, 1180, 210),
      group("impl", "Hazelcast native implementations", 92, 455, 1180, 215),
      group("factory", "Factory and JCache boundary", 1335, 210, 410, 405),
      group("runtime", "Runtime collaborators", 100, 730, 1680, 320),
    ],
    nodes: [
      uml("ops", "interface", "NearCacheOperations<V>", ["blocking front-first contract", "stats and lifecycle", "write-through operations"], "blue", 175, 230, 430, 126),
      uml("suspendOps", "interface", "SuspendNearCacheOperations<V>", ["suspend remote calls", "same operation families", "coroutine lifecycle"], "teal", 745, 230, 450, 126),
      uml("near", "class", "HazelcastNearCache<V>", ["front: CaffeineHazelcastLocalCache", "back: IMap<String,V>", "listenerId from addEntryListener"], "green", 175, 520, 430, 126),
      uml("suspendNear", "class", "HazelcastSuspendNearCache<V>", ["getAsync / setAsync / deleteAsync", "bulk getAll via IMap", "same front cache contract"], "green", 745, 520, 450, 126),
      uml("local", "interface", "HazelcastLocalCache<K,V>", ["get / put / invalidate", "invalidateAll / clear", "size and stats"], "teal", 185, 875, 390, 124),
      uml("caffeine", "class", "CaffeineHazelcastLocalCache<V>", ["Caffeine-backed front tier", "expiration and max size", "local invalidation target"], "amber", 720, 875, 410, 124),
      card("imap", "Hazelcast IMap", ["distributed back cache", "getAll / putAll bulk path", "EntryListener event source"], "purple", 1240, 910, 375, 96),
      card("listener", "HazelcastEntryEventListener", ["entryAdded/Updated -> invalidate", "removed/evicted -> invalidate", "runs in client JVM"], "pink", 1190, 790, 405, 76),
      card("factory", "HazelcastCaches", ["nearCache / suspendNearCache", "nearJCache / suspendNearJCache", "stable artifact/package names"], "blue", 1365, 285, 350, 96),
      card("jcache", "JCache factory note", ["listener-free public factory path", "read-through/write-through only", "direct listener-backed path unsupported"], "gray", 1365, 440, 350, 108),
    ],
    routes: [
      route("near", "ops", "implements", "green", [{ x: 540, y: 520 }, { x: 540, y: 356 }]),
      route("suspendNear", "suspendOps", "implements", "green", [{ x: 970, y: 520 }, { x: 970, y: 356 }]),
      route("caffeine", "local", "implements", "amber", [{ x: 720, y: 933 }, { x: 575, y: 933 }]),
      route("imap", "listener", "entry events", "pink", [{ x: 1428, y: 910 }, { x: 1428, y: 866 }]),
      route("listener", "caffeine", "invalidates", "pink", [{ x: 1190, y: 828 }, { x: 1145, y: 828 }, { x: 1145, y: 850 }, { x: 925, y: 850 }, { x: 925, y: 875 }], { allowDetour: true }),
      route("factory", "jcache", "documents", "gray", [{ x: 1540, y: 381 }, { x: 1540, y: 440 }], { dashed: true }),
    ],
  },
  {
    file: "cache-cache-hazelcast-diagram-02",
    kind: "architecture",
    title: "Hazelcast Two-Tier NearCache Runtime",
    subtitle: "Write-through reads and writes use Caffeine plus IMap; resilient mode adds an async remote-write lane.",
    intent: "Explain Hazelcast NearCache runtime flow, listener invalidation, and resilient write-behind separation.",
    width: 1900,
    height: 1200,
    titleBottom: 138,
    footerTop: 1096,
    groups: [
      group("caller", "Application boundary", 100, 165, 1700, 195),
      group("facade", "NearCache facade", 100, 390, 1700, 220),
      group("tiers", "Two cache tiers", 100, 640, 1700, 220),
      group("resilience", "Optional resilient write-behind", 100, 890, 1700, 165),
    ],
    nodes: [
      card("caller", "Application code", ["blocking or suspend API", "String keys / typed values"], "blue", 140, 250, 285, 76),
      card("facade", "HazelcastNearCache variants", ["front hit returns immediately", "front miss loads IMap", "writes update both tiers"], "green", 560, 455, 420, 108),
      card("config", "HazelcastNearCacheConfig", ["cacheName validation", "maxLocalSize > 0", "optional expiration rules"], "purple", 1080, 466, 400, 86),
      card("front", "Caffeine front cache", ["local memory tier", "fast contains/get", "clearLocal keeps IMap"], "teal", 230, 705, 370, 96),
      card("imap", "Hazelcast IMap", ["distributed remote tier", "getAll / putAll", "EntryListener source"], "amber", 1235, 705, 380, 96),
      card("listener", "IMap EntryListener", ["client-side event callback", "invalidates front keys"], "pink", 735, 710, 355, 86),
      card("queue", "Write-behind queue", ["front updated immediately", "remote write retry", "stale-read prevention"], "green", 520, 955, 360, 76),
      card("strategy", "Failure strategy", ["retry policy", "graceful degradation", "visible resilience config"], "red", 1040, 955, 350, 76),
    ],
    routes: [
      route("caller", "facade", "cache API", "blue", [{ x: 282, y: 326 }, { x: 282, y: 375 }, { x: 770, y: 375 }, { x: 770, y: 455 }], { allowDetour: true }),
      route("config", "facade", "configures", "purple", [{ x: 1080, y: 509 }, { x: 980, y: 509 }], { showLabel: false }),
      route("facade", "front", "read first / fill", "teal", [{ x: 680, y: 563 }, { x: 680, y: 625 }, { x: 415, y: 625 }, { x: 415, y: 705 }], { allowDetour: true }),
      route("front", "imap", "miss lookup", "amber", [{ x: 415, y: 801 }, { x: 415, y: 875 }, { x: 1425, y: 875 }, { x: 1425, y: 801 }], { allowDetour: true }),
      route("facade", "imap", "write-through", "green", [{ x: 860, y: 563 }, { x: 860, y: 625 }, { x: 1425, y: 625 }, { x: 1425, y: 705 }], { allowDetour: true }),
      route("imap", "listener", "entry events", "pink", [{ x: 1235, y: 753 }, { x: 1090, y: 753 }]),
      route("listener", "front", "invalidate", "pink", [{ x: 735, y: 753 }, { x: 600, y: 753 }]),
      route("queue", "strategy", "retry / fallback", "red", [{ x: 880, y: 993 }, { x: 1040, y: 993 }], { dashed: true }),
    ],
  },
  {
    file: "cache-cache-lettuce-diagram-01",
    kind: "class",
    title: "Lettuce Native NearCache Class Structure",
    subtitle: "Native Lettuce NearCache variants use Redis CLIENT TRACKING for peer invalidation and explicit write-through updates.",
    intent: "Explain the Lettuce native NearCache class structure, local front cache, Redis back cache, and RESP3 tracking listener.",
    width: 1900,
    height: 1300,
    titleBottom: 138,
    footerTop: 1196,
    groups: [
      group("contracts", "Core contracts", 100, 165, 1680, 210),
      group("impl", "Lettuce native implementations", 100, 390, 1680, 205),
      group("runtime", "Redis and local collaborators", 100, 625, 1680, 315),
      group("factory", "Factory and configuration", 100, 970, 1680, 180),
    ],
    nodes: [
      uml("ops", "interface", "NearCacheOperations<V>", ["blocking near-cache API", "front/back statistics", "resource lifecycle"], "blue", 175, 230, 430, 126),
      uml("suspendOps", "interface", "SuspendNearCacheOperations<V>", ["suspend read/write API", "coroutine cleanup", "same statistics contract"], "teal", 745, 230, 450, 126),
      uml("near", "class", "LettuceNearCache<V>", ["RedisCommands + asyncCommands", "EVALSHA CAS path", "CLIENT TRACKING optional"], "green", 175, 455, 430, 126),
      uml("suspendNear", "class", "LettuceSuspendNearCache<V>", ["RedisCoroutinesCommands", "async mget for misses", "CancellationException propagates"], "green", 745, 455, 450, 126),
      uml("local", "interface", "LettuceLocalCache<K,V>", ["get / put / invalidate", "invalidateAll / clear", "Caffeine stats"], "teal", 210, 760, 390, 124),
      uml("caffeine", "class", "LettuceCaffeineLocalCache<V>", ["Caffeine-backed L1", "max size and expiration", "local invalidation target"], "amber", 685, 760, 410, 124),
      card("redis", "Redis via Lettuce", ["namespaced keys", "GET/SET/UNLINK/SCAN", "RESP3 push channel"], "purple", 1225, 815, 390, 96),
      card("tracking", "TrackingInvalidationListener", ["CLIENT TRACKING ON NOLOOP", "prefix-aware key decode", "full flush clears local"], "pink", 1185, 690, 405, 76),
      card("factory", "LettuceCaches", ["nearCache / suspendNearCache", "nearJCache / suspendNearJCache", "resilient variants"], "blue", 475, 1030, 390, 82),
      card("config", "LettuceNearCacheConfig", ["cacheName and keyPrefix", "redisTtl and RESP3 flag", "batch/retry size validation"], "purple", 1015, 1030, 405, 92),
    ],
    routes: [
      route("near", "ops", "implements", "green", [{ x: 540, y: 455 }, { x: 540, y: 356 }]),
      route("suspendNear", "suspendOps", "implements", "green", [{ x: 970, y: 455 }, { x: 970, y: 356 }]),
      route("caffeine", "local", "implements", "amber", [{ x: 685, y: 818 }, { x: 600, y: 818 }], { showLabel: false }),
      route("redis", "tracking", "push invalidation", "pink", [{ x: 1420, y: 815 }, { x: 1420, y: 766 }]),
      route("tracking", "caffeine", "invalidates", "pink", [{ x: 1185, y: 728 }, { x: 1135, y: 728 }, { x: 1135, y: 745 }, { x: 890, y: 745 }, { x: 890, y: 760 }], { allowDetour: true }),
      route("config", "factory", "feeds DSL", "purple", [{ x: 1015, y: 1071 }, { x: 865, y: 1071 }], { dashed: true }),
    ],
  },
  {
    file: "cache-cache-lettuce-diagram-02",
    kind: "architecture",
    title: "Lettuce JCache NearCache Composition",
    subtitle: "JCache-compatible factories compose a Caffeine front with Lettuce-backed JCache wrappers and listener propagation.",
    intent: "Explain the Lettuce JCache near-cache composition and how JCache listener events synchronize the front cache.",
    width: 1880,
    height: 1200,
    titleBottom: 138,
    footerTop: 1096,
    groups: [
      group("caller", "JCache caller boundary", 100, 165, 1680, 195),
      group("factory", "Lettuce factory and adapters", 100, 390, 1680, 220),
      group("tiers", "JCache tiers", 100, 640, 1680, 220),
      group("events", "JCache listener propagation", 100, 890, 1680, 165),
    ],
    nodes: [
      card("caller", "Application / JCache API", ["Cache<K,V>", "SuspendJCache<String,V>"], "blue", 140, 250, 285, 76),
      card("factory", "LettuceCaches", ["nearJCache()", "suspendNearJCache()", "codec and config DSL"], "blue", 500, 455, 325, 92),
      card("near", "NearJCache / SuspendNearJCache", ["front-first read", "back fill", "write-through"], "green", 910, 455, 395, 92),
      card("config", "NearJCacheConfig", ["cacheName", "isSynchronous", "remote sync timeout"], "purple", 1390, 462, 315, 78),
      card("front", "Caffeine front JCache", ["local fast path", "listener target", "no Redis roundtrip"], "teal", 230, 705, 370, 96),
      card("back", "LettuceJCache / LettuceSuspendJCache", ["Redis hash storage", "TTL contract", "close keeps data"], "amber", 1210, 705, 405, 96),
      card("listener", "CacheEntryListener", ["CREATED/UPDATED -> put", "REMOVED/EXPIRED -> remove", "peer front propagation"], "pink", 230, 950, 440, 88),
      card("map", "LettuceMap", ["HMGET batch check", "HSET/HSETEX fallback", "codec serialization"], "gray", 1265, 955, 315, 76),
    ],
    routes: [
      route("caller", "factory", "factory call", "blue", [{ x: 282, y: 326 }, { x: 282, y: 375 }, { x: 662, y: 375 }, { x: 662, y: 455 }], { allowDetour: true }),
      route("factory", "near", "creates", "blue", [{ x: 825, y: 501 }, { x: 910, y: 501 }], { showLabel: false }),
      route("config", "near", "configures", "purple", [{ x: 1390, y: 501 }, { x: 1305, y: 501 }], { showLabel: false }),
      route("near", "front", "read first", "teal", [{ x: 1030, y: 547 }, { x: 1030, y: 625 }, { x: 560, y: 625 }, { x: 560, y: 705 }], { allowDetour: true }),
      route("front", "back", "miss lookup / write", "amber", [{ x: 600, y: 753 }, { x: 1210, y: 753 }]),
      route("near", "back", "back delegate", "green", [{ x: 1180, y: 547 }, { x: 1180, y: 625 }, { x: 1412, y: 625 }, { x: 1412, y: 705 }], { allowDetour: true }),
      route("back", "listener", "entry events", "pink", [{ x: 1412, y: 801 }, { x: 1412, y: 875 }, { x: 730, y: 875 }, { x: 730, y: 994 }, { x: 670, y: 994 }], { allowDetour: true, showLabel: false }),
      route("listener", "front", "updates front", "pink", [{ x: 500, y: 950 }, { x: 500, y: 801 }], { showLabel: false }),
      route("back", "map", "stores hash", "gray", [{ x: 1510, y: 801 }, { x: 1510, y: 955 }]),
    ],
  },
  {
    file: "cache-cache-lettuce-diagram-03",
    kind: "architecture",
    title: "Lettuce Cache Stability Contracts",
    subtitle: "Independent runtime contracts protect atomic updates, deletes, close semantics, and memoizer recovery.",
    intent: "Summarize the Lettuce stability contracts described by the README and implemented across NearCache, JCache, and memoizers.",
    width: 1900,
    height: 1040,
    titleBottom: 138,
    footerTop: 936,
    groups: [
      group("cas", "Atomic replace path", 105, 195, 390, 670),
      group("delete", "Non-blocking delete path", 545, 195, 390, 670),
      group("close", "JCache close contract", 985, 195, 390, 670),
      group("memo", "Memoizer recovery", 1425, 195, 365, 670),
    ],
    nodes: [
      card("cas1", "replace(old,new)", ["NearCache API", "same sync/suspend contract"], "blue", 145, 275, 310, 76),
      card("cas2", "EVALSHA CAS", ["COMPARE_AND_SET SHA1", "20-byte script digest"], "purple", 145, 440, 310, 76),
      card("cas3", "NOSCRIPT fallback", ["retry with full EVAL", "identical semantics"], "amber", 145, 605, 310, 76),
      card("cas4", "front update on success", ["local value refreshed", "failure leaves value untouched"], "green", 145, 770, 310, 76),
      card("del1", "remove / clearBack", ["key-scoped delete", "bulk removal path"], "blue", 585, 275, 310, 76),
      card("del2", "UNLINK", ["background memory free", "client roundtrip stays O(1)"], "purple", 585, 440, 310, 76),
      card("del3", "SCAN namespace", ["cacheName:keyPrefix", "no FLUSHDB"], "amber", 585, 605, 310, 76),
      card("del4", "front invalidation", ["local remove/clear", "tracking push handles peers"], "green", 585, 770, 310, 76),
      card("close1", "LettuceJCache.close()", ["release resources", "do not delete Redis data"], "blue", 1025, 275, 310, 76),
      card("close2", "explicit clear()", ["data removal is caller-owned", "JSR-107 compliant"], "purple", 1025, 440, 310, 76),
      card("close3", "Suspend managers", ["non-cancellable cleanup", "finish remaining closes"], "amber", 1025, 605, 310, 76),
      card("close4", "listener cleanup", ["deregister resources", "connection handles released"], "green", 1025, 770, 310, 76),
      card("memo1", "in-flight promise", ["share concurrent work", "not durable cache data"], "blue", 1460, 275, 290, 76),
      card("memo2", "failure / cancel", ["complete exceptionally", "remove coordination state"], "red", 1460, 440, 290, 76),
      card("memo3", "remove(key,promise)", ["avoid race eviction", "only our promise removed"], "purple", 1460, 605, 290, 76),
      card("memo4", "next call recomputes", ["fresh evaluator run", "no failed value cached"], "green", 1460, 770, 290, 76),
    ],
    routes: [
      route("cas1", "cas2", "executes", "purple", [{ x: 300, y: 351 }, { x: 300, y: 440 }]),
      route("cas2", "cas3", "if NOSCRIPT", "amber", [{ x: 300, y: 516 }, { x: 300, y: 605 }], { dashed: true }),
      route("cas3", "cas4", "same result", "green", [{ x: 300, y: 681 }, { x: 300, y: 770 }]),
      route("del1", "del2", "uses", "purple", [{ x: 740, y: 351 }, { x: 740, y: 440 }]),
      route("del2", "del3", "scans", "amber", [{ x: 740, y: 516 }, { x: 740, y: 605 }]),
      route("del3", "del4", "clears", "green", [{ x: 740, y: 681 }, { x: 740, y: 770 }]),
      route("close1", "close2", "separates", "purple", [{ x: 1180, y: 351 }, { x: 1180, y: 440 }]),
      route("close2", "close3", "protects", "amber", [{ x: 1180, y: 516 }, { x: 1180, y: 605 }]),
      route("close3", "close4", "then releases", "green", [{ x: 1180, y: 681 }, { x: 1180, y: 770 }]),
      route("memo1", "memo2", "on error", "red", [{ x: 1605, y: 351 }, { x: 1605, y: 440 }]),
      route("memo2", "memo3", "atomic cleanup", "purple", [{ x: 1605, y: 516 }, { x: 1605, y: 605 }]),
      route("memo3", "memo4", "allows", "green", [{ x: 1605, y: 681 }, { x: 1605, y: 770 }]),
    ],
  },
  {
    file: "cache-cache-lettuce-diagram-04",
    kind: "architecture",
    title: "Lettuce RESP3 NearCache Runtime",
    subtitle: "The native runtime keeps L1 hits local and uses RESP3 tracking pushes to invalidate only matching cache-name keys.",
    intent: "Explain the Lettuce native runtime flow, key isolation, CLIENT TRACKING listener, and resilient write-behind option.",
    width: 1900,
    height: 1200,
    titleBottom: 138,
    footerTop: 1096,
    groups: [
      group("caller", "Application boundary", 100, 165, 1700, 195),
      group("facade", "Native Lettuce NearCache", 100, 390, 1700, 220),
      group("tiers", "Cache tiers", 100, 640, 1700, 220),
      group("events", "RESP3 tracking and resilience", 100, 890, 1700, 165),
    ],
    nodes: [
      card("caller", "Application code", ["blocking or suspend API", "typed values via codec"], "blue", 140, 250, 285, 76),
      card("facade", "LettuceNearCache variants", ["front hit is local", "miss loads Redis", "writes register tracking"], "green", 560, 455, 420, 108),
      card("keys", "Key isolation", ["{cacheName}:{key}", "SCAN only own prefix", "no database-wide flush"], "purple", 1080, 466, 400, 86),
      card("front", "LettuceCaffeineLocalCache", ["L1 Caffeine storage", "stats and max size", "invalidate target"], "teal", 230, 705, 370, 96),
      card("redis", "Redis backend", ["GET/SET with TTL", "UNLINK delete", "EVALSHA CAS"], "amber", 1235, 705, 380, 96),
      card("tracking", "TrackingInvalidationListener", ["CLIENT TRACKING ON NOLOOP", "decode ByteBuffer keys", "ignore other prefixes"], "pink", 710, 705, 405, 96),
      card("queue", "Resilient write lane", ["front update first", "async remote retry", "failure strategy"], "green", 520, 955, 365, 76),
      card("push", "Redis push message", ["invalidate key list", "null means full flush", "prefix filter before clear"], "red", 1350, 955, 360, 76),
    ],
    routes: [
      route("caller", "facade", "cache API", "blue", [{ x: 282, y: 326 }, { x: 282, y: 375 }, { x: 770, y: 375 }, { x: 770, y: 455 }], { allowDetour: true }),
      route("keys", "facade", "configures", "purple", [{ x: 1080, y: 509 }, { x: 980, y: 509 }], { showLabel: false }),
      route("facade", "front", "read first", "teal", [{ x: 680, y: 563 }, { x: 680, y: 625 }, { x: 415, y: 625 }, { x: 415, y: 705 }], { allowDetour: true }),
      route("front", "redis", "miss lookup", "amber", [{ x: 415, y: 801 }, { x: 415, y: 875 }, { x: 1425, y: 875 }, { x: 1425, y: 801 }], { allowDetour: true }),
      route("facade", "redis", "write-through", "green", [{ x: 860, y: 563 }, { x: 860, y: 625 }, { x: 1425, y: 625 }, { x: 1425, y: 705 }], { allowDetour: true }),
      route("redis", "tracking", "push event", "pink", [{ x: 1235, y: 753 }, { x: 1115, y: 753 }]),
      route("tracking", "front", "invalidate", "pink", [{ x: 710, y: 753 }, { x: 600, y: 753 }]),
      route("redis", "push", "invalidates", "red", [{ x: 1530, y: 801 }, { x: 1530, y: 955 }], { dashed: true }),
    ],
  },
];

const summaries = [];
for (const diagram of diagrams) {
  diagram.sourceEvidence = sourceEvidence;
  validateDiagram(diagram);
  const svg = renderSvg(diagram);
  assertSvg(diagram, svg);
  writeFileSync(join(OUT, `${diagram.file}.svg`), svg);
  const dot = renderDot(diagram);
  writeFileSync(join(OUT, `${diagram.file}.dot`), dot);
  execFileSync(DOT, ["-Tplain", join(OUT, `${diagram.file}.dot`), "-o", join(OUT, `${diagram.file}.plain`)], { stdio: "inherit" });
  execFileSync(DOT, ["-Tsvg", join(OUT, `${diagram.file}.dot`), "-o", join(OUT, `${diagram.file}-graphviz.svg`)], { stdio: "inherit" });
  execFileSync(DOT, ["-Tpng", join(OUT, `${diagram.file}.dot`), "-o", join(OUT, `${diagram.file}-graphviz.png`)], { stdio: "inherit" });
  execFileSync(RSVG, ["--format", "png", "--output", join(OUT, `${diagram.file}.png`), join(OUT, `${diagram.file}.svg`)], { stdio: "inherit" });
  const summary = geometrySummary(diagram);
  summaries.push(summary);
  console.log(summary);
}

writeFileSync(join(OUT, "cache-cache-core-geometry-summary.txt"), `${summaries.filter((line) => line.startsWith("cache-cache-core-")).join("\n")}\n`);
writeFileSync(join(OUT, "cache-cache-backend-geometry-summary.txt"), `${summaries.filter((line) => /cache-cache-(hazelcast|lettuce)-/.test(line)).join("\n")}\n`);

function group(id, title, x, y, w, h) {
  return { id, title, x, y, w, h };
}

function card(id, title, details, color, x, y, w, h) {
  return { id, title, details, color, x, y, w, h, kind: "card" };
}

function uml(id, stereotype, title, members, color, x, y, w, h) {
  return { id, stereotype, title, members, color, x, y, w, h, kind: "uml" };
}

function route(from, to, label, color, points, options = {}) {
  return {
    from,
    to,
    label,
    color,
    points,
    dashed: Boolean(options.dashed),
    allowDetour: Boolean(options.allowDetour),
    showLabel: options.showLabel !== false,
  };
}

function renderSvg(diagram) {
  const marker = diagram.kind === "sequence" ? 5 : 5;
  const footerY = footerTop(diagram);
  const frame = innerFrame(diagram);
  const paths = diagram.routes.map((item) => renderRoute(item)).join("\n");
  const groups = diagram.groups.map((item) => renderGroup(item)).join("\n");
  const nodes = diagram.nodes.map((item) => item.kind === "uml" ? renderUml(item) : renderCard(item)).join("\n");
  const summary = footerSummary(diagram);

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${diagram.width}" height="${diagram.height}" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-labelledby="title desc">
  <title id="title">${esc(diagram.title)}</title>
  <desc id="desc">${esc(diagram.intent)}</desc>
  <defs>
    <font-face font-family="${ARCH_FONT}" src="url('${fontUrl(fonts.arch)}')"/>
    <font-face font-family="${DETAIL_FONT}" src="url('${fontUrl(fonts.detail)}')"/>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.11"/></filter>
    <marker id="arrow" viewBox="0 0 ${marker} ${marker}" markerWidth="${marker}" markerHeight="${marker}" refX="4.2" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0 0 L 5 2.5 L 0 5 z" fill="context-stroke"/></marker>
    <marker id="hollow" viewBox="0 0 7 6" markerWidth="7" markerHeight="6" refX="6.5" refY="3" orient="auto"><path d="M 0.8 0.8 L 6.5 3 L 0.8 5.2 z" fill="#FFFFFF" stroke="context-stroke" stroke-width="1.2"/></marker>
    <style>
      .canvas{fill:#F6F9FC}.decor{fill:#FFFFFF;stroke:#C4D6E7;stroke-width:3.2;filter:url(#shadow)}
      .innerFrame{fill:#FBFDFF;stroke:#D9E4EF;stroke-width:2}.group{fill:#F3F7FB;stroke:#D7E2EC;stroke-width:2}
      .title{font-family:"${ARCH_FONT}";font-size:46px;fill:#22344A;font-weight:400}
      .subtitle{font-family:"${DETAIL_FONT}";font-size:18px;fill:#536476;font-weight:400}
      .groupTitle{font-family:"${ARCH_FONT}";font-size:23px;fill:#22344A;font-weight:400}
      .cardTitle,.classTitle{font-family:"${ARCH_FONT}";font-size:22px;fill:#22344A;font-weight:400}
      .stereo,.detail,.member,.small{font-family:"${DETAIL_FONT}";font-weight:400}
      .stereo{font-size:13px;fill:#687789}.detail{font-size:14px;fill:#42556B}.member{font-size:13px;fill:#42556B}.small{font-size:13px;fill:#627184}
      .card{filter:url(#shadow);stroke-width:2}.connector{fill:none;stroke-width:2.7;marker-end:url(#arrow);stroke-linejoin:round;stroke-linecap:round}.inherit{marker-end:url(#hollow)}.dashed{stroke-dasharray:7 6}
      .labelPill{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:1.1}
    </style>
  </defs>
  <rect class="canvas" width="${diagram.width}" height="${diagram.height}"/>
  <rect class="decor" x="44" y="36" width="${diagram.width - 88}" height="${diagram.height - 72}" rx="34"/>
  <rect class="innerFrame" x="${frame.x}" y="${frame.y}" width="${frame.w}" height="${frame.h}" rx="24"/>
  <text class="title" x="72" y="88">${esc(diagram.title)}</text>
  <text class="subtitle" x="76" y="121">${esc(diagram.subtitle)}</text>
${groups}
${paths}
${nodes}
  <g transform="translate(76,${footerY})">
    <rect class="labelPill" x="0" y="0" width="${diagram.width - 152}" height="48" rx="10"/>
    <text class="small" x="${(diagram.width - 152) / 2}" y="24" text-anchor="middle" dominant-baseline="middle">${esc(diagram.footer ?? "bluetape4k-projects / cache modules - github.com/bluetape4k/bluetape4k-projects")}</text>
  </g>
</svg>
`;
}

function renderGroup(item) {
  return `  <g id="panel-${esc(item.id)}">
    <rect class="group" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="20"/>
    <text class="groupTitle" x="${item.x + 30}" y="${item.y + 28}" dominant-baseline="middle">${esc(item.title)}</text>
  </g>`;
}

function renderCard(item) {
  const color = palette[item.color] || palette.gray;
  const lines = [item.title, ...item.details];
  const lineHeight = 19;
  const total = (lines.length - 1) * lineHeight;
  const text = lines.map((line, index) => {
    const cls = index === 0 ? "cardTitle" : "detail";
    const y = item.h / 2 - total / 2 + index * lineHeight;
    return `    <text class="${cls}" x="${item.w / 2}" y="${fmt(y)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`;
  }).join("\n");
  return `  <g id="node-${esc(item.id)}" transform="translate(${item.x},${item.y})">
    <rect class="card" x="0" y="0" width="${item.w}" height="${item.h}" rx="12" fill="${color.fill}" stroke="${color.stroke}"/>
${text}
  </g>`;
}

function renderUml(item) {
  const color = palette[item.color] || palette.gray;
  const header = 50;
  const memberTop = header + 12;
  const lineHeight = 18;
  const memberBlock = (item.members.length - 1) * lineHeight;
  const memberStart = memberTop + ((item.h - memberTop - 12) - memberBlock) / 2;
  const members = item.members.map((line, index) =>
    `    <text class="member" x="${item.w / 2}" y="${fmt(memberStart + index * lineHeight)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`
  ).join("\n");
  return `  <g id="node-${esc(item.id)}" transform="translate(${item.x},${item.y})">
    <rect class="card" x="0" y="0" width="${item.w}" height="${item.h}" rx="10" fill="${color.fill}" stroke="${color.stroke}"/>
    <line x1="0" y1="${header}" x2="${item.w}" y2="${header}" stroke="${color.stroke}" stroke-width="1.6"/>
    <text class="stereo" x="${item.w / 2}" y="17" text-anchor="middle" dominant-baseline="middle">&lt;&lt;${esc(item.stereotype)}&gt;&gt;</text>
    <text class="classTitle" x="${item.w / 2}" y="36" text-anchor="middle" dominant-baseline="middle">${esc(item.title)}</text>
${members}
  </g>`;
}

function renderRoute(item) {
  const color = (palette[item.color] || palette.gray).line;
  const d = item.points.map((point, index) => `${index === 0 ? "M" : "L"} ${fmt(point.x)} ${fmt(point.y)}`).join(" ");
  const dashed = item.dashed ? " dashed" : "";
  const inherit = /implements/.test(item.label) ? " inherit" : "";
  const label = routeLabel(item, color);
  return `  <path class="connector${dashed}${inherit}" data-edge="${esc(item.from)}-&gt;${esc(item.to)}" data-label="${esc(item.label)}" d="${d}" stroke="${color}"/>
${label}`;
}

function routeLabel(item, color) {
  if (!item.label || item.showLabel === false) return "";
  const { x, y } = labelAnchor(item);
  const width = Math.max(70, item.label.length * 7.4 + 20);
  return `  <g transform="translate(${fmt(x - width / 2)},${fmt(y - 13)})">
    <rect class="labelPill" x="0" y="0" width="${fmt(width)}" height="26" rx="8"/>
    <text class="small" x="${fmt(width / 2)}" y="13" text-anchor="middle" dominant-baseline="middle" fill="${color}">${esc(item.label)}</text>
  </g>`;
}

function labelAnchor(item) {
  const segments = [];
  for (let index = 1; index < item.points.length; index += 1) {
    const a = item.points[index - 1];
    const b = item.points[index];
    const length = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    segments.push({ a, b, length, horizontal: near(a.y, b.y) });
  }
  const horizontal = segments
    .filter((segment) => segment.horizontal && segment.length >= 70)
    .sort((a, b) => b.length - a.length)[0];
  if (horizontal) {
    return {
      x: (horizontal.a.x + horizontal.b.x) / 2,
      y: horizontal.a.y - 24,
    };
  }
  const vertical = segments.sort((a, b) => b.length - a.length)[0];
  const labelWidth = Math.max(70, item.label.length * 7.4 + 20);
  return {
    x: (vertical.a.x + vertical.b.x) / 2 + labelWidth / 2 + 22,
    y: (vertical.a.y + vertical.b.y) / 2,
  };
}

function renderDot(diagram) {
  const lines = [
    "digraph G {",
    `  graph [rankdir=TB, bgcolor="white", splines=ortho, nodesep=0.6, ranksep=0.85, fontname="${DETAIL_FONT}", label="${escDot(diagram.title)} evidence", labelloc=t];`,
    `  node [shape=box, style="rounded,filled", fontname="${ARCH_FONT}", fontsize=18, color="#D7E2EC", fillcolor="#F7FAFC"];`,
    `  edge [fontname="${DETAIL_FONT}", fontsize=11, color="#56708C", arrowsize=0.8];`,
  ];
  for (const item of diagram.nodes) {
    const color = palette[item.color] || palette.gray;
    lines.push(`  "${escDot(item.id)}" [label="${escDot(item.title)}", fillcolor="${color.fill}", color="${color.stroke}"];`);
  }
  for (const item of diagram.routes) {
    const color = (palette[item.color] || palette.gray).line;
    lines.push(`  "${escDot(item.from)}" -> "${escDot(item.to)}" [label="${escDot(item.label)}", color="${color}", style="${item.dashed ? "dashed" : "solid"}"];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function validateDiagram(diagram) {
  const errors = [];
  const ids = new Set(diagram.nodes.map((node) => node.id));
  for (const item of diagram.routes) {
    if (!ids.has(item.from) || !ids.has(item.to)) errors.push(`${item.from}->${item.to}: missing endpoint`);
    if (!item.label) errors.push(`${item.from}->${item.to}: missing route label`);
    if (item.points.length < 2) errors.push(`${item.from}->${item.to}: needs at least two points`);
    for (let index = 1; index < item.points.length; index += 1) {
      const a = item.points[index - 1];
      const b = item.points[index];
      if (!orthogonal(a, b)) errors.push(`${item.from}->${item.to}: non-orthogonal segment ${index}`);
      for (const node of diagram.nodes) {
        if (node.id === item.from || node.id === item.to) continue;
        if (segmentIntersectsRectInterior(a, b, node, MIN_CLEARANCE)) {
          errors.push(`${item.from}->${item.to}: segment ${index} crosses ${node.id}`);
        }
      }
    }
    const fromNode = diagram.nodes.find((node) => node.id === item.from);
    const toNode = diagram.nodes.find((node) => node.id === item.to);
    if (fromNode && toNode) {
      const fromError = endpointTangent(item.points[0], item.points[1], fromNode, "source");
      const toError = endpointTangent(item.points.at(-1), item.points.at(-2), toNode, "target");
      if (fromError) errors.push(`${item.from}->${item.to}: ${fromError}`);
      if (toError) errors.push(`${item.from}->${item.to}: ${toError}`);
    }
    if (item.showLabel !== false) {
      const label = routeLabelBox(item);
      for (const node of diagram.nodes) {
        if (rectsOverlap(label, node, 8)) errors.push(`${item.from}->${item.to}: route label overlaps ${node.id}`);
      }
      for (const group of diagram.groups) {
        if (rectsOverlap(label, groupLabelBox(group), 8)) errors.push(`${item.from}->${item.to}: route label overlaps ${group.id} layer label gutter`);
      }
      for (const otherRoute of diagram.routes) {
        for (let index = 1; index < otherRoute.points.length; index += 1) {
          if (segmentIntersectsRectInterior(otherRoute.points[index - 1], otherRoute.points[index], label, 1)) {
            errors.push(`${item.from}->${item.to}: route label covers ${otherRoute.from}->${otherRoute.to} segment ${index}`);
          }
        }
      }
    }
  }
  for (let i = 0; i < diagram.routes.length; i += 1) {
    for (let j = i + 1; j < diagram.routes.length; j += 1) {
      const conflicts = routeConflicts(diagram.routes[i], diagram.routes[j]);
      for (const conflict of conflicts) errors.push(conflict);
    }
  }
  for (let i = 0; i < diagram.nodes.length; i += 1) {
    for (let j = i + 1; j < diagram.nodes.length; j += 1) {
      if (rectsOverlap(diagram.nodes[i], diagram.nodes[j], 2)) errors.push(`${diagram.nodes[i].id}<->${diagram.nodes[j].id}: node overlap`);
    }
  }
  for (let i = 0; i < diagram.groups.length; i += 1) {
    for (let j = i + 1; j < diagram.groups.length; j += 1) {
      if (rectsOverlap(diagram.groups[i], diagram.groups[j], 6)) errors.push(`${diagram.groups[i].id}<->${diagram.groups[j].id}: layer/panel overlap`);
    }
  }
  const frame = innerFrame(diagram);
  for (const item of diagram.groups) {
    if (!rectInsideRect(item, frame, { left: 8, right: 8, top: 8, bottom: 8 })) {
      errors.push(`${item.id}: layer/panel exceeds inner frame bounds`);
    }
    const label = groupLabelBox(item);
    for (const node of diagram.nodes) {
      if (rectsOverlap(label, node, 4)) errors.push(`${node.id}: card overlaps ${item.id} layer label gutter`);
    }
    for (const edge of diagram.routes) {
      for (let index = 1; index < edge.points.length; index += 1) {
        if (segmentIntersectsRectInterior(edge.points[index - 1], edge.points[index], label, 2)) {
          errors.push(`${edge.from}->${edge.to}: segment ${index} crosses ${item.id} layer label gutter`);
        }
      }
    }
  }
  for (const node of diagram.nodes) {
    const owner = diagram.groups.find((item) => pointInsideRect(center(node), item));
    if (!owner) {
      errors.push(`${node.id}: no containing layer/panel`);
      continue;
    }
    if (!rectInsideRect(node, owner, { left: 16, right: 16, top: 60, bottom: 14 })) {
      errors.push(`${node.id}: card exceeds ${owner.id} layer/panel bounds`);
    }
    const paddingError = textBlockPaddingError(node);
    if (paddingError) errors.push(`${node.id}: ${paddingError}`);
  }
  const m = margins(diagram);
  if (Math.max(Math.abs(m.left - m.right), Math.abs(m.top - m.bottom)) > 230) {
    errors.push(`margin imbalance: ${m.left}/${m.right}/${m.top}/${m.bottom}`);
  }
  if (errors.length) throw new Error(`${diagram.file} failed gate:\n${errors.map((line) => `- ${line}`).join("\n")}`);
}

function assertSvg(diagram, svg) {
  const errors = [];
  if (!svg.includes(ARCH_FONT)) errors.push(`missing ${ARCH_FONT}`);
  if (!svg.includes(DETAIL_FONT)) errors.push(`missing ${DETAIL_FONT}`);
  if (/\b(Inter|Arial|Helvetica)\b/.test(svg)) errors.push("forbidden UI font");
  if (!/markerWidth="5" markerHeight="5"/.test(svg)) errors.push("expected 5x5 connector marker");
  if (!/class="decor"/.test(svg)) errors.push("missing visible decorator");
  if (errors.length) throw new Error(`${diagram.file}: ${errors.join("; ")}`);
}

function geometrySummary(diagram) {
  const m = margins(diagram);
  const segments = diagram.routes.reduce((sum, item) => sum + item.points.length - 1, 0);
  return `${diagram.file}: nodes=${diagram.nodes.length} routes=${diagram.routes.length} segments=${segments} badEndpointAngle=0 badBends=0 interiorCrossings=0 nodeOverlaps=0 laneClearance=0 margins=${m.left}/${m.right}/${m.top}/${m.bottom} titleGap=${Math.min(...diagram.nodes.map((node) => node.y)) - diagram.titleBottom} sourceIntent="${diagram.intent}" bestPractice=${diagram.kind === "class" ? "class-diagram-style-v3" : "architecture-layered-exposed-mvc-virtualthread"} rejectedPatterns=relationship-heavy-grid,card-penetrating-connector,tangent-or-zero-degree-endpoint,text-overflow-or-bad-centering`;
}

function footerSummary(diagram) {
  return diagram.footer ?? "bluetape4k-projects / cache modules - github.com/bluetape4k/bluetape4k-projects";
}

function footerTop(diagram) {
  return Math.min(diagram.footerTop ?? diagram.height - 104, diagram.height - 104);
}

function routeLabelBox(item) {
  const { x, y } = labelAnchor(item);
  const width = Math.max(70, item.label.length * 7.4 + 20);
  return { x: x - width / 2, y: y - 13, w: width, h: 26 };
}

function groupLabelBox(item) {
  return {
    x: item.x + 20,
    y: item.y + 10,
    w: Math.min(item.w - 40, Math.max(220, item.title.length * 10 + 50)),
    h: 42,
  };
}

function textBlockPaddingError(node) {
  if (node.kind === "card") {
    const lineCount = 1 + node.details.length;
    const textSpan = (lineCount - 1) * 19;
    const verticalPadding = (node.h - textSpan) / 2;
    return verticalPadding < 9 ? `normal-card text block is cramped: padding=${fmt(verticalPadding)}` : null;
  }
  if (node.kind === "uml") {
    const memberTop = 62;
    const memberSpan = (node.members.length - 1) * 18;
    const memberPadding = (node.h - memberTop - 12 - memberSpan) / 2;
    return memberPadding < 7 ? `UML member block is cramped: padding=${fmt(memberPadding)}` : null;
  }
  return null;
}

function innerFrame(diagram) {
  return {
    x: 82,
    y: 154,
    w: diagram.width - 164,
    h: Math.max(120, footerTop(diagram) - 154 - 28),
  };
}

function margins(diagram) {
  return {
    left: Math.round(Math.min(...diagram.nodes.map((node) => node.x))),
    right: Math.round(diagram.width - Math.max(...diagram.nodes.map((node) => node.x + node.w))),
    top: Math.round(Math.min(...diagram.nodes.map((node) => node.y)) - diagram.titleBottom),
    bottom: Math.round(footerTop(diagram) - Math.max(...diagram.nodes.map((node) => node.y + node.h))),
  };
}

function segmentIntersectsRectInterior(a, b, rect, pad = 0) {
  const minX = rect.x - pad;
  const maxX = rect.x + rect.w + pad;
  const minY = rect.y - pad;
  const maxY = rect.y + rect.h + pad;
  if (near(a.y, b.y)) {
    if (a.y <= minY || a.y >= maxY) return false;
    return Math.max(a.x, b.x) > minX && Math.min(a.x, b.x) < maxX;
  }
  if (near(a.x, b.x)) {
    if (a.x <= minX || a.x >= maxX) return false;
    return Math.max(a.y, b.y) > minY && Math.min(a.y, b.y) < maxY;
  }
  return false;
}

function endpointTangent(endpoint, nextPoint, node, role) {
  const side = rectSide(endpoint, node);
  if (!side) return `${role} endpoint is not on ${node.id} boundary`;
  if ((side === "left" || side === "right") && near(endpoint.x, nextPoint.x)) {
    return `${role} endpoint tangent to ${node.id} ${side} edge`;
  }
  if ((side === "top" || side === "bottom") && near(endpoint.y, nextPoint.y)) {
    return `${role} endpoint tangent to ${node.id} ${side} edge`;
  }
  return null;
}

function rectSide(point, rect) {
  const onLeft = near(point.x, rect.x) && point.y >= rect.y - 0.5 && point.y <= rect.y + rect.h + 0.5;
  const onRight = near(point.x, rect.x + rect.w) && point.y >= rect.y - 0.5 && point.y <= rect.y + rect.h + 0.5;
  const onTop = near(point.y, rect.y) && point.x >= rect.x - 0.5 && point.x <= rect.x + rect.w + 0.5;
  const onBottom = near(point.y, rect.y + rect.h) && point.x >= rect.x - 0.5 && point.x <= rect.x + rect.w + 0.5;
  if (onLeft) return "left";
  if (onRight) return "right";
  if (onTop) return "top";
  if (onBottom) return "bottom";
  return null;
}

function routeConflicts(a, b) {
  const errors = [];
  for (let i = 1; i < a.points.length; i += 1) {
    for (let j = 1; j < b.points.length; j += 1) {
      const s1 = { a: a.points[i - 1], b: a.points[i] };
      const s2 = { a: b.points[j - 1], b: b.points[j] };
      const conflict = segmentConflict(s1, s2);
      if (conflict) errors.push(`${a.from}->${a.to} overlaps/crosses ${b.from}->${b.to}: ${conflict}`);
    }
  }
  return errors;
}

function segmentConflict(s1, s2) {
  if (samePoint(s1.a, s2.a) || samePoint(s1.a, s2.b) || samePoint(s1.b, s2.a) || samePoint(s1.b, s2.b)) return null;
  const h1 = near(s1.a.y, s1.b.y);
  const h2 = near(s2.a.y, s2.b.y);
  if (h1 && h2) {
    if (!near(s1.a.y, s2.a.y)) return null;
    const overlap = rangeOverlap([s1.a.x, s1.b.x], [s2.a.x, s2.b.x]);
    return overlap > 8 ? `horizontal shared lane at y=${fmt(s1.a.y)} length=${fmt(overlap)}` : null;
  }
  if (!h1 && !h2) {
    if (!near(s1.a.x, s2.a.x)) return null;
    const overlap = rangeOverlap([s1.a.y, s1.b.y], [s2.a.y, s2.b.y]);
    return overlap > 8 ? `vertical shared lane at x=${fmt(s1.a.x)} length=${fmt(overlap)}` : null;
  }
  const h = h1 ? s1 : s2;
  const v = h1 ? s2 : s1;
  const hx = normalizeRange([h.a.x, h.b.x]);
  const vy = normalizeRange([v.a.y, v.b.y]);
  const x = v.a.x;
  const y = h.a.y;
  if (x > hx[0] + 0.5 && x < hx[1] - 0.5 && y > vy[0] + 0.5 && y < vy[1] - 0.5) {
    return `orthogonal crossing at ${fmt(x)},${fmt(y)}`;
  }
  return null;
}

function rangeOverlap(a, b) {
  const [a0, a1] = normalizeRange(a);
  const [b0, b1] = normalizeRange(b);
  return Math.max(0, Math.min(a1, b1) - Math.max(a0, b0));
}

function normalizeRange(range) {
  return [Math.min(range[0], range[1]), Math.max(range[0], range[1])];
}

function samePoint(a, b) {
  return near(a.x, b.x) && near(a.y, b.y);
}

function center(rect) {
  return { x: rect.x + rect.w / 2, y: rect.y + rect.h / 2 };
}

function pointInsideRect(point, rect) {
  return point.x >= rect.x && point.x <= rect.x + rect.w && point.y >= rect.y && point.y <= rect.y + rect.h;
}

function rectInsideRect(rect, container, pad) {
  return rect.x >= container.x + pad.left &&
    rect.y >= container.y + pad.top &&
    rect.x + rect.w <= container.x + container.w - pad.right &&
    rect.y + rect.h <= container.y + container.h - pad.bottom;
}

function rectsOverlap(a, b, pad = 0) {
  return a.x < b.x + b.w + pad && a.x + a.w + pad > b.x && a.y < b.y + b.h + pad && a.y + a.h + pad > b.y;
}

function orthogonal(a, b) {
  return near(a.x, b.x) || near(a.y, b.y);
}

function near(a, b) {
  return Math.abs(a - b) <= 0.5;
}

function fmt(value) {
  return Number(value.toFixed(2)).toString();
}

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function escDot(value) {
  return String(value ?? "").replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}

function fontUrl(file) {
  return `file://${file.replaceAll("'", "%27")}`;
}

function findExecutable(name, candidates) {
  for (const candidate of candidates) if (existsSync(candidate)) return candidate;
  for (const dir of (process.env.PATH || "").split(":")) {
    const candidate = join(dir, name);
    if (existsSync(candidate)) return candidate;
  }
  return null;
}

function findFontFile(name) {
  const roots = [
    `${process.env.HOME}/Library/Fonts`,
    "/Library/Fonts",
    "/System/Library/Fonts",
    "/usr/local/share/fonts",
    "/opt/homebrew/share/fonts",
  ];
  const normalized = name.toLowerCase().replace(/[^a-z0-9]/g, "");
  for (const root of roots) {
    const found = walk(root).find((file) => file.toLowerCase().replace(/[^a-z0-9]/g, "").includes(normalized));
    if (found) return found;
  }
  try {
    const lines = execFileSync("fc-list", [], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }).split("\n");
    const line = lines.find((item) => item.toLowerCase().includes(name.toLowerCase()));
    if (line) return line.split(":")[0];
  } catch {
    // fontconfig is optional on macOS.
  }
  return null;
}

function walk(root) {
  if (!root || !existsSync(root)) return [];
  const out = [];
  const stack = [root];
  while (stack.length) {
    const current = stack.pop();
    for (const name of readdirSync(current)) {
      const file = join(current, name);
      const stat = statSync(file);
      if (stat.isDirectory()) stack.push(file);
      else if (/\.(ttf|otf|ttc)$/i.test(file)) out.push(file);
    }
  }
  return out;
}
