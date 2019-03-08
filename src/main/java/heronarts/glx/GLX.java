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

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.*;


import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.bgfx.BGFXPlatform.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.bgfx.BGFXPlatformData;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXEngine;
import heronarts.lx.model.LXModel;

public class GLX extends LX {

  private long window;

  private int windowWidth = 1280;
  private int windowHeight = 720;
  private int frameBufferWidth = 0;
  private int frameBufferHeight = 0;
  float xContentScale = 1;
  float yContentScale = 1;

  private int bgfxRenderer = BGFX_RENDERER_TYPE_COUNT;
  private int bgfxFormat = 0;

  public final VGraphics vg;

  public final boolean zZeroToOne;

  private final InputDispatch inputDispatch = new InputDispatch(this);

  public final UI ui;
  public final LXEngine.Frame uiFrame;

  public final class Programs {

    public final Tex2d tex2d;

    public Programs(GLX glx) {
      this.tex2d = new Tex2d(glx);
    }

    public void dispose() {
      this.tex2d.dispose();
    }

  }

  protected final Programs program;

  protected GLX(Flags flags) throws IOException {
    this(flags, null);
  }

  protected GLX(Flags flags, LXModel model) throws IOException {
    super(flags, model);

    initializeWindow();
    this.zZeroToOne = !bgfx_get_caps().homogeneousDepth();

    // Initialize global shader programs and VG library
    this.program = new Programs(this);
    this.vg = new VGraphics(this);

    // Initialize LED frame buffer for the UI
    this.uiFrame = new LXEngine.Frame(this);
    this.engine.getFrameNonThreadSafe(this.uiFrame);

    // Create the UI system
    this.ui = buildUI();
  }

  public void run() {

    // Start the LX engine thread
    System.out.println("Starting LX Engine");
    this.engine.setInputDispatch(this.inputDispatch);
    this.engine.start();

    // Enter the core rendering loop
    loop();

    // Stop the LX engine
    System.out.println("Stopping LX engine");
    this.engine.stop();

    // Clean up after ourselves
    dispose();

    // Shut down bgfx
    bgfx_shutdown();

    // Free the window callbacks and destroy the window
    glfwFreeCallbacks(this.window);
    glfwDestroyWindow(this.window);

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();

    // The program *should* end now, if not it means we hung a thread somewhere...
    System.out.println("Done with the main thread...");

  }

  /**
   * Subclasses may override to create a custom structured UI
   * @throws IOException
   */
  protected UI buildUI() throws IOException {
    return new UI(this);
  }

  public int getRenderer() {
    return this.bgfxRenderer;
  }

  public int getWindowWidth() {
    return this.windowWidth;
  }

  public int getWindowHeight() {
    return this.windowHeight;
  }

  public int getFrameBufferWidth() {
    return this.frameBufferWidth;
  }

  public int getFrameBufferHeight() {
    return this.frameBufferHeight;
  }

  private void initializeWindow() {
    GLFWErrorCallback.createPrint(System.err).set();

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit()) {
      throw new RuntimeException("Unable to initialize GLFW");
    }

