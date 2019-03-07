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

public abstract class VertexBuffer {

  private final VertexDeclaration vertexDeclaration;
  private final ByteBuffer vertexData;
  private final short vbh;
  private final int numVertices;

  public VertexBuffer(GLX glx, int numVertices) {
    this(glx, numVertices, VertexDeclaration.ATTRIB_POSITION | VertexDeclaration.ATTRIB_TEXCOORD0);
  }

  public VertexBuffer(GLX glx, int numVertices, int attributes) {
    this.vertexDeclaration = new VertexDeclaration(glx, attributes);
    this.vertexData = MemoryUtil.memAlloc(this.vertexDeclaration.getStride() * numVertices);
    bufferData(this.vertexData);
    this.vertexData.flip();
    this.vbh = bgfx_create_vertex_buffer(bgfx_make_ref(this.vertexData), this.vertexDeclaration.getHandle(), BGFX_BUFFER_NONE);
    this.numVertices = numVertices;
  }

  protected abstract void bufferData(ByteBuffer buffer);

  public short getHandle() {
    return this.vbh;
  }

  public int getNumVertices() {
    return this.numVertices;
  }

  public void dispose() {
    bgfx_destroy_vertex_buffer(this.vbh);
    MemoryUtil.memFree(this.vertexData);
    this.vertexDeclaration.dispose();
  }
}
