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
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.bgfx.BGFX.*;

public class GLXUtils {

  private GLXUtils() {}

  public static ByteBuffer loadShader(GLX glx, String name) throws IOException {
    String path = "resources/shaders/";
    switch (glx.getRenderer()) {
    case BGFX_RENDERER_TYPE_DIRECT3D11:
    case BGFX_RENDERER_TYPE_DIRECT3D12:
      path += "dx11/";
      break;
    case BGFX_RENDERER_TYPE_DIRECT3D9:
      path += "dx9/";
      break;
    case BGFX_RENDERER_TYPE_OPENGL:
      path += "glsl/";
      break;
    case BGFX_RENDERER_TYPE_METAL:
      path += "metal/";
      break;
    default:
      throw new IOException("No shaders supported for " + bgfx_get_renderer_name(glx.getRenderer()) + " renderer");
    }
    return loadResource(path + name + ".bin");
  }

  /**
   * Loads the resource at the given path into a newly allocated buffer. The buffer is owned by
   * the caller and must be freed explicitly.
   *
   * @param path Path to the resource
   * @return Buffer allocated by MemoryUtil
   * @throws IOException If there is an error loading the resource
   */
  public static ByteBuffer loadResource(String path) throws IOException {
    ByteBuffer resource = null;
    Path file = Paths.get(path);
    if (Files.isReadable(file)) {
      try (
        SeekableByteChannel fc = Files.newByteChannel(file);
      ) {
        resource = MemoryUtil.memAlloc((int) fc.size() + 1);
        while (fc.read(resource) != -1);
        resource.flip();
        return resource;
      } catch (IOException iox) {
        MemoryUtil.memFree(resource);
        throw iox;
      }
    }

    URL url = GLXUtils.class.getResource(path);
    if (url == null) {
      throw new IOException("Resource not found: " + path);
    }
    int resourceSize = url.openConnection().getContentLength();
    resource = MemoryUtil.memAlloc(resourceSize);
    try (
      InputStream stream = url.openStream();
      ReadableByteChannel rbc = Channels.newChannel(stream);
    ) {
      while (rbc.read(resource) != -1);
      resource.flip();
      return resource;
    } catch(IOException iox) {
      MemoryUtil.memFree(resource);
      throw iox;
    }
  }
}
