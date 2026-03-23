package com.aresstack.mermaid;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One-stop API for rendering Mermaid diagram code to SVG or {@link BufferedImage}.
 *
 * <h3>Quick start</h3>
 * <pre>
 *   // SVG string:
 *   String svg = Mermaid.render("graph TD; A--&gt;B;");
 *
 *   // BufferedImage (ready for Swing, JavaFX, ImageIO.write, …):
 *   BufferedImage img = Mermaid.renderToImage("graph TD; A--&gt;B;");
 * </pre>
 *
 * <p>All methods are thread-safe; the underlying GraalJS engine and Mermaid
 * bundle are initialised lazily on first use and cached for the lifetime of
 * the JVM.
 */
public final class Mermaid {

    private static final Logger LOG = Logger.getLogger(Mermaid.class.getName());

    private Mermaid() { /* static utility */ }

    // ═══════════════════════════════════════════════════════════
    //  SVG output
    // ═══════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════
    //  Image output
    // ═══════════════════════════════════════════════════════════

    /**
     * Render a Mermaid diagram directly to a {@link BufferedImage} at the
     * intrinsic SVG size (typically ~2 000 px on the larger axis).
     * The image is auto-cropped to remove surrounding whitespace.
     *
     * <pre>
     *   BufferedImage img = Mermaid.renderToImage("graph TD; A--&gt;B;");
     *   ImageIO.write(img, "png", new File("diagram.png"));
     * </pre>
     *
     * @param diagramCode Mermaid definition
     * @return rasterised image, or {@code null} if rendering failed
     */
    public static BufferedImage renderToImage(String diagramCode) {
        String svg = render(diagramCode);
        return svg == null ? null : svgToImage(svg);
    }

    /**
     * Render a Mermaid diagram to a {@link BufferedImage} at an <b>exact</b>
     * pixel width.  Height is determined automatically from the SVG aspect
     * ratio.  Useful for zoom / hi-DPI / thumbnails.
     *
     * <pre>
     *   // 4K-width render:
     *   BufferedImage hi = Mermaid.renderToImage("graph TD; A--&gt;B;", 3840);
     *
     *   // Thumbnail:
     *   BufferedImage thumb = Mermaid.renderToImage("graph TD; A--&gt;B;", 300);
     * </pre>
     *
     * @param diagramCode Mermaid definition
     * @param width       exact output width in pixels
     * @return rasterised image, or {@code null} if rendering failed
     */
    public static BufferedImage renderToImage(String diagramCode, int width) {
        String svg = render(diagramCode);
        return svg == null ? null : svgToImage(svg, width);
    }

    // ═══════════════════════════════════════════════════════════
    //  SVG → Image conversion (for pre-rendered SVG strings)
    // ═══════════════════════════════════════════════════════════

    /**
     * Convert an SVG string to a {@link BufferedImage} at intrinsic size.
     * The image is auto-cropped to remove surrounding whitespace.
     *
     * <p>Use this when you already have an SVG string (e.g. from
     * {@link #render(String)}) and want to rasterise it.
     *
     * @param svg SVG document as string
     * @return rasterised image, or {@code null} on failure
     */
    public static BufferedImage svgToImage(String svg) {
        return svgToImage(svg, -1);
    }

    /**
     * Convert an SVG string to a {@link BufferedImage} at an exact pixel width.
     * Pass {@code width <= 0} for intrinsic size.
     * The image is auto-cropped to remove surrounding whitespace.
     *
     * @param svg   SVG document as string
     * @param width exact output width in pixels, or &le; 0 for intrinsic
     * @return rasterised image, or {@code null} on failure
     */
    public static BufferedImage svgToImage(String svg, int width) {
        if (svg == null || svg.isEmpty()) return null;
        try {
            byte[] svgBytes = sanitizeForBatik(svg).getBytes("UTF-8");
            BatikTranscoder transcoder = new BatikTranscoder();
            if (width > 0) {
                transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
            }
            TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgBytes));
            transcoder.transcode(input, new TranscoderOutput());
            return autoCrop(transcoder.getImage());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[Mermaid] SVG rasterisation failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Auto-crop
    // ═══════════════════════════════════════════════════════════

    /**
     * Trim transparent and white pixels from the edges of an image.
     * Returns a cropped copy, or the original if no significant trimming
     * is possible.
     *
     * @param src source image (may be {@code null})
     * @return cropped image, or {@code null} if src was {@code null}
     */
    public static BufferedImage autoCrop(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        int top = 0, bottom = h - 1, left = 0, right = w - 1;

        outer_top:
        for (; top < h; top++)
            for (int x = 0; x < w; x++)
                if (isContentPixel(src.getRGB(x, top))) break outer_top;

        outer_bottom:
        for (; bottom > top; bottom--)
            for (int x = 0; x < w; x++)
                if (isContentPixel(src.getRGB(x, bottom))) break outer_bottom;

        outer_left:
        for (; left < w; left++)
            for (int y = top; y <= bottom; y++)
                if (isContentPixel(src.getRGB(left, y))) break outer_left;

        outer_right:
        for (; right > left; right--)
            for (int y = top; y <= bottom; y++)
                if (isContentPixel(src.getRGB(right, y))) break outer_right;

        int margin = 8;
        top = Math.max(0, top - margin);
        bottom = Math.min(h - 1, bottom + margin);
        left = Math.max(0, left - margin);
        right = Math.min(w - 1, right + margin);

        int cw = right - left + 1;
        int ch = bottom - top + 1;
        if (cw >= w - 2 && ch >= h - 2) return src;

        return src.getSubimage(left, top, cw, ch);
    }

    // ═══════════════════════════════════════════════════════════
    //  Internal helpers
    // ═══════════════════════════════════════════════════════════

    /** Returns true if the pixel is NOT fully transparent and NOT near-white. */
    private static boolean isContentPixel(int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a < 10) return false;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r < 250 || g < 250 || b < 250;
    }

    /**
     * Strip CSS properties that crash Batik's CSSEngine
     * (alignment-baseline, dominant-baseline).
     */
    private static String sanitizeForBatik(String svg) {
        boolean changed = false;
        if (svg.contains("alignment-baseline")) {
            svg = svg.replaceAll("\\s+alignment-baseline\\s*=\\s*\"[^\"]*\"", "");
            svg = svg.replaceAll("\\s+alignment-baseline\\s*=\\s*'[^']*'", "");
            svg = svg.replaceAll("alignment-baseline\\s*:\\s*[^;\"'}<]+[;]?", "");
            changed = true;
        }
        if (svg.contains("dominant-baseline")) {
            svg = svg.replaceAll("\\s+dominant-baseline\\s*=\\s*\"[^\"]*\"", "");
            svg = svg.replaceAll("\\s+dominant-baseline\\s*=\\s*'[^']*'", "");
            svg = svg.replaceAll("dominant-baseline\\s*:\\s*[^;\"'}<]+[;]?", "");
            changed = true;
        }
        if (changed) {
            svg = svg.replaceAll("\\s+style\\s*=\\s*\"[;\\s]*\"", "");
            svg = svg.replaceAll("\\s+style\\s*=\\s*'[;\\s]*'", "");
        }
        return svg;
    }

    /** Batik transcoder that captures the rasterised BufferedImage. */
    private static final class BatikTranscoder extends ImageTranscoder {
        private BufferedImage image;

        @Override
        public BufferedImage createImage(int w, int h) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput out) {
            this.image = img;
        }

        BufferedImage getImage() {
            return image;
        }
    }
}
