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

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

public class DynamicVertexBuffer {

  public static int ATTRIB_POSITION = 1 << 0;
  public static int ATTRIB_COLOR0 = 1 << 1;
  public static int ATTRIB_TEXCOORD0 = 1 << 2;

  private final VertexDeclaration vertexDeclaration;
  private final ByteBuffer vertexData;

  private final short vbh;
  private final int numVertices;

  public DynamicVertexBuffer(GLX glx, int numVertices) {
    this(glx, numVertices, ATTRIB_POSITION | ATTRIB_COLOR0);
  }

  public DynamicVertexBuffer(GLX glx, int numVertices, int attributes) {
    this.vertexDeclaration = new VertexDeclaration(glx, attributes);
    this.vertexData = MemoryUtil.memAlloc(this.vertexDeclaration.getStride() * numVertices);
    this.vbh = bgfx_create_dynamic_vertex_buffer(numVertices, this.vertexDeclaration.getHandle(), BGFX_BUFFER_NONE);
    this.numVertices = numVertices;
  }

  public short getHandle() {
    return this.vbh;
  }

  public int getNumVertices() {
    return this.numVertices;
  }

  public ByteBuffer getVertexData() {
    return this.vertexData;
  }

  public void update() {
    bgfx_update_dynamic_vertex_buffer(this.vbh, 0, bgfx_make_ref(this.vertexData));
  }

  public void dispose() {
    bgfx_destroy_dynamic_vertex_buffer(this.vbh);
    MemoryUtil.memFree(this.vertexData);
    this.vertexDeclaration.dispose();
  }
}
