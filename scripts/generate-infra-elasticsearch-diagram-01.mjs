#!/usr/bin/env node

import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-elasticsearch-diagram-01.svg";

const svg = String.raw`<svg xmlns="http://www.w3.org/2000/svg" width="1500" height="1040" viewBox="0 0 1500 1040" role="img" aria-label="Elasticsearch client API structure">
  <defs>
    <filter id="shadow" x="-8%" y="-10%" width="116%" height="124%">
      <feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0f172a" flood-opacity="0.10"/>
    </filter>
    <marker id="openBlue" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="#2563eb" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
    </marker>
    <marker id="openGreen" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="#16a34a" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
    </marker>
    <marker id="openPurple" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="#9333ea" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
    </marker>
    <marker id="openOrange" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="#ea580c" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
    </marker>
    <marker id="openTeal" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="#0d9488" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
    </marker>
    <style>
      svg { font-family: "Architects Daughter", "Comic Mono", "Comic Sans MS", ui-sans-serif, system-ui, sans-serif; }
      .canvas { fill: #f8fafc; }
      .frame { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1.5; filter: url(#shadow); }
      .title { font-family: "Architects Daughter"; font-size: 42px; fill: #0f172a; }
      .subtitle { font-family: "Comic Mono"; font-size: 15px; fill: #475569; }
      .class { stroke-width: 1.7; filter: url(#shadow); }
      .hdr { stroke-width: 0; }
      .name { font-family: "Architects Daughter"; font-size: 20px; fill: #0f172a; }
      .stereo { font-family: "Comic Mono"; font-size: 11.5px; fill: #64748b; }
      .member { font-family: "Comic Mono"; font-size: 12.5px; fill: #475569; }
      .small { font-family: "Comic Mono"; font-size: 11.5px; fill: #64748b; }
      .dep { fill: none; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; stroke-dasharray: 8 6; }
      .assoc { fill: none; stroke-width: 2.6; stroke-linecap: round; stroke-linejoin: round; }
      .groupLabel { font-family: "Architects Daughter"; font-size: 22px; fill: #0f172a; }
      .groupLine { stroke: #cbd5e1; stroke-width: 1.3; stroke-dasharray: 7 6; fill: none; }
    </style>
  </defs>

  <rect class="canvas" width="1500" height="1040"/>
  <rect class="frame" x="36" y="30" width="1428" height="980" rx="8"/>
  <text class="title" x="74" y="84">Elasticsearch Client API Structure</text>
  <text class="subtitle" x="78" y="116">Public builders produce official Elastic clients; coroutine extensions attach to ElasticsearchAsyncClient without wrapping it.</text>

  <path class="groupLine" d="M 76 144 H 1424"/>
  <text class="groupLabel" x="86" y="174">Construction API</text>
  <path class="groupLine" d="M 76 510 H 1424"/>
  <text class="groupLabel" x="86" y="540">Coroutine extension API</text>

  <g id="config">
    <rect class="class" x="88" y="214" width="252" height="152" rx="8" fill="#fff7ed" stroke="#ea580c"/>
    <rect class="hdr" x="88" y="214" width="252" height="48" rx="8" fill="#fed7aa" opacity="0.75"/>
    <text class="stereo" x="214" y="237" text-anchor="middle">&lt;&lt;config class&gt;&gt;</text>
    <text class="name" x="214" y="260" text-anchor="middle">ElasticsearchClientConfig</text>
    <line x1="88" y1="272" x2="340" y2="272" stroke="#fdba74"/>
    <text class="member" x="110" y="300">+ host / port / scheme</text>
    <text class="member" x="110" y="325">+ username / password</text>
    <text class="member" x="110" y="350">+ sslContext / mapper</text>
  </g>

  <g id="dsl">
    <rect class="class" x="404" y="210" width="292" height="160" rx="8" fill="#eff6ff" stroke="#2563eb"/>
    <rect class="hdr" x="404" y="210" width="292" height="48" rx="8" fill="#dbeafe" opacity="0.8"/>
    <text class="stereo" x="550" y="233" text-anchor="middle">&lt;&lt;top-level functions&gt;&gt;</text>
    <text class="name" x="550" y="256" text-anchor="middle">Client DSL</text>
    <line x1="404" y1="270" x2="696" y2="270" stroke="#bfdbfe"/>
    <text class="member" x="426" y="298">+ elasticsearchAsyncClient {}</text>
    <text class="member" x="426" y="323">+ elasticsearchClient {}</text>
    <text class="small" x="426" y="348">creates config, delegates to factory</text>
  </g>

  <g id="factory">
    <rect class="class" x="772" y="200" width="286" height="178" rx="8" fill="#f0fdf4" stroke="#16a34a"/>
    <rect class="hdr" x="772" y="200" width="286" height="48" rx="8" fill="#dcfce7" opacity="0.85"/>
    <text class="stereo" x="915" y="223" text-anchor="middle">&lt;&lt;object&gt;&gt;</text>
    <text class="name" x="915" y="246" text-anchor="middle">ElasticsearchClients</text>
    <line x1="772" y1="260" x2="1058" y2="260" stroke="#bbf7d0"/>
    <text class="member" x="794" y="288">+ asyncClientOf(...)</text>
    <text class="member" x="794" y="313">+ clientOf(...)</text>
    <text class="member" x="794" y="338">+ transportOf(...)</text>
    <text class="small" x="794" y="363">validates host, port, scheme</text>
  </g>

  <g id="official">
    <rect class="class" x="1156" y="184" width="244" height="214" rx="8" fill="#f0fdfa" stroke="#0d9488"/>
    <rect class="hdr" x="1156" y="184" width="244" height="48" rx="8" fill="#ccfbf1" opacity="0.9"/>
    <text class="stereo" x="1278" y="207" text-anchor="middle">&lt;&lt;Elastic Java client&gt;&gt;</text>
    <text class="name" x="1278" y="230" text-anchor="middle">Official clients</text>
    <line x1="1156" y1="244" x2="1400" y2="244" stroke="#99f6e4"/>
    <text class="member" x="1178" y="273">+ ElasticsearchAsyncClient</text>
    <text class="member" x="1178" y="298">+ ElasticsearchClient</text>
    <line x1="1156" y1="318" x2="1400" y2="318" stroke="#99f6e4"/>
    <text class="small" x="1178" y="347">created from ElasticsearchTransport</text>
    <text class="small" x="1178" y="372">async client exposes Future APIs</text>
  </g>

  <g id="defaults">
    <rect class="class" x="88" y="400" width="252" height="74" rx="8" fill="#fff7ed" stroke="#ea580c"/>
    <text class="name" x="214" y="430" text-anchor="middle">ElasticsearchDefaults</text>
    <text class="small" x="214" y="454" text-anchor="middle">host, port, batch, flush interval</text>
  </g>

  <g id="mapper">
    <rect class="class" x="404" y="410" width="292" height="74" rx="8" fill="#eff6ff" stroke="#2563eb"/>
    <text class="name" x="550" y="440" text-anchor="middle">JsonpMappers</text>
    <text class="small" x="550" y="464" text-anchor="middle">jackson3JsonpMapper / jacksonJsonpMapper</text>
  </g>

  <g id="transport">
    <rect class="class" x="800" y="426" width="230" height="74" rx="8" fill="#f7fee7" stroke="#65a30d"/>
    <text class="name" x="915" y="456" text-anchor="middle">Rest5ClientTransport</text>
    <text class="small" x="915" y="480" text-anchor="middle">HC5 transport for ES 9.x</text>
  </g>

  <path class="dep" d="M 340 290 H 404" stroke="#ea580c" marker-end="url(#openOrange)"/>
  <path class="assoc" d="M 696 290 H 772" stroke="#2563eb" marker-end="url(#openBlue)"/>
  <path class="dep" d="M 214 400 V 366" stroke="#ea580c" marker-end="url(#openOrange)"/>
  <path class="dep" d="M 340 437 H 374 V 330 H 404" stroke="#ea580c" marker-end="url(#openOrange)"/>
  <path class="dep" d="M 696 447 H 748 V 348 H 772" stroke="#2563eb" marker-end="url(#openBlue)"/>
  <path class="assoc" d="M 915 378 V 426" stroke="#65a30d" marker-end="url(#openGreen)"/>
  <path class="assoc" d="M 1058 289 H 1156" stroke="#16a34a" marker-end="url(#openGreen)"/>

  <g id="receiver">
    <rect class="class" x="548" y="570" width="404" height="66" rx="8" fill="#f0fdfa" stroke="#0d9488"/>
    <text class="stereo" x="750" y="594" text-anchor="middle">&lt;&lt;extension receiver&gt;&gt;</text>
    <text class="name" x="750" y="619" text-anchor="middle">ElasticsearchAsyncClient</text>
  </g>

  <g id="documents">
    <rect class="class" x="88" y="706" width="250" height="124" rx="8" fill="#faf5ff" stroke="#9333ea"/>
    <rect class="hdr" x="88" y="706" width="250" height="42" rx="8" fill="#ede9fe" opacity="0.9"/>
    <text class="name" x="213" y="734" text-anchor="middle">Document and index APIs</text>
    <line x1="88" y1="758" x2="338" y2="758" stroke="#ddd6fe"/>
    <text class="member" x="110" y="786">+ index/get/update/delete</text>
    <text class="member" x="110" y="811">+ exists/count/index lifecycle</text>
  </g>

  <g id="search">
    <rect class="class" x="410" y="704" width="250" height="128" rx="8" fill="#f0fdfa" stroke="#0d9488"/>
    <rect class="hdr" x="410" y="704" width="250" height="42" rx="8" fill="#ccfbf1" opacity="0.9"/>
    <text class="name" x="535" y="732" text-anchor="middle">PIT Search Flow</text>
    <line x1="410" y1="758" x2="660" y2="758" stroke="#99f6e4"/>
    <text class="member" x="432" y="786">+ openPointInTimeSuspending</text>
    <text class="member" x="432" y="811">+ searchAsFlow(search_after)</text>
  </g>

  <g id="bulk">
    <rect class="class" x="732" y="706" width="250" height="124" rx="8" fill="#fff7ed" stroke="#ea580c"/>
    <rect class="hdr" x="732" y="706" width="250" height="42" rx="8" fill="#fed7aa" opacity="0.82"/>
    <text class="name" x="857" y="734" text-anchor="middle">Bulk APIs</text>
    <line x1="732" y1="758" x2="982" y2="758" stroke="#fdba74"/>
    <text class="member" x="754" y="786">+ bulkAsFlow(chunked)</text>
    <text class="member" x="754" y="811">+ suspendBulk</text>
  </g>

  <g id="ingester">
    <rect class="class" x="1080" y="688" width="284" height="154" rx="8" fill="#f0fdf4" stroke="#16a34a"/>
    <rect class="hdr" x="1080" y="688" width="284" height="44" rx="8" fill="#dcfce7" opacity="0.9"/>
    <text class="name" x="1222" y="717" text-anchor="middle">BulkIngester support</text>
    <line x1="1080" y1="744" x2="1364" y2="744" stroke="#bbf7d0"/>
    <text class="member" x="1102" y="772">+ bulkIngesterOf(...)</text>
    <text class="member" x="1102" y="797">+ addSuspend(...)</text>
    <text class="member" x="1102" y="822">+ bulkProgressListener()</text>
  </g>

  <g id="events">
    <rect class="class" x="996" y="896" width="438" height="66" rx="8" fill="#f0fdf4" stroke="#16a34a"/>
    <text class="stereo" x="1215" y="920" text-anchor="middle">&lt;&lt;sealed interface&gt;&gt;</text>
    <text class="name" x="1215" y="945" text-anchor="middle">BulkProgressEvent: Before / After / Error</text>
  </g>

  <path class="dep" d="M 1278 398 V 442 H 1040 V 603 H 952" stroke="#0d9488" marker-end="url(#openTeal)"/>
  <path class="dep" d="M 750 636 V 670 H 213 V 706" stroke="#9333ea" marker-end="url(#openPurple)"/>
  <path class="dep" d="M 750 636 V 704 H 535" stroke="#0d9488" marker-end="url(#openTeal)"/>
  <path class="dep" d="M 750 636 V 670 H 857 V 706" stroke="#ea580c" marker-end="url(#openOrange)"/>
  <path class="dep" d="M 952 603 H 1028 V 765 H 1080" stroke="#16a34a" marker-end="url(#openGreen)"/>
  <path class="dep" d="M 1222 842 V 896" stroke="#16a34a" marker-end="url(#openGreen)"/>
</svg>`;

writeFileSync(out, `${svg}\n`);
console.log(`wrote ${out}`);
