# Mermaid Renderer

Renders Mermaid diagram code to SVG in pure Java via GraalJS.

## Mermaid-Version

Aktuell gebündelt: **Mermaid 11.4.1** (ESM → IIFE via esbuild).

Das Bundle wird erzeugt über `js-bundle/`:

    cd mermaid-renderer/js-bundle && npm install && npm run bundle

Output: `src/main/resources/mermaid/mermaid.min.js`

Ältere Versionen (9.x) waren UMD/IIFE und konnten direkt heruntergeladen werden.
Ab 10.x ist Mermaid ESM-only und erfordert den esbuild-Bundler.

## Ausführen

Vom **Projekt-Root**:

    gradlew :mermaid-renderer:run

Tests:

    gradlew :mermaid-renderer:test

Visueller Test (im `app`-Modul):

    gradlew :app:run -PmainClass=mermaid.com.aresstack.MermaidRenderTest

## Architektur

| Klasse | Zweck |
|--------|-------|
| `MermaidRenderer` | Singleton-Fassade — `renderToSvg(diagramCode)` |
| `MermaidSvgFixup` | Post-Processing: Batik-Kompatibilitäts-Fixes für SVG |
| `GraalJsExecutor` | Interner GraalJS Polyglot-Context Wrapper |
| `GraalJsExecutor.JavaBridge` | Java↔JS Brücke: Textmessung (Java2D) + BBox (Batik) |
| `BatikBBoxService` | **NEU** — Exakte SVG-BBox-Berechnung via Batik GVT-Tree |
| `JsExecutionResult` | Immutables Ergebnisobjekt (success/failure) |
| `MermaidRendererMain` | Standalone-Main zum manuellen Testen |

### BatikBBoxService — Akkurate getBBox()-Berechnung

Der Browser-Shim muss `getBBox()` für SVG-Elemente implementieren, damit
Mermaid korrekte Layouts berechnen kann. Besonders bei `<text>` mit `<tspan>`-
Kindern (em-Einheiten, absolute/relative Positionierung, font-family-Auflösung)
versagen rein heuristische JS-Berechnungen.

**Lösung:** Der `BatikBBoxService` nutzt Apache Batik's GVT (Graphics Vector Toolkit)
für pixelgenaue BBox-Berechnung:

1. Das SVG-Element wird per `outerHTML` serialisiert
2. `javaBridge.computeSvgBBox(svgXml)` schickt den Fragment-String nach Java
3. Batik parsed das Fragment in ein SVG-DOM → baut GVT-Tree → nutzt Java2D Fonts
4. `GraphicsNode.getGeometryBounds()` liefert die exakte Bounding Box zurück

| Komponente | Rolle |
|------------|-------|
| `BatikBBoxService` | Batik GVT-Builder + LRU-Cache (512 Einträge) |
| `JavaBridge.computeSvgBBox()` | JS→Java Brücke via GraalJS HostAccess |
| `_computeBBoxViaBatik()` (JS) | Serialisierung + Aufruf, Fallback auf JS-Heuristik |

**Wann wird Batik genutzt?**
- `<text>` Elemente (mit/ohne `<tspan>`) — die fehleranfälligste Kategorie
- Fragments ≤ 5000 Zeichen (größere Fragmente nutzen weiterhin JS-Heuristik)

**Wann wird JS-Heuristik genutzt?**
- Einfache Shapes (rect, circle, ellipse, line, polygon, path) — trivial korrekt in JS
- SVG-Root und Container (svg, g, div) — aggregieren Kind-BBoxen
- Fallback wenn Batik fehlschlägt (z.B. unvollständiges SVG während der Rendering-Phase)

### MermaidSvgFixup — Batik-Kompatibilität

Mermaid erzeugt SVG, das in Browsern funktioniert, aber in Apache Batik
(unserem Rasteriser) diverse Probleme hat. `MermaidSvgFixup.fixForBatik()`
wendet folgende DOM-Level-Fixes an:

| Fix | Problem | Lösung |
|-----|---------|--------|
| `moveMarkersToDefs` | Batik findet Marker nur in `<defs>` | Alle `<marker>` nach `<defs>` verschieben |
| `fixMarkerFills` | Marker-Pfeile unsichtbar (fill:none Vererbung) | Explizites `fill="#333333"` + `style` setzen |
| `fixMarkerViewBox` | viewBox 12×20 auf 12×12 Marker → Verzerrung | viewBox entfernen, marker-eigene Koordinaten nutzen |
| `fixGroupZOrder` | **Nodes malen ÜBER Pfeile → Spitzen verdeckt** | Reihenfolge: nodes → edgePaths → edgeLabels |
| `fixNodeZOrder` | Text hinter Shape | Shapes vor Labels sortieren |
| `fixLabelCentering` | Text nicht zentriert (dominant-baseline) | `dy="0.35em"` + `text-anchor="middle"` |
| `fixEdgeStrokes` | Linien unsichtbar (CSS-abhängig) | Explizite `stroke`/`stroke-width` Attribute |
| `fixEdgeLabelBackground` | Label-Hintergrund fehlt | fill/opacity auf vorhandene rects |
| `fixEdgeLabelRect` | Kein Rect hinter Label-Text | Background-Rect einfügen (opacity=1) |
| `fixCssFillNone` | CSS `fill:none` überschreibt Marker-Fill | CSS-Override für `.arrowMarkerPath` |
| `fixCssForBatik` | **`hsl()`, `rgba()`, `filter`, `position` → Batik crasht** | hsl→hex, rgba→hex, unsupported props strippen |
| `fixSequenceLifelines` | **Lebenslinien zu kurz → enden vor unteren Actor-Boxen** | y2 auf Top der unteren Boxen verlängern, y1 auf Bottom der oberen Boxen |
| `fixRequirementLabels` | **Requirement-Box-Labels überlagern sich (alle y=0)** | Labels vertikal in Title-/Attribut-Bereich verteilen |
| `fixImageHref` | **`<image href="…">` → Batik kennt nur `xlink:href`** | SVG 2 `href` nach `xlink:href` kopieren |
| `fixViewBoxFromAttributes` | ViewBox zu klein (fehlende Element-Koordinaten) | Alle x/y/width/height/x1/y1/x2/y2 scannen |
| `setDimensions` | **SVG hat keine/falsche Pixel-Dimensionen → verpixeltes Bild** | `width`+`height` setzen: max(vbW,vbH) → 2000px, andere Achse proportional |

