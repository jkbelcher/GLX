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

import static org.lwjgl.bgfx.BGFX.BGFX_ATTRIB_COLOR0;
import static org.lwjgl.bgfx.BGFX.BGFX_ATTRIB_POSITION;
import static org.lwjgl.bgfx.BGFX.BGFX_ATTRIB_TEXCOORD0;
import static org.lwjgl.bgfx.BGFX.BGFX_ATTRIB_TYPE_FLOAT;
import static org.lwjgl.bgfx.BGFX.BGFX_ATTRIB_TYPE_UINT8;
import static org.lwjgl.bgfx.BGFX.bgfx_vertex_decl_add;
import static org.lwjgl.bgfx.BGFX.bgfx_vertex_decl_begin;
import static org.lwjgl.bgfx.BGFX.bgfx_vertex_decl_end;

import org.lwjgl.bgfx.BGFXVertexDecl;

public class VertexDeclaration {

  public static int ATTRIB_POSITION = 1 << 0;
  public static int ATTRIB_COLOR0 = 1 << 1;
  public static int ATTRIB_TEXCOORD0 = 1 << 2;

  private final BGFXVertexDecl handle;
  private int stride = 0;

  public VertexDeclaration(GLX glx, int attributes) {
    this.handle = BGFXVertexDecl.calloc();
    bgfx_vertex_decl_begin(this.handle, glx.getRenderer());
    if ((attributes & ATTRIB_POSITION) != 0) {
      bgfx_vertex_decl_add(this.handle, BGFX_ATTRIB_POSITION, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
      this.stride += 3 * Float.BYTES;
    }
    if ((attributes & ATTRIB_COLOR0) != 0) {
      bgfx_vertex_decl_add(this.handle, BGFX_ATTRIB_COLOR0, 4, BGFX_ATTRIB_TYPE_UINT8, true, false);
      this.stride += 4;
    }
    if ((attributes & ATTRIB_TEXCOORD0) != 0) {
      bgfx_vertex_decl_add(this.handle, BGFX_ATTRIB_TEXCOORD0, 2, BGFX_ATTRIB_TYPE_FLOAT, false, false);
      this.stride += 2 * Float.BYTES;
    }
    bgfx_vertex_decl_end(this.handle);
  }

  public BGFXVertexDecl getHandle() {
    return this.handle;
  }

  public void dispose() {
    this.handle.free();
  }

  public int getStride() {
    return this.stride;
  }
}
