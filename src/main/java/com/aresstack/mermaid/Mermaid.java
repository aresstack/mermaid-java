package com.aresstack.mermaid;

/**
 * One-stop API for rendering Mermaid diagram code to SVG.
 *
 * <h3>Quick start</h3>
 * <pre>
 *   String svg = Mermaid.render("graph TD; A--&gt;B;");
 * </pre>
 *
 * <p>The returned SVG is post-processed so it is well-formed and compatible
 * with Apache Batik and other strict SVG rasterisers.  If you only need the
 * raw Mermaid output without Batik fixes, use {@link #renderRaw(String)}.
 *
 * <p>All methods are thread-safe; the underlying GraalJS engine and Mermaid
 * bundle are initialised lazily on first use and cached for the lifetime of
 * the JVM.
 */
public final class Mermaid {

    private Mermaid() { /* static utility */ }

    /**
     * Render a Mermaid diagram to SVG with all post-processing and
     * Batik-compatibility fixes applied.
     *
     * <pre>
     *   String svg = Mermaid.render("graph TD; A--&gt;B;");
     * </pre>
     *
     * @param diagramCode Mermaid definition, e.g. {@code "graph TD; A-->B;"}
     * @return ready-to-use SVG string, or {@code null} if rendering failed
     */
    public static String render(String diagramCode) {
        String raw = renderRaw(diagramCode);
        if (raw == null) {
            return null;
        }
        return MermaidSvgFixup.fixForBatik(raw, diagramCode);
    }

    /**
     * Render a Mermaid diagram to SVG <em>without</em> Batik post-processing.
     * <p>
     * The returned SVG already has basic sanitisation applied (namespace
     * fixups, foreignObject conversion, etc.) but does <em>not</em> include
     * the more extensive DOM-level fixes from {@link MermaidSvgFixup}.
     *
     * @param diagramCode Mermaid definition
     * @return raw SVG string, or {@code null} if rendering failed
     */
    public static String renderRaw(String diagramCode) {
        return MermaidRenderer.getInstance().renderToSvg(diagramCode);
    }

    /**
     * Render a Mermaid diagram and return a detailed result object that
     * contains either the SVG output or an error description.
     *
     * @param diagramCode Mermaid definition
     * @return result with {@link JsExecutionResult#isSuccessful()} and
     *         either {@link JsExecutionResult#getOutput()} (raw SVG) or
     *         {@link JsExecutionResult#getErrorMessage()}
     */
    public static JsExecutionResult renderDetailed(String diagramCode) {
        return MermaidRenderer.getInstance().renderToSvgDetailed(diagramCode);
    }
}

