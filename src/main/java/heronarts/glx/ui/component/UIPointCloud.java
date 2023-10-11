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

import com.google.gson.JsonObject;

import heronarts.glx.DynamicVertexBuffer;
import heronarts.glx.GLX;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.shader.ShaderProgram;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXEngine;
import heronarts.lx.LXSerializable;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.EnumParameter;

public class UIPointCloud extends UI3dComponent implements LXSerializable {

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
      bgfx_set_texture(0, this.uniformTexture, textures[ledStyle.getValuei()].getHandle(), BGFX_SAMPLER_NONE);
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
        putVertex(p.x, p.y, p.z);
        putTex2d(0f, 0f);

        putVertex(p.x, p.y, p.z);
        putTex2d(1f, 0f);

        putVertex(p.x, p.y, p.z);
        putTex2d(0f, 1f);

        putVertex(p.x, p.y, p.z);
        putTex2d(0f, 1f);

        putVertex(p.x, p.y, p.z);
        putTex2d(1f, 0f);

        putVertex(p.x, p.y, p.z);
        putTex2d(1f, 1f);
      }
    }
  }

  public enum LedStyle {

    LENS1("Lens 1", "led.ktx"),
    LENS2("Lens 2", "led2.ktx"),
    LENS3("Lens 3", "led3.ktx"),
    CIRCLE("Circle", "led4.ktx"),
    SQUARE("Square", "led5.ktx");

    public final String label;
    public final String texture;

    private LedStyle(String label, String texture) {
      this.label = label;
      this.texture = texture;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final BoundedParameter pointSize =
    new BoundedParameter("Point Size", 3, .1, 100000)
    .setDescription("Size of points rendered in the preview display");

  public final BooleanParameter depthTest =
    new BooleanParameter("Depth Test", true)
    .setDescription("Whether to use depth test in rendering");

  public final EnumParameter<LedStyle> ledStyle =
    new EnumParameter<LedStyle>("LED Style", LedStyle.LENS1)
    .setDescription("Which LED texture to render");

  private final GLX lx;

  private final Program program;
  private final Texture[] textures = new Texture[LedStyle.values().length];

  private VertexBuffer modelBuffer;
  private DynamicVertexBuffer colorBuffer;

  // This is the model that our current vertex buffers (UI thread) is based upon,
  // which could be a frame behind the engine!
  private LXModel model = null;

  private int modelGeneration = -1;

  private boolean auxiliary = false;

  public UIPointCloud(GLX lx) {
    this.lx = lx;
    this.program = new Program(lx);
    int ti = 0;
    for (LedStyle ledStyle : LedStyle.values()) {
      this.textures[ti++] = new Texture(ledStyle.texture);
    };
    this.colorBuffer = null;
    this.modelBuffer = null;
  }

  public UIPointCloud setAuxiliary(boolean auxiliary) {
    this.auxiliary = auxiliary;
    return this;
  }

  @Override
  public void dispose() {
    for (Texture texture : this.textures) {
      texture.dispose();
    }
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    if (this.colorBuffer != null) {
      this.colorBuffer.dispose();
    }
    this.program.dispose();
  }

  private void buildModelBuffer() {
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    this.modelBuffer = new ModelBuffer(lx);
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
    LXModel frameModel = frame.getModel();
    int frameModelGeneration = frameModel.getGeneration();

    // Empty model? Don't do anything.
    if (frameModel.size == 0) {
      return;
    }

    // Is our buffer model out of date? Rebuild it if so...
    if (this.model != frameModel) {
      LXModel oldModel = this.model;
      this.model = frameModel;
      this.modelGeneration = frameModelGeneration;
      buildModelBuffer();
      if ((this.colorBuffer == null) || (oldModel == null) || (oldModel.size != frameModel.size)) {
        buildColorBuffer();
      }
    } else if (this.modelGeneration != frameModelGeneration) {
      // Model geometry (but not size) has changed, rebuild model buffer
      buildModelBuffer();
      this.modelGeneration = frameModelGeneration;
    }

    // Update the color data
    ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();
    for (int c : this.auxiliary ? frame.getAuxColors() : frame.getColors()) {
      for (int i = 0; i < ModelBuffer.VERTICES_PER_POINT; ++i) {
        colorData.putInt(c);
      }
    }
    colorData.flip();
    this.colorBuffer.update();

    // Submit our drawing program!
    long state =
      BGFX_STATE_WRITE_RGB |
      BGFX_STATE_WRITE_Z |
      BGFX_STATE_BLEND_ALPHA |
      BGFX_STATE_ALPHA_REF(32);
    if (this.depthTest.isOn()) {
      state |= BGFX_STATE_DEPTH_TEST_LESS;
    }
    this.program.submit(view, state);
  }

  private static final String KEY_POINT_SIZE = "pointSize";
  private static final String KEY_DEPTH_TEST = "depthTest";
  private static final String KEY_LED_STYLE = "ledStyle";

  @Override
  public void save(LX lx, JsonObject object) {
    object.addProperty(KEY_POINT_SIZE, this.pointSize.getValue());
    object.addProperty(KEY_DEPTH_TEST, this.depthTest.isOn());
    object.addProperty(KEY_LED_STYLE, this.ledStyle.getValuei());
  }

  @Override
  public void load(LX lx, JsonObject object) {
    if (object.has(LXComponent.KEY_RESET)) {
      this.pointSize.reset();
      this.depthTest.reset();
      this.ledStyle.reset();
    } else {
      LXSerializable.Utils.loadDouble(this.pointSize, object, KEY_POINT_SIZE);
      LXSerializable.Utils.loadBoolean(this.depthTest, object, KEY_DEPTH_TEST);
      LXSerializable.Utils.loadInt(this.ledStyle, object, KEY_LED_STYLE);
    }
  }

}