### Ressourcen

| Datei | Zweck |
|-------|-------|
| `browser-shim.js` | Minimales Browser-Umfeld für GraalJS (DOM, CSS, Selektoren) |
| `mermaid.min.js` | Mermaid 11.4.1 IIFE-Bundle (generiert via esbuild, nicht eingecheckt) |

### Integration in app

    implementation project(':mermaid-renderer')

    MermaidRenderer renderer = MermaidRenderer.getInstance();
    String svg = renderer.renderToSvg("graph TD; A-->B;");
    svg = MermaidSvgFixup.fixForBatik(svg); // Batik-kompatibel machen

### Visueller Test (MermaidRenderTest)

Das `app`-Modul enthält drei visuelle Testsuiten:

| Klasse | Inhalt |
|--------|--------|
| `MermaidRenderTest` | 8 Micro-Tests (Flowchart, Sequenz, Mindmap, …) — Grundfunktionalität |
| `MermaidRenderTest2` | 15 Tests für erweiterte Diagrammtypen (Class, State, ER, Gantt, Pie, …) |
| `MermaidRenderTest3` | 6 Tests für noch fehlende/experimentelle Diagrammtypen |

Alle Tests bieten Zoom+Pan pro Diagramm (Mausrad, Drag, +/−/Einpassen),
Erwartungsbeschreibung, Anmerkungsfeld und spezifische Ja/Nein/Teilweise-Fragen.
Ergebnis als JSON → `mermaid-test-result.json` / `mermaid-test2-result.json`.

### Browser-Shim — `_computeElementDims`

Mermaid verwendet `getBBox()` zur Layout-Berechnung (Knotengrößen, Pfadendpunkte).
Der Browser-Shim implementiert `_computeElementDims()` für genaue Dimensionen:

| SVG-Element | Dimensionsquelle |
|-------------|-----------------|
| `rect` | `width`/`height` Attribute |
| `circle` | `r` × 2 |
| `ellipse` | `rx` × 2, `ry` × 2 |
| `polygon` | `points` parsen → min/max Bounding Box |
| `line` | `x1`/`y1`/`x2`/`y2` |
| `text` | `_estimateTextWidth()` (char × 8px + 16px) |
| `g`/Container | Kinder-BBoxes aggregieren (min/max, mit Transform-Offset) |

## Bekannte Einschränkung — Batik BBox für `<text>`-Elemente

Batik BBox-Berechnung ist für `<text>`-Elemente **deaktiviert**, weil die
Serialisierung → Batik-Parse → GVT-Rückgabe dabei Styling verliert (insbesondere
`font-family`, `font-weight`, `text-anchor`).  Stattdessen wird die JS-Heuristik
verwendet.  Außerdem wird die ViewBox-Erweiterung für Text-Overflow angewendet.

> **TODO (zukünftige Versionen):** Disable Batik BBox for `<text>` elements
> due to style loss; enhance viewBox expansion for text overflow.  Dies muss
> irgendwann behoben werden, damit `<text>`-BBoxen wieder pixelgenau sind.

## Fehlende / experimentelle Diagrammtypen (Test3)

| # | Diagrammtyp | Status | Anmerkung |
|---|-------------|--------|-----------|
| 1 | **Requirement Diagram** | ✅ gefixt | Labels werden per `fixRequirementLabels()` vertikal verteilt |
| 2 | **C4-Diagramm** (System Context) | ✅ gefixt | `<image href>` → `xlink:href` per `fixImageHref()` für Batik |
| 3 | **ZenUML** | ❌ nicht unterstützt | Wird von der gebündelten Mermaid-Lib nicht unterstützt (benötigt zusätzliche DOM-APIs) |
| 4 | **Radar-Chart** (`radar-beta`) | ❌ nicht unterstützt | Wird von der gebündelten Mermaid-Lib nicht unterstützt (experimenteller Typ) |
| 5 | **Treemap** (`treemap-beta`) | ❌ nicht unterstützt | Wird von der gebündelten Mermaid-Lib nicht unterstützt (experimenteller Typ) |
| 6 | **Venn-Diagramm** (`venn`) | ❌ nicht unterstützt | Wird von der gebündelten Mermaid-Lib nicht unterstützt (experimenteller Typ) |