    // Configure GLFW
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);

    // Create GLFW window
    this.window = glfwCreateWindow(
      this.windowWidth,
      this.windowHeight,
      "LXStudio",
      NULL,
      NULL
    );
    if (this.window == NULL) {
      throw new RuntimeException("Failed to create the GLFW window");
    }

    // Detect window/framebuffer sizes and content scale
    try (MemoryStack stack = MemoryStack.stackPush()) {
      FloatBuffer xScale = stack.mallocFloat(1);
      FloatBuffer yScale = stack.mallocFloat(1);
      glfwGetWindowContentScale(this.window, xScale, yScale);
      this.xContentScale = xScale.get(0);
      this.yContentScale = yScale.get(0);

      IntBuffer xSize = stack.mallocInt(1);
      IntBuffer ySize = stack.mallocInt(1);
      glfwGetWindowSize(this.window, xSize, ySize);
      this.windowWidth = xSize.get(0);
      this.windowHeight = ySize.get(0);

      glfwGetFramebufferSize(this.window, xSize, ySize);
      this.frameBufferWidth = xSize.get(0);
      this.frameBufferHeight = ySize.get(0);
    }

    glfwSetWindowCloseCallback(this.window, (window) -> {
      System.out.println("Trying to close window...");
      // TODO(mcslee): check with LX for needing a save work prompt
    });

    glfwSetWindowSizeCallback(this.window, (window, width, height) -> {
      this.windowWidth = width;
      this.windowHeight = height;
      bgfx_reset(this.windowWidth, this.windowHeight, BGFX_RESET_VSYNC, this.bgfxFormat);
      ui.resize();
      draw();
    });

    glfwSetFramebufferSizeCallback(this.window, (window, width, height) -> {
      this.frameBufferWidth = width;
      this.frameBufferHeight = height;
      // TODO(mcslee): should we redraw here? seems redundant...
    });

    // Register input dispatching callbacks
    glfwSetKeyCallback(this.window, this.inputDispatch::glfwKeyCallback);
    glfwSetCharCallback(this.window, this.inputDispatch::glfwCharCallback);
    glfwSetCursorPosCallback(this.window, this.inputDispatch::glfwCursorPosCallback);
    glfwSetMouseButtonCallback(this.window, this.inputDispatch::glfwMouseButtonCallback);
    glfwSetScrollCallback(window, this.inputDispatch::glfwScrollCallback);

    // Initialize BGFX platform data
    initializePlatformData();

    // Construct the BGFX instance
    try (MemoryStack stack = MemoryStack.stackPush()) {
      BGFXInit init = BGFXInit.mallocStack(stack);
      bgfx_init_ctor(init);
      init
        .type(this.bgfxRenderer)
        .vendorId(BGFX_PCI_ID_NONE)
        .deviceId((short) 0)
        .callback(null)
        .allocator(null)
        .resolution(res -> res.width(this.windowWidth).height(this.windowHeight).reset(BGFX_RESET_VSYNC));
      if (!bgfx_init(init)) {
        throw new RuntimeException("Error initializing bgfx renderer");
      }
      this.bgfxFormat = init.resolution().format();
      if (this.bgfxRenderer == BGFX_RENDERER_TYPE_COUNT) {
        this.bgfxRenderer = bgfx_get_renderer_type();
      }
    }
    String rendererName = bgfx_get_renderer_name(this.bgfxRenderer);
    if ("NULL".equals(rendererName)) {
      throw new RuntimeException("Error identifying bgfx renderer");
    }
    System.out.println("Using BGFX renderer: " + rendererName);

    // bgfx_set_debug(BGFX_DEBUG_TEXT);
  }

  protected void setWindowSize(int windowWidth, int windowHeight) {
    glfwSetWindowSize(this.window, windowWidth, windowHeight);
  }

  private void initializePlatformData() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      BGFXPlatformData platformData = BGFXPlatformData.callocStack(stack);
      switch (Platform.get()) {
      case LINUX:
        platformData.ndt(GLFWNativeX11.glfwGetX11Display());
        platformData.nwh(GLFWNativeX11.glfwGetX11Window(this.window));
        break;
      case MACOSX:
        platformData.ndt(NULL);
        platformData.nwh(GLFWNativeCocoa.glfwGetCocoaWindow(this.window));
        break;
      case WINDOWS:
        platformData.ndt(NULL);
        platformData.nwh(GLFWNativeWin32.glfwGetWin32Window(this.window));
        break;
      }
      platformData.context(NULL);
      platformData.backBuffer(NULL);
      platformData.backBufferDS(NULL);
      bgfx_set_platform_data(platformData);
    }
  }

  private void loop() {
    while (!glfwWindowShouldClose(this.window)) {
      // Poll for input events
      this.inputDispatch.poll();

      draw();

      // Copy something to the clipboard
      if (this._setSystemClipboardString != null) {
        glfwSetClipboardString(this.window, this._setSystemClipboardString);
        this._setSystemClipboardString = null;
      }
    }
  }

  private void draw() {
    // Copy the latest engine-rendered LED frame
    this.engine.copyFrameThreadSafe(this.uiFrame);
    this.ui.draw();
    bgfx_frame(false);
  }

  @Override
  public void dispose() {
    this.program.dispose();
    super.dispose();
  }

  // Prevent stacking up multiple dialogs
  private volatile boolean dialogShowing = new Boolean(false);

  public void showSaveDialog() {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(1);
          aFilterPatterns.put(stack.UTF8("*.lxp"));
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_saveFileDialog(
            "Save Project",
            mediaPath() + File.separator + "default.lxp",
            aFilterPatterns,
            "LX Project files (*.lxp)"
         );
         dialogShowing = false;
         if (path != null) {
           engine.addTask(() -> {
             saveProject(new File(path));
           });
         }
        }
      }
    }.start();
  }

  public void showOpenDialog() {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(1);
          aFilterPatterns.put(stack.UTF8("*.lxp"));
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_openFileDialog(
            "Open Project",
            mediaPath(),
            aFilterPatterns,
            "LX Project files (*.lxp)",
            false
          );
          dialogShowing = false;
          if (path != null) {
            engine.addTask(() -> {
              openProject(new File(path));
            });
          }
        }
      }
    }.start();
  }

  public String mediaPath() {
    // TODO(mcslee): this should be a sane documents folder
    return ".";
  }

  public File saveFile(String path) {
    // TODO(mcslee): should be in ~/Documents/LXStudio/ or something
    return new File(path);
  }

  private String _setSystemClipboardString = null;

  @Override
  public void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

}
