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

public class DynamicIndexBuffer {

  private final ByteBuffer indexData;
  private final short indexBufferHandle;
  private final int numIndices;

  public DynamicIndexBuffer(GLX glx, int numIndices) {
    this(glx, numIndices, false);
  }

  public DynamicIndexBuffer(GLX glx, int numIndices, boolean int32) {
    this.indexData = MemoryUtil.memAlloc((int32 ? Integer.BYTES : Short.BYTES) * numIndices);
    this.indexBufferHandle = bgfx_create_dynamic_index_buffer(numIndices, int32 ? BGFX_BUFFER_INDEX32 : BGFX_BUFFER_NONE);
    this.numIndices = numIndices;
  }

  public short getHandle() {
    return this.indexBufferHandle;
  }

  public int getNumIndices() {
    return this.numIndices;
  }

  public ByteBuffer getIndexData() {
    return this.indexData;
  }

  public void update() {
    bgfx_update_dynamic_index_buffer(this.indexBufferHandle, 0, bgfx_make_ref(this.indexData));
  }

  public void dispose() {
    bgfx_destroy_dynamic_index_buffer(this.indexBufferHandle);
    MemoryUtil.memFree(this.indexData);
  }
}
