# Changelog

All notable changes to the **mermaid-java** library are documented in this file.

## [0.2.0-beta.2] — 2025-03-25

### Fixed
- **Intl polyfill for GraalJS**: Added comprehensive `Intl` polyfill to `browser-shim.js` resolving `ReferenceError: Intl is not defined` when Mermaid 11+ calls `Intl.Segmenter` in `SplitTextToChars()`.
  - `Intl.Segmenter` — splits text by code points with proper surrogate-pair handling, returns an iterable result with `Symbol.iterator`.
  - `Intl.NumberFormat`, `Intl.DateTimeFormat`, `Intl.Collator` — minimal stubs for chart/axis label rendering.
  - `window.Intl = Intl` — ensures global availability in the GraalJS polyglot context.

## [0.2.0-beta.1] — 2025-03-24

### Added
- **Layout extraction API** (`DiagramLayoutExtractor`): Parses rendered SVG to extract node/edge positions, shapes, and bounding boxes into a structured `RenderedDiagram` model.
- **Polymorphic node types**: `FlowchartNode`, `ClassNode`, `ErEntityNode`, `SequenceActorNode`, `StateNode`, `MindmapNode` — each with type-specific properties (shape, stereotypes, attributes, cardinality, etc.).
- **ANTLR 4.9.3 grammars** for 5 Mermaid diagram types:
  - `MermaidFlowchart.g4` — flowchart / graph
  - `MermaidClassDiagram.g4` — class diagrams (members, stereotypes, relationships)
  - `MermaidErDiagram.g4` — ER diagrams (entities, attributes, relationships with cardinality)
  - `MermaidSequenceDiagram.g4` — sequence diagrams (participants, messages, loops, alt)
  - `MermaidStateDiagram.g4` — state diagrams (states, transitions, notes)
- **`SourceEditBridge`** — AST-based roundtrip editing: rename nodes, change edge labels, add/delete/reverse edges, reconnect edge endpoints, add/remove class members. Changes are applied to the Mermaid source text with precise line targeting.
- **`MermaidSvgFixup`** — post-processing for Batik compatibility: strips unsupported CSS properties (`dominant-baseline`, `alignment-baseline`), fixes `<foreignObject>` text alignment, and corrects ER-diagram label visibility.

### Fixed
- **ANTLR compilation in Maven**: Grammar files are now correctly compiled from `src/main/antlr4/` during the `generate-sources` phase.
- **Grammar package path**: Generated parsers are placed in `com.aresstack.mermaid.parser.*` to match the project's package structure.
- **ER diagram edge bounding boxes**: Edge `containsApprox()` hit-testing now works correctly for ER relationship lines.

## [0.1.0-beta.1] — 2025-03-22

### Added
- Initial release: Pure-Java Mermaid-to-SVG rendering using GraalJS + Apache Batik.
- `MermaidRenderer` — singleton engine that evaluates `mermaid.min.js` in a GraalJS polyglot context and renders diagrams to SVG strings.
- `browser-shim.js` — comprehensive browser environment polyfill for GraalJS (DOM stubs, `DOMParser`, `XMLSerializer`, `requestAnimationFrame`, `MutationObserver`, `ResizeObserver`, `CSS.supports`, `matchMedia`, etc.).
- `SvgRenderer` — Batik-based rasterization of SVG to `BufferedImage` with configurable size hints.
- Support for all major Mermaid diagram types: flowchart, sequence, class, ER, state, mindmap, Gantt, pie, and more.

