#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";
import {join} from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
    "spring-boot/hibernate-lettuce/README.md",
    "spring-boot/hibernate-lettuce/README.ko.md",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt",
    "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt",
    "cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt",
];

for (const source of sources) {
    if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
    const text = readFileSync(join(ROOT, source), "utf8");
    if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /## UML[\s\S]*spring-boot-hibernate-lettuce-diagram-01\.png/, "README UML slot");
assertContains(sources[2], /@ConfigurationProperties\(prefix = "bluetape4k\.cache\.lettuce-near"\)[\s\S]*data class LettuceNearCacheSpringProperties/, "configuration properties");
assertContains(sources[3], /class LettuceNearCacheHibernateAutoConfiguration[\s\S]*HibernatePropertiesCustomizer[\s\S]*hibernate\.cache\.region\.factory_class/, "Hibernate customizer");
assertContains(sources[4], /class LettuceNearCacheActuatorAutoConfiguration[\s\S]*LettuceNearCacheActuatorEndpoint\(entityManagerFactory\)/, "actuator endpoint bean");
assertContains(sources[5], /@Endpoint\(id = "nearcache"\)[\s\S]*data class RegionStats/, "actuator endpoint stats");
assertContains(sources[6], /class LettuceNearCacheMetricsAutoConfiguration[\s\S]*LettuceNearCacheMetricsBinder\(entityManagerFactory,\s*meterRegistry\)/, "metrics binder bean");
assertContains(sources[7], /class LettuceNearCacheMetricsBinder[\s\S]*SmartInitializingSingleton[\s\S]*Gauge[\s\S]*lettuce\.nearcache\.active\.regions/, "metrics gauges");
assertContains(sources[8], /class LettuceNearCacheRegionFactory:\s*RegionFactoryTemplate\(\)[\s\S]*fun getCaches\(\):\s*Map<String,\s*LettuceNearCache<Any>>/, "region factory cache registry");

const palette = {
    teal: ["#F0FDFA", "#0D9488", "#0F766E"],
    blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
    green: ["#F0FDF4", "#16A34A", "#15803D"],
    amber: ["#FFF7ED", "#EA580C", "#C2410C"],
    pink: ["#FDF2F8", "#DB2777", "#BE185D"],
    slate: ["#F8FAFC", "#64748B", "#475569"],
};

function esc(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function markerDefs() {
    return Object.entries(palette).map(([name, [, , dark]]) => `
  <marker id="arrow-${name}" markerWidth="18" markerHeight="18" refX="15" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 15 9 L 2 16" fill="none" stroke="${dark}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`).join("\n");
}

function classBox({id, x, y, w, h, color, stereotype, title, attrs = [], methods = []}) {
    const [fill, stroke, dark] = palette[color];
    const attrY = y + 76;
    const methodY = attrY + 34 + Math.max(24, attrs.length * 22);
    return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereotype" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereotype)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 58}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function chip({x, y, w, color, label}) {
    const [fill, stroke] = palette[color];
    return `<g>
  <rect class="chip" x="${x}" y="${y}" width="${w}" height="54" rx="18" fill="${fill}" stroke="${stroke}"/>
  <text class="chipText" x="${x + w / 2}" y="${y + 34}" text-anchor="middle">${esc(label)}</text>
</g>`;
}

function edge({from, to, points, color, marker = "arrow", dashed = false, label = "", labelAt}) {
    const [, , dark] = palette[color];
    const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
    const p = labelAt ?? points[Math.floor(points.length / 2)];
    return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2600;
const height = 1720;
const body = [
    chip({x: 1460, y: 78, w: 190, color: "teal", label: "properties"}),
    chip({x: 1680, y: 78, w: 190, color: "green", label: "hibernate"}),
    chip({x: 1900, y: 78, w: 190, color: "pink", label: "actuator"}),
    chip({x: 2120, y: 78, w: 190, color: "blue", label: "metrics"}),
    chip({x: 2340, y: 78, w: 180, color: "slate", label: "factory"}),
    classBox({
        id: "Properties",
        x: 90,
        y: 245,
        w: 650,
        h: 330,
        color: "teal",
        stereotype: "<<configuration properties>>",
        title: "LettuceNearCacheSpringProperties",
        attrs: ["prefix: bluetape4k.cache.lettuce-near", "redisUri / codec / useResp3", "local: LocalProperties", "redisTtl: RedisTtlProperties", "metrics: MetricsProperties"],
        methods: ["Local: maxSize, expireAfterWrite", "Redis TTL: default and per-region map", "Metrics: enabled, enableCaffeineStats"],
    }),
    classBox({
        id: "HibernateAuto",
        x: 880,
        y: 235,
        w: 760,
        h: 355,
        color: "green",
        stereotype: "<<auto-configuration>>",
        title: "LettuceNearCacheHibernateAutoConfiguration",
        attrs: ["@ConditionalOnClass(RegionFactory, EntityManagerFactory)", "@ConditionalOnProperty(enabled=true)", "@EnableConfigurationProperties(...)"],
        methods: ["bean: HibernatePropertiesCustomizer", "sets region.factory_class", "enables second-level cache", "maps Redis, codec, local, TTL, metrics properties"],
    }),
    classBox({
        id: "ActuatorAuto",
        x: 1780,
        y: 245,
        w: 680,
        h: 290,
        color: "pink",
        stereotype: "<<auto-configuration>>",
        title: "LettuceNearCacheActuatorAutoConfiguration",
        attrs: ["after Hibernate and HibernateJpa auto-config", "@ConditionalOnClass(Endpoint, RegionFactory, EMF)", "@ConditionalOnBean(EntityManagerFactory)"],
        methods: ["bean: LettuceNearCacheActuatorEndpoint", "exposes GET /actuator/nearcache"],
    }),
    classBox({
        id: "Endpoint",
        x: 1780,
        y: 675,
        w: 680,
        h: 360,
        color: "pink",
        stereotype: "<<endpoint>>",
        title: "LettuceNearCacheActuatorEndpoint",
        attrs: ["entityManagerFactory: EntityManagerFactory", "@Endpoint(id = nearcache)", "RegionStats data class"],
        methods: ["getAllRegionStats(): Map<String, RegionStats>", "getRegionStats(regionName): RegionStats?", "unwrap SessionFactoryImplementor", "read localStats and Hibernate L2 statistics"],
    }),
    classBox({
        id: "MetricsAuto",
        x: 90,
        y: 710,
        w: 650,
        h: 290,
        color: "blue",
        stereotype: "<<auto-configuration>>",
        title: "LettuceNearCacheMetricsAutoConfiguration",
        attrs: ["after Hibernate and Micrometer auto-config", "@ConditionalOnBean(EntityManagerFactory, MeterRegistry)", "@ConditionalOnProperty(metrics.enabled=true)"],
        methods: ["bean: LettuceNearCacheMetricsBinder", "enabled by metrics property group"],
    }),
    classBox({
        id: "MetricsBinder",
        x: 90,
        y: 1110,
        w: 650,
        h: 330,
        color: "blue",
        stereotype: "<<binder>>",
        title: "LettuceNearCacheMetricsBinder",
        attrs: ["entityManagerFactory: EntityManagerFactory", "meterRegistry: MeterRegistry", "implements SmartInitializingSingleton"],
        methods: ["afterSingletonsInstantiated()", "unwrap SessionFactoryImplementor", "register active.regions gauge", "register total.local.size gauge"],
    }),
    classBox({
        id: "RegionFactory",
        x: 910,
        y: 1140,
        w: 780,
        h: 330,
        color: "slate",
        stereotype: "<<Hibernate RegionFactory>>",
        title: "LettuceNearCacheRegionFactory",
        attrs: ["extends RegionFactoryTemplate", "caches: Map<String, LettuceNearCache<Any>>", "redisClient and codec are lifecycle scoped"],
        methods: ["prepareForUse() reads hibernate.cache.lettuce.*", "getCaches(): read-only cache registry", "createStorageAccess(regionName)", "releaseFromUse() closes caches before Redis client"],
    }),
    edge({
        from: "Properties",
        to: "HibernateAuto",
        points: [[740, 405], [880, 405]],
        color: "teal",
        marker: "arrow",
        dashed: true,
        label: "binds props",
        labelAt: [765, 392]
    }),
    edge({
        from: "HibernateAuto",
        to: "RegionFactory",
        points: [[1260, 590], [1260, 1140]],
        color: "green",
        marker: "arrow",
        dashed: true,
        label: "factory class + hibernate props",
        labelAt: [1278, 840]
    }),
    edge({
        from: "HibernateAuto",
        to: "ActuatorAuto",
        points: [[1640, 385], [1780, 385]],
        color: "pink",
        marker: "arrow",
        dashed: true,
        label: "after",
        labelAt: [1670, 372]
    }),
    edge({
        from: "ActuatorAuto",
        to: "Endpoint",
        points: [[2120, 535], [2120, 675]],
        color: "pink",
        marker: "arrow",
        dashed: true,
        label: "creates",
        labelAt: [2138, 610]
    }),
    edge({
        from: "Endpoint",
        to: "RegionFactory",
        points: [[1780, 880], [1690, 880], [1690, 1305]],
        color: "pink",
        marker: "arrow",
        dashed: true,
        label: "getCaches + stats",
        labelAt: [1708, 1080]
    }),
    edge({
        from: "Properties",
        to: "MetricsAuto",
        points: [[415, 575], [415, 710]],
        color: "teal",
        marker: "arrow",
        dashed: true,
        label: "metrics.enabled",
        labelAt: [433, 650]
    }),
    edge({
        from: "MetricsAuto",
        to: "MetricsBinder",
        points: [[415, 1000], [415, 1110]],
        color: "blue",
        marker: "arrow",
        dashed: true,
        label: "creates",
        labelAt: [433, 1068]
    }),
    edge({
        from: "MetricsBinder",
        to: "RegionFactory",
        points: [[740, 1285], [910, 1285]],
        color: "blue",
        marker: "arrow",
        dashed: true,
        label: "gauges from caches",
        labelAt: [752, 1248]
    }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Hibernate Lettuce UML Class Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .chip{stroke-width:1.6}.chipText{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .classBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:26px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Hibernate Lettuce Spring Boot 4 UML</text>
<text class="subtitle" x="76" y="120">Configuration properties, Hibernate property customization, Actuator endpoint, Micrometer binder, and the shared LettuceNearCacheRegionFactory.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-hibernate-lettuce-diagram-01.svg");
const pngPath = join(OUT, "spring-boot-hibernate-lettuce-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], {stdio: "inherit"});
console.log("Generated spring-boot-hibernate-lettuce-diagram-01.svg/png");
