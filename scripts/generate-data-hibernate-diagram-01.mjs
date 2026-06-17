#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/data-hibernate-diagram-01";
const W = 2280, H = 1180;
const c = { ink:"#0F172A", muted:"#475569", canvas:"#F8FAFC", frame:"#FFFFFF", line:"#CBD5E1", blue:"#2563EB", teal:"#0D9488", green:"#16A34A", orange:"#EA580C", purple:"#7C3AED", red:"#DC2626", gray:"#64748B" };
const sources = [
  "data/hibernate/README.md","data/hibernate/README.ko.md",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/criteria/CriteriaSupport.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSesisonSupport.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/querydsl/core/ExpressionsSupport.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/CompressedStringConverter.kt",
  "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/listeners/HibernateEntityListener.kt",
];
for (const s of sources) if (!existsSync(s)) throw new Error(`Missing source evidence: ${s}`);
if (!/Persistence Extension Structure[\s\S]*data-hibernate-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) throw new Error("README diagram slot not found");

const cards = {
  caller:{x:85,y:205,w:360,h:170,fill:"#EFF6FF",stroke:c.blue,title:"Application Code",icon:"K",body:["works with standard JPA/Hibernate","adds Kotlin reified calls"]},
  em:{x:560,y:205,w:430,h:185,fill:"#EFF6FF",stroke:c.blue,title:"EntityManagerSupport",icon:"E",body:["save / delete / deleteById","findAs / exists / countAll","unwraps Session + connection"]},
  session:{x:1130,y:205,w:420,h:185,fill:"#F0FDFA",stroke:c.teal,title:"SessionSupport",icon:"S",body:["withBatchSize restores size","findByNaturalId helpers","typed HQL/native query"]},
  driver:{x:1700,y:205,w:420,h:185,fill:"#F8FAFC",stroke:c.gray,title:"Hibernate/JPA Runtime",icon:"H",body:["EntityManager / Session","SessionFactory / JDBC","Jakarta Persistence 3.2"]},
  criteria:{x:260,y:535,w:430,h:180,fill:"#F5F3FF",stroke:c.purple,title:"Criteria + TypedQuery",icon:"C",body:["createQueryAs / attribute","int/long result helpers","findOneOrNull + paging"]},
  stateless:{x:860,y:535,w:430,h:180,fill:"#FFF7ED",stroke:c.orange,title:"StatelessSession",icon:"B",body:["withStateless transaction scope","insert/update/delete batches","reified get/query helpers"]},
  querydsl:{x:1460,y:535,w:430,h:180,fill:"#F5F3FF",stroke:c.purple,title:"Querydsl Extensions",icon:"Q",body:["expression composition","projection helpers","JPA expression conversions"]},
  model:{x:260,y:875,w:430,h:185,fill:"#ECFDF5",stroke:c.green,title:"Entity Base Model",icon:"M",body:["JpaEntity + AbstractJpaEntity","Int/Long/UUID id bases","tree entity variants"]},
  converters:{x:860,y:875,w:430,h:185,fill:"#FEF2F2",stroke:c.red,title:"AttributeConverters",icon:"A",body:["locale/time/json helpers","compression + serialization","optional AES/Tink converters"]},
  listeners:{x:1460,y:875,w:430,h:185,fill:"#FFF7ED",stroke:c.orange,title:"Listeners + Spring Shim",icon:"L",body:["post-commit event listener","JPA lifecycle logger","TestEntityManager helper"]},
};
const flows = [
  [c.blue,"M445 290 L560 290","uses",503,265],[c.teal,"M990 290 L1130 290","unwraps",1060,265],[c.gray,"M1550 290 L1700 290","delegates",1625,265],
  [c.purple,"M775 390 L775 465 L475 465 L475 535","query API",625,440],[c.orange,"M1340 390 L1340 465 L1075 465 L1075 535","batch API",1210,440],
  [c.green,"M775 390 L775 815 L475 815 L475 875","entity contract",625,790],[c.red,"M1075 715 L1075 875","column mapping",1170,800],[c.orange,"M1675 715 L1675 875","events/tests",1770,800],
  [c.gray,"M1910 390 L1910 450 L1795 450 L1795 535","query factory",1852,425],
];
function esc(v){return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;");}
function marker(color){const id=color.replace("#","a");return `<marker id="${id}" markerUnits="userSpaceOnUse" markerWidth="18" markerHeight="14" refX="17" refY="7" orient="auto"><path d="M1 1 L17 7 L1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;}
function label(t,x,y){const w=Math.max(78,t.length*8.2+18);return `<g transform="translate(${x-w/2} ${y-16})"><rect width="${w}" height="28" rx="8" fill="#fff" stroke="${c.line}" opacity=".96"/><text class="label" x="${w/2}" y="19" text-anchor="middle">${esc(t)}</text></g>`;}
function card(k){const b=cards[k];return `<g id="${k}"><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><rect class="icon" x="${b.x+24}" y="${b.y+28}" width="48" height="48" rx="8" fill="#fff" stroke="${b.stroke}"/><text class="iconText" x="${b.x+48}" y="${b.y+60}" text-anchor="middle" fill="${b.stroke}">${esc(b.icon)}</text><text class="cardTitle" x="${b.x+92}" y="${b.y+45}">${esc(b.title)}</text>${b.body.map((l,i)=>`<text class="body" x="${b.x+92}" y="${b.y+78+i*24}">${esc(l)}</text>`).join("")}</g>`;}
const defs=[...new Set(flows.map(f=>f[0]))].map(marker).join("");
const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Hibernate persistence extension structure"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter>${defs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle,.sectionTitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.6;stroke-dasharray:12 8}.card{stroke-width:2;filter:url(#softShadow)}.icon{stroke-width:1.5}.iconText{font-family:"Architects Daughter";font-size:25px}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.body{font-family:"Comic Mono";font-size:13px;fill:${c.muted}}.flow{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.label{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W-68}" height="${H-66}" rx="8"/><text class="title" x="76" y="86">Hibernate Persistence Extension Structure</text><text class="subtitle" x="78" y="118">Kotlin extension packages sit beside Hibernate/JPA primitives instead of introducing a repository abstraction.</text><rect class="section" x="62" y="140" width="2156" height="980" rx="8"/><text class="sectionTitle" x="90" y="165">module extension packages: EntityManager, Session, Criteria, StatelessSession, Querydsl, model, converters, listeners</text><g>${flows.map(f=>`<path class="flow" d="${f[1]}" stroke="${f[0]}" marker-end="url(#${f[0].replace("#","a")})"/>`).join("\n")}</g><g>${flows.map(f=>label(f[2],f[3],f[4])).join("\n")}</g>${Object.keys(cards).map(card).join("\n")}</svg>`;
writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm,""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], { stdio:"inherit" });
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
