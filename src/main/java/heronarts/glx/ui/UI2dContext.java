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

import heronarts.glx.View;
import heronarts.glx.ui.vg.VGraphics;

/**
 * TODO(mcslee): Deprecate UI2dContext? Can we always use UI2dContainer in
 * this new framework? Or do we still need some off-screen buffers...
 */
public class UI2dContext extends UI2dContainer implements UILayer {

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
    this.framebuffer = ui.vg.createFramebuffer(w, h, 0);
  }

  public short getTexture() {
    return (short) this.framebuffer.texture();
  }

  public UI2dContext setView(short viewId) {
    this.framebuffer.setView(viewId);
    return this;
  }

  public VGraphics.Paint getPaint() {
    return this.framebuffer.paint;
  }

  /**
   * Renders the content of the context to its framebuffer.
   *
   * @param vg
   */
  protected final void render(VGraphics vg) {
    // Bind the framebuffer, which rebuilds if necessary
    vg.bindFramebuffer(this.framebuffer);
    vg.beginFrame(this.width, this.height, 1);
    super.draw(ui, vg);
    vg.endFrame();

    // Note: this super.draw() call will have cleared the
    // needsRedraw and childNeedsRedraw flags on this element
    // and everything below it. That's fine, but we'll mark
    // ourselves as still needing a blitting operation so
    // that the draw() pass gets our pixels out.
    this.needsBlit = true;
  }

  @Override
  public final void draw(UI ui, View view) {
    if (!isVisible()) {
      return;
    }

    // NOTE: no rendering happens inside this method. The previous render() pass
    // will have ensured that our texture was rendered properly if it
    // needed to be.
    view.image(this);
  }

  @Override
  public void draw(UI ui, VGraphics vg) {
    if (!isVisible()) {
      return;
    }

    // NOTE: similar to the above, we don't need to actually draw ourselves
    // here, we can assume that the render() pass has already happened and
    // our framebuffer has been generated appropriately. So we just nest
    // ourselves into the vg context.

    vg.beginPath();
    vg.fillPaint(this.framebuffer.paint);
    vg.rect(0, 0, this.width, this.height);
    vg.fill();

    this.needsBlit = false;
  }

  @Override
  protected void onResize() {
    this.framebuffer.markForResize((int) this.width, (int) this.height);
    redraw();
  }

}
