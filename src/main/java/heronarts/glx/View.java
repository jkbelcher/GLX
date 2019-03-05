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

package heronarts.glx;

import static org.lwjgl.bgfx.BGFX.*;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.ui.UI2dContext;

public class View {

  protected final GLX glx;

  protected short viewId;

  protected int x = 0, y = 0, width = 0, height = 0;

  private int clearColor = 0x000000ff;
  private int clearFlags = BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL;
  private float clearDepth = 1f;

  protected final Matrix4f viewMatrix = new Matrix4f();
  protected final FloatBuffer viewMatrixBuf;
  protected final Matrix4f projectionMatrix = new Matrix4f();
  protected final FloatBuffer projectionMatrixBuf;

  public View(GLX glx) {
    this(glx, 0, 0, glx.getWindowWidth(), glx.getWindowHeight());
  }

  public View(GLX glx, int x, int y, int w, int h) {
    this.glx = glx;
    this.viewId = 0;

    this.x = x;
    this.y = 0;
    this.width = w;
    this.height = h;

    // Set up model, view and projection matrices
    this.viewMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.projectionMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.viewMatrix.get(this.viewMatrixBuf);
    this.projectionMatrix.get(this.projectionMatrixBuf);

    _setViewClear();
    _setViewRect();
    _setViewTransform();
  }

  /**
   * Subclasses may override
   */
  protected void initViewMatrices() {}

  public short getId() {
    return this.viewId;
  }

  public View setId(short viewId) {
    this.viewId = viewId;
    bgfx_reset_view(this.viewId);
    _setViewClear();
    _setViewRect();
    _setViewTransform();
    return this;
  }

  public View touch() {
    bgfx_touch(this.viewId);
    return this;
  }

  public View setOrtho(float width, float height) {
    this.viewMatrix.identity();
    this.viewMatrix.get(this.viewMatrixBuf);
    this.projectionMatrix.setOrthoLH(0, width, height, 0, -1, 1, this.glx.zZeroToOne);
    this.projectionMatrix.get(this.projectionMatrixBuf);
    _setViewTransform();
    return this;
  }

  public View setRect(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    _setViewRect();
    return this;
  }

  public View setClearColor(int rgba) {
    if (this.clearColor != rgba) {
      this.clearColor = rgba;
      _setViewClear();
    }
    return this;
  }

  public View setClearFlags(int clearFlags) {
    if (this.clearFlags != clearFlags) {
      this.clearFlags = clearFlags;
      _setViewClear();
    }
    return this;
  }

  public View setClearDepth(float clearDepth) {
    if (this.clearDepth != clearDepth) {
      this.clearDepth = clearDepth;
      _setViewClear();
    }
    return this;
  }

  public View image(UI2dContext context) {
    this.glx.program.tex2d.submit(
      this,
      bgfx_get_texture(context.getTexture(), 0),
      context.getX(),
      context.getY(),
      context.getWidth(),
      context.getHeight()
    );
    return this;
  }

  protected void _setViewClear() {
    bgfx_set_view_clear(this.viewId, this.clearFlags, this.clearColor, this.clearDepth, 0);
  }

  protected void _setViewRect() {
    bgfx_set_view_rect(this.viewId, this.x, this.y, this.width, this.height);
  }

  protected void _setViewTransform() {
    bgfx_set_view_transform(this.viewId, this.viewMatrixBuf, this.projectionMatrixBuf);
  }

  public void dispose() {
    MemoryUtil.memFree(this.viewMatrixBuf);
    MemoryUtil.memFree(this.projectionMatrixBuf);
  }
}
