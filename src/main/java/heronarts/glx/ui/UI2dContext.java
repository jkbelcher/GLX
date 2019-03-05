/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.glx.ui;

import static org.lwjgl.bgfx.BGFX.*;
import heronarts.glx.View;
import heronarts.glx.ui.vg.VGraphics;

/**
 * TODO(mcslee): Deprecate UI2dContext? Can we always use UI2dContainer in
 * this new framework? Or do we still need some off-screen buffers...
 */
public class UI2dContext extends UI2dContainer implements UILayer {

  private final View view;
  private VGraphics.Framebuffer framebuffer;

  /**
   * Constructs a new UI2dContext
   *
   * @param ui the UI to place it in
   * @param x x-position
   * @param y y-position
   * @param w width
   * @param h height
   */
  public UI2dContext(UI ui, int x, int y, int w, int h) {
    super(x, y, w, h);
    setUI(ui);
    this.view = new View(ui.lx, 0, 0, w, h);
    this.view.setClearFlags(BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL);
    this.framebuffer = ui.vg.createFramebuffer(w, h, 0);
    this.framebuffer.setView(this.view.getId());
  }

  public short getTexture() {
    return (short) this.framebuffer.texture();
  }

  /**
   * Renders the content of the context to its framebuffer.
   *
   * @param vg
   */
  protected final void render(VGraphics vg) {
    vg.bindFramebuffer(this.framebuffer);
    vg.beginFrame(this.width, this.height, 1);
    super.draw(ui, vg);
    vg.endFrame();
  }

  @Override
  public final void draw(UI ui, View view) {
    if (!isVisible()) {
      return;
    }
    if (this.framebuffer.isStale()) {
      this.framebuffer.rebuffer();
    }
    if (this.needsRedraw || this.childNeedsRedraw) {
      render(ui.vg);
    }
    view.image(this);
  }

  @Override
  public void draw(UI ui, VGraphics vg) {
    if (!isVisible()) {
      return;
    }
    if (this.framebuffer.isStale()) {
      this.framebuffer.rebuffer();
    }
    if (this.needsRedraw || this.childNeedsRedraw) {
      // NanoVG is not re-entrant... and we're rendering one
      // UI2dContext into another. The solution is BGFX views,
      // we'll schedule this context to be rendered by the engine
      // so that its texture is updated and available
      ui.pushRender(this);
    }

    vg.beginPath();
    vg.fillPaint(this.framebuffer.paint);
    vg.rect(0, 0, this.width, this.height);
    vg.fill();
  }

  @Override
  protected void onResize() {
    this.framebuffer.resize((int) this.width, (int) this.height);
    redraw();
  }

}
