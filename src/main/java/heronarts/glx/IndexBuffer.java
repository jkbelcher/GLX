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

public abstract class IndexBuffer {

  private final ByteBuffer indexData;
  private final short indexBufferHandle;
  private final int numIndices;

  public IndexBuffer(GLX glx, int numIndices) {
    this.indexData = MemoryUtil.memAlloc(Short.BYTES * numIndices);
    bufferData(this.indexData);
    this.indexData.flip();
    this.indexBufferHandle = bgfx_create_index_buffer(bgfx_make_ref(this.indexData), BGFX_BUFFER_NONE);
    this.numIndices = numIndices;
  }

  protected abstract void bufferData(ByteBuffer buffer);

  public short getHandle() {
    return this.indexBufferHandle;
  }

  public int getNumIndices() {
    return this.numIndices;
  }

  public ByteBuffer getIndexData() {
    return this.indexData;
  }

  public void dispose() {
    bgfx_destroy_index_buffer(this.indexBufferHandle);
    MemoryUtil.memFree(this.indexData);
  }
}
