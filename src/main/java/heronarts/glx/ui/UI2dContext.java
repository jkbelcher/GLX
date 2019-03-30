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
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.glx.ui;

import heronarts.glx.View;
import heronarts.glx.ui.vg.VGraphics;

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

    // Framebuffers are in frame-buffer pixel space. If content-scaling
    // is being applied we must scale.
    this.framebuffer = ui.vg.createFramebuffer(
      (int) (w * ui.getContentScaleX()),
      (int) (h * ui.getContentScaleY()),
      0
    );
  }

  public short getTexture() {
    return (short) this.framebuffer.handle();
  }

  public UI2dContext setView(short viewId) {
    this.framebuffer.setView(viewId);
    return this;
  }

  public VGraphics.Paint getPaint() {
    return this.framebuffer.paint;
  }

  /**
   * Renders the content of the context to its own framebuffer. Note that this
   * method assumes that all nested VGraphics instances beneath this one in the
   * tree have themselves already been rendered. This is because NanoVG is not
   * re-entrant and we share one instance.
   *
   * @param vg VGraphics instance
   */
  protected final void render(VGraphics vg) {
    // Bind the framebuffer, which rebuilds if necessary
    vg.bindFramebuffer(this.framebuffer);

    // NanoVG operates in framebuffer pixel-space, but it is aware of content scaling. So we
    // pass the framebuffer bounds here, but tell NanoVG what content scaling our UI uses.
    // NOTE - NanoVG only takes a single scaling parameter. If some machine out there actually
    // utilizes different X and Y scaling, this is going to be a real mess...
    vg.beginFrame(
      this.framebuffer.getWidth(),
      this.framebuffer.getHeight(),
      this.ui.getContentScaleX()
    );
    super.draw(this.ui, vg);
    vg.endFrame();

    // Note: this super.draw() call will have cleared the
    // needsRedraw and childNeedsRedraw flags on this element
    // and everything below it. That's fine, but we'll mark
    // ourselves as still needing a blitting operation so
    // that the draw() pass gets our pixels out.
    this.needsBlit = true;
  }

  /**
   * Draws the context into a parent view. This method assumes that this
   * context's framebuffer texture has already been generated and is ready
   * to be drawn.
   *
   * @param ui UI context
   * @param view Parent view to draw into
   */
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

  /**
   * Draws this context into the given graphics context. Note that in this case
   * we assume our framebuffer is already drawn and that we're just blitting
   * it into a parent 2d context.
   *
   * @param ui UI context
   * @param vg Graphics context
   */
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
    // Reminder! Framebuffers are in frame-buffer pixel space. If content-scaling
    // is being applied we must scale from UI space to  framebuffer space.
    this.framebuffer.markForResize(
      (int) (this.width * this.ui.getContentScaleX()),
      (int) (this.height * this.ui.getContentScaleY())
    );
    redraw();
  }

}
