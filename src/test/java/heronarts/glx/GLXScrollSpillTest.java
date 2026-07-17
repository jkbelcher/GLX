/**
 * Copyright 2026- Mark C. Slee, Heron Arts LLC
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
 * @author Justin Belcher <justin@jkb.studio>
 */

package heronarts.glx;

import static org.lwjgl.bgfx.BGFX.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dContext;
import heronarts.glx.ui.UI2dScrollContainer;
import heronarts.glx.ui.UI2dScrollContext;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;

/**
 * Manual regression rig for scissored scrolling. Panes of text bounce-scroll
 * continuously, covering the scroll-context, nested-context, scroll-container,
 * and fractional-size cases. After a fixed number of frames, each pane's
 * framebuffer is read back and dumped as a PNG into the directory given as
 * the first program argument, and the process exits. Bright pixels just
 * outside a pane's box in the dumps indicate scissor spill.
 */
public class GLXScrollSpillTest extends GLX {

  private static final int NUM_ROWS = 40;
  private static final float ROW_HEIGHT = 16;
  private static final float PANE_W = 220;
  private static final float PANE_H = 300;
  private static final float SCROLL_STEP = 0.7f;
  private static final int CAPTURE_FRAME = 900;
  private static final int DUMP_FRAME = CAPTURE_FRAME + 20;

  private static String captureDir = ".";

  protected GLXScrollSpillTest(WindowEngine windowEngine) throws IOException {
    super(windowEngine);
  }

  private static float contentHeight() {
    return NUM_ROWS * ROW_HEIGHT;
  }

