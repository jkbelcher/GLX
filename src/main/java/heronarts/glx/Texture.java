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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.bgfx.BGFX.*;

public class Texture {

  private final short th;
  private final ByteBuffer textureData;

  public Texture(String path) {
    try {
      this.textureData = GLXUtils.loadResource("textures/" + path);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
    this.th = bgfx_create_texture(bgfx_make_ref(this.textureData), BGFX_TEXTURE_NONE, 0, null);
  }

  public short getHandle() {
    return this.th;
  }

  public void dispose() {
    bgfx_destroy_texture(this.th);
    MemoryUtil.memFree(this.textureData);
  }
}
