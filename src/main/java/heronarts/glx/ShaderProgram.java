package heronarts.glx;

import static org.lwjgl.bgfx.BGFX.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public class ShaderProgram {
  private short handle;
  private ByteBuffer vertexShaderCode;
  private ByteBuffer fragmentShaderCode;

  public ShaderProgram(GLX glx, String vsName, String fsName) {
    try {
      this.vertexShaderCode = GLXUtils.loadShader(glx, vsName);
      this.fragmentShaderCode = GLXUtils.loadShader(glx, fsName);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
    this.handle = bgfx_create_program(
      bgfx_create_shader(bgfx_make_ref(this.vertexShaderCode)),
      bgfx_create_shader(bgfx_make_ref(this.fragmentShaderCode)),
      true
    );
  }

  public void submit(View view) {
    bgfx_set_state(BGFX_STATE_WRITE_RGB | BGFX_STATE_WRITE_A | BGFX_STATE_WRITE_Z | BGFX_STATE_BLEND_ALPHA, 0);
    setUniforms(view);
    setVertexBuffers(view);
    bgfx_submit(view.getId(), this.handle, 0, false);
  }

  protected void setVertexBuffers(View view) {
    // Subclasses override to set vertex buffers
  }

  protected void setUniforms(View view) {
    // Subclasses override to set textures and uniforms
  }

  public void dispose() {
    bgfx_destroy_program(this.handle);
    MemoryUtil.memFree(this.vertexShaderCode);
    MemoryUtil.memFree(this.fragmentShaderCode);
  }
}