  private static void fill(UI2dContainer container) {
    for (int i = 0; i < NUM_ROWS; ++i) {
      new UILabel(4, i * ROW_HEIGHT, PANE_W - 8, ROW_HEIGHT - 2, "WWMM 888 Row " + i + " ####")
        .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE)
        .addToContainer(container);
    }
  }

  // ---- Capture machinery, all driven from the bgfx thread via paneA onDraw ----

  private static class Capture {
    final String name;
    final UI2dContext context;
    short readTex = -1;
    ByteBuffer data = null;
    int texWidth, texHeight;

    Capture(String name, UI2dContext context) {
      this.name = name;
      this.context = context;
    }

    void blitAndRead(GLX glx, int blitView) {
      this.texWidth = (int) Math.ceil(this.context.getWidth() * glx.windowEngine.mainWindow.getUIContentScaleX());
      this.texHeight = (int) Math.ceil(this.context.getHeight() * glx.windowEngine.mainWindow.getUIContentScaleY());
      this.readTex = bgfx_create_texture_2d(this.texWidth, this.texHeight, false, 1,
        BGFX_TEXTURE_FORMAT_BGRA8,
        BGFX_TEXTURE_BLIT_DST | BGFX_TEXTURE_READ_BACK, null, 0);
      short srcTex = bgfx_get_texture(this.context.getTexture(), 0);
      bgfx_blit(blitView, this.readTex, 0, 0, 0, 0, srcTex, 0, 0, 0, 0, this.texWidth, this.texHeight, 0);
      this.data = MemoryUtil.memAlloc(this.texWidth * this.texHeight * 4);
      bgfx_read_texture(this.readTex, this.data, 0, 0);
    }

    void dump() throws IOException {
      BufferedImage img = new BufferedImage(this.texWidth, this.texHeight, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < this.texHeight; ++y) {
        for (int x = 0; x < this.texWidth; ++x) {
          int i = 4 * (y * this.texWidth + x);
          int b = this.data.get(i) & 0xff;
          int g = this.data.get(i + 1) & 0xff;
          int r = this.data.get(i + 2) & 0xff;
          img.setRGB(x, y, (r << 16) | (g << 8) | b);
        }
      }
      File out = new File(captureDir, this.name + ".png");
      ImageIO.write(img, "png", out);
      GLX.log("CAPTURED " + out.getAbsolutePath() + " " + this.texWidth + "x" + this.texHeight);
    }
  }

  // NOTE: no initializers - buildUI() runs from the super constructor before
  // field initializers on this class execute
  private List<Capture> captures;
  private int frameCount;
  private boolean dumped;

  private void captureTick() {
    ++this.frameCount;
    if (this.frameCount == CAPTURE_FRAME) {
      int blitView = 250;
      for (Capture capture : this.captures) {
        capture.blitAndRead(this, blitView++);
      }
      GLX.log("CAPTURE issued at frame " + this.frameCount);
    } else if (this.frameCount >= DUMP_FRAME && !this.dumped) {
      this.dumped = true;
      try {
        for (Capture capture : this.captures) {
          capture.dump();
        }
      } catch (IOException x) {
        GLX.error(x);
      }
      GLX.log("CAPTURE COMPLETE");
      new Thread(() -> {
        try { Thread.sleep(500); } catch (InterruptedException ix) {}
        System.exit(0);
      }).start();
    }
  }

  private static class Bounce {
    float dir = -SCROLL_STEP;
  }

  @Override
  protected UI buildUI() throws IOException {
    UI ui = super.buildUI();
    this.captures = new ArrayList<>();

    // A: bare scroll context as a top-level layer; also drives frame counting
    final Bounce bounceA = new Bounce();
    UI2dScrollContext paneA = new UI2dScrollContext(ui, 40, 60, PANE_W, PANE_H) {
      @Override
      protected void onDraw(UI ui, VGraphics vg) {
        captureTick();
        float sy = getScrollY() + bounceA.dir;
        if (sy <= this.height - getScrollHeight() || sy >= 0) {
          bounceA.dir = -bounceA.dir;
        }
        setScrollY(sy);
      }
    };
    paneA.setBackgroundColor(0xff262626);
    paneA.setScrollHeight(contentHeight());
    fill(paneA);
    ui.addLayer(paneA);

    // B: scroll context nested inside a plain parent context
    UI2dContext parentB = new UI2dContext(ui, 320, 60, PANE_W + 40, PANE_H + 80);
    parentB.setBackgroundColor(0xff3a3a44);
    final Bounce bounceB = new Bounce();
    UI2dScrollContext paneB = new UI2dScrollContext(ui, 20, 40, PANE_W, PANE_H) {
      @Override
      protected void onDraw(UI ui, VGraphics vg) {
        float sy = getScrollY() + bounceB.dir;
        if (sy <= this.height - getScrollHeight() || sy >= 0) {
          bounceB.dir = -bounceB.dir;
        }
        setScrollY(sy);
      }
    };
    paneB.setBackgroundColor(0xff262626);
    paneB.setScrollHeight(contentHeight());
    fill(paneB);
    paneB.addToContainer(parentB);
    ui.addLayer(parentB);

    // C: scroll container (non-context) inside a parent context
    UI2dContext parentC = new UI2dContext(ui, 620, 60, PANE_W + 40, PANE_H + 80);
    parentC.setBackgroundColor(0xff443a3a);
    final Bounce bounceC = new Bounce();
    UI2dScrollContainer paneC = new UI2dScrollContainer(ui, 20, 40, PANE_W, PANE_H) {
      @Override
      protected void onDraw(UI ui, VGraphics vg) {
        float sy = getScrollY() + bounceC.dir;
        if (sy <= this.height - getScrollHeight() || sy >= 0) {
          bounceC.dir = -bounceC.dir;
        }
        setScrollY(sy);
      }
    };
    paneC.setBackgroundColor(0xff262626);
    paneC.setScrollHeight(contentHeight());
    fill(paneC);
    paneC.addToContainer(parentC);
    ui.addLayer(parentC);

    // D: scroll context with fractional height, so that height * contentScale
    // is fractional and the buffer carries a never-repainted ceil strip
    final Bounce bounceD = new Bounce();
    UI2dScrollContext paneD = new UI2dScrollContext(ui, 40, 380, PANE_W, 300.3f) {
      @Override
      protected void onDraw(UI ui, VGraphics vg) {
        float sy = getScrollY() + bounceD.dir;
        if (sy <= this.height - getScrollHeight() || sy >= 0) {
          bounceD.dir = -bounceD.dir;
        }
        setScrollY(sy);
      }
    };
    paneD.setBackgroundColor(0xff262626);
    paneD.setScrollHeight(contentHeight());
    fill(paneD);
    ui.addLayer(paneD);

    this.captures.add(new Capture("paneA", paneA));
    this.captures.add(new Capture("paneB", paneB));
    this.captures.add(new Capture("parentB", parentB));
    this.captures.add(new Capture("parentC", parentC));
    this.captures.add(new Capture("paneD", paneD));

    return ui;
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      captureDir = args[0];
    }
    final Flags flags = new Flags();
    flags.windowWidth = 920;
    flags.windowHeight = 460;
    final WindowEngine windowEngine = new WindowEngine(flags);
    new Thread(() -> {
      try {
        new GLXScrollSpillTest(windowEngine).run();
      } catch (Exception x) {
        GLX.error(x);
      }
    }).start();

    // Run GLFW main loop
    windowEngine.main();
  }
}
