# mermaid-java

Renders [Mermaid](https://mermaid.js.org/) diagram code to SVG in **pure Java** — no browser, no Node.js, no native dependencies.  
Internally uses GraalJS to execute the official Mermaid library and Apache Batik for accurate SVG text measurement.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.aresstack</groupId>
    <artifactId>mermaid-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.aresstack:mermaid-java:0.1.0'
```

## Quick Start

```java
import com.aresstack.mermaid.Mermaid;

// One-liner — renders to SVG with all post-processing applied:
String svg = Mermaid.render("graph TD; A-->B;");
```

That's it. The returned `String` is a self-contained SVG document ready to be
written to a file, embedded in HTML, or rasterised with Batik / ImageIO.

### Raw SVG (no Batik fixes)

If you don't need the Batik-compatibility post-processing:

```java
String rawSvg = Mermaid.renderRaw("graph TD; A-->B;");
```

### Error handling

```java
import com.aresstack.mermaid.JsExecutionResult;

JsExecutionResult result = Mermaid.renderDetailed("graph TD; A-->B;");
if (result.isSuccessful()) {
    String svg = result.getOutput();
} else {
    System.err.println(result.getErrorMessage());
}
```

### Lower-level API

The `Mermaid` facade delegates to two classes that you can also use directly:

```java
import com.aresstack.mermaid.MermaidRenderer;
import com.aresstack.mermaid.MermaidSvgFixup;

MermaidRenderer renderer = MermaidRenderer.getInstance();
String svg = renderer.renderToSvg("graph TD; A-->B;");
svg = MermaidSvgFixup.fixForBatik(svg);
```

## Supported Diagram Types

| Type | Status | Notes |
|------|--------|-------|
| Flowchart | ✅ | incl. subgraphs |
| Sequence | ✅ | lifeline fixes applied |
| Class | ✅ | |
| State | ✅ | |
| ER (Entity Relationship) | ✅ | label reposition fixes |
| Pie | ✅ | |
| Gantt | ✅ | |
| Journey (User Journey) | ✅ | |
| Mindmap | ✅ | multi-line box expansion |
| Git Graph | ✅ | |
| Sankey | ✅ | |
| Block | ✅ | |
| Architecture | ✅ | |
| Packet | ✅ | |
| Requirement | ✅ | label distribution fix |
| C4 (System Context) | ✅ | `href` → `xlink:href` fix |
| ZenUML | ❌ | requires extra DOM APIs |
| Radar (`radar-beta`) | ❌ | experimental |
| Treemap (`treemap-beta`) | ❌ | experimental |

## Mermaid Version

Currently bundled: **Mermaid 11.4.1** (ESM → IIFE via esbuild).

The bundle is built from `js-bundle/`:

```bash
cd js-bundle && npm install && npm run bundle
```

Output: `src/main/resources/mermaid/mermaid.min.js`

## Architecture

| Class | Purpose |
|-------|---------|
| `Mermaid` | **Public API** — static one-liner `render()` / `renderRaw()` / `renderDetailed()` |
| `MermaidRenderer` | Singleton — GraalJS context management, Mermaid initialisation |
| `MermaidSvgFixup` | Post-processing: Batik-compatibility DOM fixes |
| `GraalJsExecutor` | Internal GraalJS polyglot-context wrapper |
| `GraalJsExecutor.JavaBridge` | Java↔JS bridge: text measurement (Java2D) + BBox (Batik) |
| `BatikBBoxService` | Exact SVG BBox computation via Batik GVT tree |
| `JsExecutionResult` | Immutable result object (success / failure) |

### BatikBBoxService — Accurate `getBBox()`

Mermaid relies on `getBBox()` for layout calculations. The headless browser shim
delegates to `BatikBBoxService` for `<text>` elements, which parses the SVG fragment
through Batik's GVT tree and returns pixel-accurate bounding boxes using Java2D fonts.

### MermaidSvgFixup — Batik Compatibility

Mermaid emits browser-centric SVG. `MermaidSvgFixup.fixForBatik()` applies 16+ DOM-level
fixes (marker relocation, z-order, lifeline extension, hsl→hex conversion, …) so the output
renders correctly in Apache Batik and other strict SVG rasterisers.

## Building

```bash
# Gradle (development)
./gradlew build

# Maven (for release)
mvn clean package
```

## License

[MIT](LICENSE)
