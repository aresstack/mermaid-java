# mermaid-java

Renders [Mermaid](https://mermaid.js.org/) diagram code to SVG or `BufferedImage` in **pure Java** — no browser, no Node.js, no native dependencies.  
Internally uses GraalJS to execute the official Mermaid library and Apache Batik for accurate SVG text measurement and rasterisation.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.aresstack</groupId>
    <artifactId>mermaid-java</artifactId>
    <version>0.1.0-beta.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.aresstack:mermaid-java:0.1.0-beta.1'
```

## Quick Start

No external files or Node.js required — the Mermaid JS engine is bundled inside the JAR.

```java
import com.aresstack.Mermaid;

// One-liner — get a ready-to-use SVG string:
String svg = Mermaid.render("graph TD; A-->B;");

// Or get a BufferedImage directly:
BufferedImage img = Mermaid.renderToImage("graph TD; A-->B;");
```

### Save as PNG

```java
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

BufferedImage img = Mermaid.renderToImage("graph TD; A-->B;");
ImageIO.write(img, "png", new File("diagram.png"));
```

### Control the output width (zoom / hi-DPI / thumbnails)

```java
// High-resolution render (4K width):
BufferedImage hi = Mermaid.renderToImage("graph TD; A-->B;", 3840);

// Thumbnail:
BufferedImage thumb = Mermaid.renderToImage("graph TD; A-->B;", 300);
```

### Re-rasterise an existing SVG at a different size

If you already have an SVG string and need multiple resolutions:

```java
String svg = Mermaid.render("graph TD; A-->B;");

BufferedImage normal = Mermaid.svgToImage(svg);       // intrinsic size
BufferedImage large  = Mermaid.svgToImage(svg, 2400);  // exact width
BufferedImage small  = Mermaid.svgToImage(svg, 400);   // thumbnail
```

### Embed in Swing

```java
BufferedImage img = Mermaid.renderToImage("graph TD; A-->B;");
JLabel label = new JLabel(new ImageIcon(img));
```

### Embed in JavaFX

```java
BufferedImage img = Mermaid.renderToImage("graph TD; A-->B;");
ImageView view = new ImageView(SwingFXUtils.toFXImage(img, null));
```

### Error handling

```java
JsExecutionResult result = Mermaid.renderDetailed("graph TD; A-->B;");
if (result.isSuccessful()) {
    String svg = result.getOutput();
} else {
    System.err.println(result.getErrorMessage());
}
```

## API Overview

| Method | Returns | Description |
|--------|---------|-------------|
| `Mermaid.render(code)` | `String` | SVG with all Batik-compatibility fixes |
| `Mermaid.renderRaw(code)` | `String` | SVG without post-processing |
| `Mermaid.renderToImage(code)` | `BufferedImage` | Rasterised image at intrinsic size, auto-cropped |
| `Mermaid.renderToImage(code, width)` | `BufferedImage` | Rasterised at exact pixel width |
| `Mermaid.svgToImage(svg)` | `BufferedImage` | Convert existing SVG string → image |
| `Mermaid.svgToImage(svg, width)` | `BufferedImage` | Convert existing SVG → image at exact width |
| `Mermaid.autoCrop(image)` | `BufferedImage` | Trim transparent/white edges |
| `Mermaid.renderDetailed(code)` | `JsExecutionResult` | SVG or error details |

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
| `Mermaid` | **Public API** — `render()` / `renderToImage()` / `svgToImage()` |
| `MermaidRenderer` | Singleton — GraalJS context management, Mermaid initialisation |
| `MermaidSvgFixup` | Post-processing: Batik-compatibility DOM fixes |
| `GraalJsExecutor` | Internal GraalJS polyglot-context wrapper |
| `BatikBBoxService` | Exact SVG BBox computation via Batik GVT tree |
| `JsExecutionResult` | Immutable result object (success / failure) |

## Building

```bash
# Gradle (development)
./gradlew build

# Maven (for release)
mvn clean package
```

## License

This project is licensed under the [MIT License](LICENSE).

### Bundled third-party software

This JAR includes a bundled copy of [Mermaid.js](https://github.com/mermaid-js/mermaid)
(v11.4.1), which is licensed separately under the **MIT License**,
Copyright © Knut Sveidqvist.
See the [Mermaid license](https://github.com/mermaid-js/mermaid/blob/develop/LICENSE)
for details.
