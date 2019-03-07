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

package heronarts.glx.ui.component;

import static org.lwjgl.bgfx.BGFX.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import heronarts.glx.DynamicVertexBuffer;
import heronarts.glx.GLX;
import heronarts.glx.ShaderProgram;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.LXEngine;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BoundedParameter;

public class UIPointCloud extends UI3dComponent implements LXModel.Listener {

  private class Program extends ShaderProgram {
    private short uniformTexture;
    private short uniformDimensions;
    private final FloatBuffer dimensionsBuffer;

    Program(GLX lx) {
      super(lx, "vs_led", "fs_led");
      this.uniformTexture = bgfx_create_uniform("s_texColor", BGFX_UNIFORM_TYPE_SAMPLER, 1);
      this.uniformDimensions = bgfx_create_uniform("u_dimensions", BGFX_UNIFORM_TYPE_VEC4, 1);
      this.dimensionsBuffer = MemoryUtil.memAllocFloat(4);
    }

    @Override
    public void dispose() {
      bgfx_destroy_uniform(this.uniformTexture);
      bgfx_destroy_uniform(this.uniformDimensions);
      MemoryUtil.memFree(this.dimensionsBuffer);
      super.dispose();
    }

    @Override
    public void setVertexBuffers(View view) {
      bgfx_set_vertex_buffer(0, modelBuffer.getHandle(), 0, modelBuffer.getNumVertices());
      bgfx_set_dynamic_vertex_buffer(1, colorBuffer.getHandle(), 0, colorBuffer.getNumVertices());
    }

    @Override
    public void setUniforms(View view) {
      bgfx_set_texture(0, this.uniformTexture, texture.getHandle(), BGFX_SAMPLER_NONE);
      this.dimensionsBuffer.put(0, view.getWidth());
      this.dimensionsBuffer.put(1, view.getHeight());
      this.dimensionsBuffer.put(2, view.getAspectRatio());
      switch (getContext().projection.getEnum()) {
      case PERSPECTIVE:
        this.dimensionsBuffer.put(3, 2.0f / view.getAspectRatio() * pointSize.getValuef());
        break;
      case ORTHOGRAPHIC:
        this.dimensionsBuffer.put(3, 2.0f * pointSize.getValuef() / view.getWidth());
        break;
      }
      bgfx_set_uniform(this.uniformDimensions, this.dimensionsBuffer, 1);
    }
  }

  private class ModelBuffer extends VertexBuffer {

    private static final int VERTICES_PER_POINT = 6;

    private ModelBuffer(GLX lx) {
      super(lx, model.size * VERTICES_PER_POINT, VertexDeclaration.ATTRIB_POSITION | VertexDeclaration.ATTRIB_TEXCOORD0);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      for (LXPoint p : model.points) {
        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(0f);
        buffer.putFloat(0f);

        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(1f);
        buffer.putFloat(0f);

        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(0f);
        buffer.putFloat(1f);

        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(0f);
        buffer.putFloat(1f);

        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(1f);
        buffer.putFloat(0f);

        buffer.putFloat(p.x);
        buffer.putFloat(p.y);
        buffer.putFloat(p.z);
        buffer.putFloat(1f);
        buffer.putFloat(1f);
      }
    }
  }

  public final BoundedParameter pointSize =
    new BoundedParameter("Point Size", 5, 0, 100)
    .setDescription("Size of points rendered in the preview display");

  private final GLX lx;

  private final Program program;
  private final Texture texture;

  private VertexBuffer modelBuffer;
  private DynamicVertexBuffer colorBuffer;

  // This is the model that is active in the engine and we listen to for changes
  private LXModel model;

  // This is the model that our current vertex buffers (UI thread) is based upon,
  // which could be 1 frame behind the engine!
  private LXModel bufferModel;

  private boolean updateModelBuffer = false;

  public UIPointCloud(GLX lx) {
    this.lx = lx;
    this.program = new Program(lx);
    this.texture = new Texture("led.ktx");
    this.colorBuffer = null;
    this.modelBuffer = null;
    this.model = null;
    this.bufferModel = null;
    setModel(lx.getModel());
  }

  public void dispose() {
    this.texture.dispose();
    this.modelBuffer.dispose();
    this.colorBuffer.dispose();
    this.program.dispose();
  }

  private void setModel(LXModel model) {
    if (model == null) {
      throw new IllegalArgumentException("May not set null model on point cloud");
    }
    if (this.model != model) {
      if (this.model != null) {
        this.model.removeListener(this);
      }
      this.model = model;
      model.addListener(this);
    }
  }

  @Override
  public void onModelUpdated(LXModel model) {
    // Mark the model buffer as needing an update
    this.updateModelBuffer = true;
  }

  private void buildModelBuffer() {
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    this.modelBuffer = new ModelBuffer(lx);
    this.updateModelBuffer = false;
  }

  private void buildColorBuffer() {
    if (this.colorBuffer != null) {
      this.colorBuffer.dispose();
    }
    this.colorBuffer = new DynamicVertexBuffer(lx, this.model.size * ModelBuffer.VERTICES_PER_POINT, VertexDeclaration.ATTRIB_COLOR0);
  }

  @Override
  public void onDraw(UI ui, View view) {
    LXEngine.Frame frame = this.lx.uiFrame;
    setModel(frame.getModel());

    // Empty model? Don't do anything.
    if (this.model.size == 0) {
      return;
    }

    // Is our buffer model out of date? Rebuild it if so...
    if (this.bufferModel != this.model) {
      buildModelBuffer();
      // Only need to rebuild the color buffer if its size has changed
      if (this.bufferModel == null || (this.bufferModel.size != this.model.size)) {
        buildColorBuffer();
      }
      // This is now the model our buffers are based upon
      this.bufferModel = this.model;
    }

    // Rebuild the model buffer if points in the model have moved
    if (this.updateModelBuffer) {
      buildModelBuffer();
    }

    // Update the color data
    ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();
    for (int c : frame.getColors()) {
      for (int i = 0; i < ModelBuffer.VERTICES_PER_POINT; ++i) {
        colorData.putInt(c);
      }
    }
    colorData.flip();
    this.colorBuffer.update();

    // Submit our drawing program!
    this.program.submit(view);
  }

}
