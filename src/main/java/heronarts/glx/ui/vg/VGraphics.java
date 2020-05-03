/**
 * Copyright 2019- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.glx.ui.vg;

import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGLUFramebufferBGFX;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.View;
import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGBGFX.*;

/**
 * Pretty much just a wrapper around the LWJGL NanoVGBGFX library, that makes it a bit more
 * idiomatic java style and doesn't require the API client to keep track of the NanoVG context
 * handle or prefix everything with nvg. Also makes for simpler method chaining calls.
 */
public class VGraphics {

  public static enum Winding {
    CCW(NVG_CCW),
    CW(NVG_CW);

    private final int raw;

    private Winding(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum Align {
    LEFT(NVG_ALIGN_LEFT, true),
    CENTER(NVG_ALIGN_CENTER, true),
    RIGHT(NVG_ALIGN_RIGHT, true),
    TOP(NVG_ALIGN_TOP, false),
    MIDDLE(NVG_ALIGN_MIDDLE, false),
    BOTTOM(NVG_ALIGN_BOTTOM, false),
    BASELINE(NVG_ALIGN_BASELINE, false);

    private final int raw;
    private boolean isHorizontal;

    private Align(int raw, boolean isHorizontal) {
      this.raw = raw;
      this.isHorizontal = isHorizontal;
    }

    public boolean isHorizontal() {
      return this.isHorizontal;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum LineJoin {
    MITER(NVG_MITER),
    ROUND(NVG_ROUND),
    BEVEL(NVG_BEVEL);

    private final int raw;

    private LineJoin(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum LineCap {
    BUTT(NVG_BUTT),
    ROUND(NVG_ROUND),
    SQUARE(NVG_SQUARE);

    private final int raw;

    private LineCap(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public class Font {
    public final int id;
    public final String name;
    public float size = 10;

    private Font(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public Font fontSize(float size) {
      this.size = size;
      return this;
    }
  }

  public class Image {
    public final int id;
    public final int width;
    public final int height;
    public final Paint paint;
    private final NVGColor tint = NVGColor.create();
    private final ByteBuffer imageData;

    private Image(int id, ByteBuffer imageData, int w, int h, boolean is2x) {
      this.id = id;
      this.imageData = imageData;
      if (is2x) {
        w /= 2;
        h /= 2;
      }
      this.width = w;
      this.height = h;
      this.paint = imagePattern(0, 0, w, h, id);
      noTint();
    }

    public void noTint() {
      setTint(1f, 1f, 1f, 1f);
    }

    public void setTint(int argb) {
      setTint(
        (0xff & (argb >>> 16)) / 255f,
        (0xff & (argb >>> 8)) / 255f,
        (0xff & argb) / 255f,
        (argb >>> 24) / 255f
      );
    }

    public void setTint(float r, float g, float b, float a) {
      this.tint.r(r);
      this.tint.g(g);
      this.tint.b(b);
      this.tint.a(a);
    }

    public void dispose() {
      nvgDeleteImage(vg, this.id);
      MemoryUtil.memFree(this.imageData);
    }
  }

  public class Paint {

    private final NVGPaint paint = NVGPaint.create();

    public Paint imagePattern(float ox, float oy, float ex, float ey, int image) {
      return imagePattern(ox, oy, ex, ey, 1f, image);
    }

    public Paint imagePattern(float ox, float oy, float ex, float ey, float alpha, int image) {
      nvgImagePattern(vg, ox, oy, ex, ey, 0, image, alpha, this.paint);
      return this;
    }
  }

  public class Framebuffer {
    private NVGLUFramebufferBGFX buffer;
    public final Paint paint = new Paint();
    private float width;
    private float height;
    private short viewId;
    private final int imageFlags;
    private boolean isStale = false;

    public Framebuffer(float w, float h, int imageFlags) {
      this.width = w;
      this.height = h;
      this.imageFlags = imageFlags;
      this.viewId = 0;
      makeBuffer();
    }

    public Framebuffer markStale() {
      this.isStale = true;
      return this;
    }

    public boolean isStale() {
      return this.isStale;
    }

    public int image() {
      return this.buffer.image();
    }

    public int handle() {
      return this.buffer.handle();
    }

    public float getWidth() {
      return this.width;
    }

    public float getHeight() {
      return this.height;
    }

    public Framebuffer setView(short viewId) {
      this.viewId = viewId;
      return this;
    }

    public Framebuffer bind() {
      if (this.isStale) {
        rebuffer();
      }
      view.bind(this.viewId);
      nvgluSetViewFramebuffer(this.viewId, this.buffer);
      nvgluBindFramebuffer(this.buffer);
      return this;
    }

    public void markForResize(float w, float h) {
      if (this.width != w || this.height != h) {
        this.width = w;
        this.height = h;
        markStale();
      }
    }

    public void rebuffer() {
      nvgluDeleteFramebuffer(this.buffer);
      makeBuffer();
      this.isStale = false;
    }

    private void makeBuffer() {
      this.buffer = nvgluCreateFramebuffer(vg, (int) Math.ceil(this.width), (int) Math.ceil(this.height), this.imageFlags);

      // Note what happens here... the framebuffer is in framebuffer-pixel space. But
      // when we're going to paint it into another UI2dContext, those pixels will be in
      // UI-space. Therefore, we need to transform the image by content-scaling factor.
      this.paint.imagePattern(0, 0, this.width / glx.getContentScaleX(), this.height / glx.getContentScaleY(), this.buffer.image());
    }

  }

  private final GLX glx;
  private final View view;
  private final long vg;
  private final NVGColor fillColor = NVGColor.create();
  private final NVGColor strokeColor = NVGColor.create();
  private final Set<Framebuffer> allocatedBuffers = new HashSet<Framebuffer>();

  public VGraphics(GLX glx) {
    this.glx = glx;
    this.vg = nvgCreate(true, 0, NULL);
    this.view = new View(glx);
    this.view.setClearFlags(BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL);
  }

  public long getHandle() {
    return this.vg;
  }

  public Framebuffer createFramebuffer(float w, float h, int imageFlags) {
    Framebuffer framebuffer = new Framebuffer(w, h, imageFlags);
    this.allocatedBuffers.add(framebuffer);
    return framebuffer;
  }

  public void bindFramebuffer(Framebuffer framebuffer) {
    framebuffer.bind();
  }

  public void deleteFrameBuffer(Framebuffer framebuffer) {
    nvgluDeleteFramebuffer(framebuffer.buffer);
    this.allocatedBuffers.remove(framebuffer);
  }

  public VGraphics fillColor(int argb) {
    return fillColor(
      (0xff & (argb >>> 16)) / 255f,
      (0xff & (argb >>> 8)) / 255f,
      (0xff & argb) / 255f,
      (argb >>> 24) / 255f
    );
  }

  public VGraphics fillColor(float r, float g, float b) {
    return fillColor(r, g, b, 1f);
  }

  public VGraphics fillColor(float r, float g, float b, float a) {
    this.fillColor.a(a);
    this.fillColor.r(r);
    this.fillColor.g(g);
    this.fillColor.b(b);
    nvgFillColor(this.vg, this.fillColor);
    return this;
  }

  public VGraphics fillPaint(Paint paint) {
    nvgFillPaint(this.vg, paint.paint);
    return this;
  }

  public VGraphics strokeColor(int argb) {
    return strokeColor(
      (0xff & (argb >>> 16)) / 255f,
      (0xff & (argb >>> 8)) / 255f,
      (0xff & argb) / 255f,
      (argb >>> 24) / 255f
    );
  }

  public VGraphics strokeWidth(float strokeWidth) {
    nvgStrokeWidth(this.vg, strokeWidth);
    return this;
  }

  public VGraphics strokeColor(float r, float g, float b) {
    return strokeColor(r, g, b, 1f);
  }

  public VGraphics strokeColor(float r, float g, float b, float a) {
    this.strokeColor.a(a);
    this.strokeColor.r(r);
    this.strokeColor.g(g);
    this.strokeColor.b(b);
    nvgStrokeColor(this.vg, this.strokeColor);
    return this;
  }

  public VGraphics lineJoin(LineJoin lineJoin) {
    nvgLineJoin(this.vg, lineJoin.asInt());
    return this;
  }

  public VGraphics lineCap(LineCap lineCap) {
    nvgLineCap(this.vg, lineCap.asInt());
    return this;
  }

  public VGraphics beginFrame(float width, float height, float contentScale) {
    nvgBeginFrame(this.vg, width, height, contentScale);
    return this;
  }

  public VGraphics endFrame() {
    nvgEndFrame(this.vg);
    return this;
  }

  public VGraphics beginPath() {
    nvgBeginPath(this.vg);
    return this;
  }

  public VGraphics closePath() {
    nvgClosePath(this.vg);
    return this;
  }

  public VGraphics pathWinding(Winding dir) {
    nvgPathWinding(this.vg, dir.asInt());
    return this;
  }

  public VGraphics moveTo(float x, float y) {
    nvgMoveTo(this.vg, x, y);
    return this;
  }

  public VGraphics line(float x1, float y1, float x2, float y2) {
    moveTo(x1, y1);
    return lineTo(x2, y2);
  }

  public VGraphics lineTo(float x, float y) {
    nvgLineTo(this.vg, x, y);
    return this;
  }

  public VGraphics bezierTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
    nvgBezierTo(this.vg, c1x, c1y, c2x, c2y, x, y);
    return this;
  }

  public VGraphics quadTo(float cx, float cy, float x, float y) {
    nvgQuadTo(this.vg, cx, cy, x, y);
    return this;
  }

  public VGraphics arcTo(float x1, float y1, float x2, float y2, float radius) {
    nvgArcTo(this.vg, x1, y1, x2, y2, radius);
    return this;
  }

  public VGraphics arc(float cx, float cy, float r, float a0, float a1) {
    return arc(cx, cy, r, a0, a1, Winding.CW);
  }

  public VGraphics arc(float cx, float cy, float r, float a0, float a1, Winding dir) {
    nvgArc(this.vg, cx, cy, r, a0, a1, dir.asInt());
    return this;
  }

  public VGraphics beginPathMoveToArcFill(float cx, float cy, float r, float a0, float a1) {
    if (a1 - a0 > Math.PI) {
      // NOTE(bugfix): there's a bug on windows when contentscale is applied.
      // if the arc spans over PI (180degrees) and becomes convex, then the content
      // scale becomes lost and the shape is drawn in the wrong place. will have
      // to figure this out some other time, for now, breaking into two arcs fixes it
      float aHack = a0 + (a1-a0)/2;
      beginPath();
      moveTo(cx, cy);
      arc(cx, cy, r, a0, aHack);
      fill();
      beginPath();
      moveTo(cx, cy);
      arc(cx, cy, r, aHack - .1f, a1);
      fill();
      return this;
    }
    beginPath();
    moveTo(cx, cy);
    arc(cx, cy, r, a0, a1);
    fill();
    return this;
  }

  public VGraphics rect(float x, float y, float w, float h) {
    nvgRect(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics rect(float x, float y, float w, float h, float r) {
    if (r > 0) {
      return roundedRect(x, y, w, h, r);
    }
    return rect(x, y, w, h);
  }

  public VGraphics roundedRect(float x, float y, float w, float h, float r) {
    nvgRoundedRect(this.vg, x, y, w, h, r);
    return this;
  }

  public VGraphics roundedRectVarying(float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft) {
    nvgRoundedRectVarying(this.vg, x, y, w, h, radTopLeft, radTopRight, radBottomRight, radBottomLeft);
    return this;
  }

  public VGraphics ellipse(float cx, float cy, float rx, float ry) {
    nvgEllipse(this.vg, cx, cy, rx, ry);
    return this;
  }

  public VGraphics circle(float cx, float cy, float r) {
    nvgCircle(this.vg, cx, cy, r);
    return this;
  }

  public VGraphics image(Image image, float x, float y) {
    return image(image, x, y, 1f);
  }

  public VGraphics image(Image image, float x, float y, float alpha) {
    image.paint.imagePattern(x, y, image.width, image.height, alpha, image.id);
    image.paint.paint.innerColor(image.tint);
    fillPaint(image.paint);
    return rect(x, y, image.width, image.height);
  }

  public Paint imagePattern(float ox, float oy, float ex, float ey, int image) {
    return imagePattern(ox, oy, ex, ey, 1f, image);
  }

  public Paint imagePattern(float ox, float oy, float ex, float ey, float alpha, int image) {
    return new Paint().imagePattern(ox, oy, ex, ey, alpha, image);
  }

  public VGraphics fill() {
    nvgFill(this.vg);
    return this;
  }

  public VGraphics stroke() {
    nvgStroke(this.vg);
    return this;
  }

  public Image loadImage(String imagePath) throws IOException {
    return createImageMem(GLXUtils.loadResource("resources/images/" + imagePath), imagePath.contains("@2x."));
  }

  public Image loadIcon(String iconPath) throws IOException {
    return createImageMem(GLXUtils.loadResource("resources/icons/" + iconPath), iconPath.contains("@2x."));
  }

  public Font loadFont(String fontName, String fontPath) throws IOException {
    return createFontMem(fontName, GLXUtils.loadResource("resources/fonts/" + fontPath));
  }

  private Font createFontMem(String name, ByteBuffer fontData) {
    int font = nvgCreateFontMem(this.vg, name, fontData, 0);
    return new Font(font, name);
  }

  private Image createImageMem(ByteBuffer imageData, boolean is2x) {
    int image = nvgCreateImageMem(this.vg, 0, imageData);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      nvgImageSize(this.vg, image, width, height);
      return new Image(image, imageData, width.get(0), height.get(0), is2x);
    }
  }

  public VGraphics fontSize(float size) {
    nvgFontSize(this.vg, size);
    return this;
  }

  public VGraphics fontBlur(float blur) {
    nvgFontBlur(this.vg, blur);
    return this;
  }

  public VGraphics textLetterSpacing(float spacing) {
    nvgTextLetterSpacing(this.vg, spacing);
    return this;
  }

  public VGraphics textLineHeight(float lineHeight) {
    nvgTextLineHeight(this.vg, lineHeight);
    return this;
  }

  public VGraphics textAlign(Align horizontal) {
    return textAlign(horizontal, Align.BASELINE);
  }

  public VGraphics textAlign(Align horizontal, Align vertical) {
    if (!horizontal.isHorizontal()) {
      throw new IllegalArgumentException("Cannot set horizontal alignment to vertical value: " + horizontal);
    }
    if (vertical.isHorizontal()) {
      throw new IllegalArgumentException("Cannot set vertical alignment to horizontal value: " + vertical);
    }
    nvgTextAlign(this.vg, horizontal.asInt() | vertical.asInt());
    return this;
  }

  public VGraphics fontFace(Font font) {
    nvgFontFaceId(this.vg, font.id);
    nvgFontSize(this.vg, font.size);
    return this;
  }

  public float text(float x, float y, String str) {
    return nvgText(this.vg, x, y, str);
  }

  public VGraphics textBox(float x, float y, float breakRowWidth, String str) {
    nvgTextBox(this.vg, x, y, breakRowWidth, str);
    return this;
  }

  public float textBoxHeight(String str, float width) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      FloatBuffer bounds = stack.mallocFloat(4);
      nvgTextBoxBounds(this.vg, 0, 0, width, str, bounds);
      return bounds.get(3);
    }
  }

  public float textWidth(String str) {
    return nvgTextBounds(this.vg, 0, 0, str, (FloatBuffer) null);
  }

  public VGraphics translate(float tx, float ty) {
    nvgTranslate(this.vg, tx, ty);
    return this;
  }

  public VGraphics rotate(float angle) {
    nvgRotate(this.vg, angle);
    return this;
  }

  public VGraphics scissor(float x, float y, float w, float h) {
    nvgScissor(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics intersectScissor(float x, float y, float w, float h) {
    nvgIntersectScissor(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics resetScissor() {
    nvgResetScissor(this.vg);
    return this;
  }

}
