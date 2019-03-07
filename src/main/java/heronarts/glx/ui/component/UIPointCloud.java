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
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BoundedParameter;

public class UIPointCloud extends UI3dComponent {

  public final BoundedParameter pointSize =
    new BoundedParameter("Point Size", 5, 0, 100)
    .setDescription("Size of points rendered in the preview display");

  private Program program;
  private VertexBuffer vertexBuffer;
  private DynamicVertexBuffer colorBuffer;
  private Texture texture;

  private static final int NUM_LEDS = 500;

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
      bgfx_set_vertex_buffer(0, vertexBuffer.getHandle(), 0, vertexBuffer.getNumVertices());
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

  public UIPointCloud(GLX lx) {
    this.program = new Program(lx);
    this.texture = new Texture("led.ktx");
    this.colorBuffer = new DynamicVertexBuffer(lx, NUM_LEDS * 6, VertexDeclaration.ATTRIB_COLOR0);
    this.vertexBuffer = new VertexBuffer(lx, NUM_LEDS * 6, VertexDeclaration.ATTRIB_POSITION | VertexDeclaration.ATTRIB_TEXCOORD0) {

      @Override
      protected void bufferData(ByteBuffer buffer) {
        for (int i = 0; i < NUM_LEDS; ++i) {
          float x = (float) Math.random() * 200 - 100;
          float y = (float) Math.random() * 200 - 100;
          float z = 0;

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(0f);
          buffer.putFloat(0f);

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(1f);
          buffer.putFloat(0f);

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(0f);
          buffer.putFloat(1f);

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(0f);
          buffer.putFloat(1f);

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(1f);
          buffer.putFloat(0f);

          buffer.putFloat(x);
          buffer.putFloat(y);
          buffer.putFloat(z);
          buffer.putFloat(1f);
          buffer.putFloat(1f);
        }
      }
    };

  }

  public void dispose() {
    this.texture.dispose();
    this.vertexBuffer.dispose();
    this.colorBuffer.dispose();
    this.program.dispose();
  }

  @Override
  public void onDraw(UI ui, View view) {
    ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();
    int c = LXColor.hsb(System.currentTimeMillis() / 100 % 360., 100, 100);
    for (int i = 0; i < this.colorBuffer.getNumVertices(); ++i) {
      colorData.putInt(c);
    }
    colorData.flip();
    this.colorBuffer.update();
    this.program.submit(view);
  }
}
