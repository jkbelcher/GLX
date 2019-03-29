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
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.bgfx.BGFXPlatform.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.bgfx.BGFXPlatformData;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import heronarts.glx.shader.Shape;
import heronarts.glx.shader.Tex2d;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIDialogBox;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXEngine;
import heronarts.lx.model.LXModel;

public class GLX extends LX {

  private long window;

  private long handCursor;
  private long useCursor = 0;
  private boolean needsCursorUpdate = false;

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
    public final Shape shape;

    public Programs(GLX glx) {
      this.tex2d = new Tex2d(glx);
      this.shape = new Shape(glx);
    }

    public void dispose() {
      this.tex2d.dispose();
      this.shape.dispose();
    }
  }

  public final Programs program;

  protected GLX(Flags flags) throws IOException {
    this(flags, null);
  }

  protected GLX(Flags flags, LXModel model) throws IOException {
    super(flags, model);

    // Get initial window size from preferences
    int preferenceWidth = this.preferences.getWindowWidth();
    int preferenceHeight = this.preferences.getWindowHeight();
    if (preferenceWidth > 0 && preferenceHeight > 0) {
      this.windowWidth = preferenceWidth;
      this.windowHeight = preferenceHeight;
    }

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

    // TODO(mcslee): join the LX engine thread? make sure it's really done?

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
   *
   * @throws IOException if required UI assets could not be loaded
   * @return The instantiated UI object
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

  public float getContentScale() {
    return this.xContentScale;
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
      "LX Studio",
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

    glfwSetWindowFocusCallback(this.window, (window, focused) -> {
      if (focused) {
        // Update the cursor position callback... if the window wasn't focused
        // and the user re-focused it with a click followed by mouse drag, then
        // the CursorPosCallback won't have had a chance to fire yet. So
        // we give it a kick whenever the window refocuses.
        try (MemoryStack stack = MemoryStack.stackPush()) {
          DoubleBuffer xPos = stack.mallocDouble(1);
          DoubleBuffer yPos = stack.mallocDouble(1);
          glfwGetCursorPos(this.window, xPos, yPos);
          this.inputDispatch.onFocus(xPos.get(0), yPos.get(0));
        }
      }
    });

    glfwSetWindowCloseCallback(this.window, (window) -> {
      glfwSetWindowShouldClose(this.window, false);
      confirmChangesSaved("quit", () -> {
        glfwSetWindowShouldClose(this.window, true);
      });
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
    });

    glfwSetDropCallback(this.window, (window, count, names) -> {
      if (count == 1) {
        try {
          final File file = new File(GLFWDropCallback.getName(names, 0));
          if (file.exists() && file.isFile()) {
            if (file.getName().endsWith(".lxp")) {
              confirmChangesSaved("open project " + file.getName(), () -> {
                openProject(file);
              });
            } else if (file.getName().endsWith(".jar")) {
              final File destination = new File(getMediaFolder(LX.Media.CONTENT), file.getName());
              if (destination.exists()) {
                showConfirmDialog(file.getName() + " already exists in content folder, overwrite?", () -> { importContentJar(file, destination); });
              } else {
                importContentJar(file, destination);
              }
            }
          }
        } catch (Exception x) {
          x.printStackTrace();
        }
      }
    });

    // Register input dispatching callbacks
    glfwSetKeyCallback(this.window, this.inputDispatch::glfwKeyCallback);
    glfwSetCharCallback(this.window, this.inputDispatch::glfwCharCallback);
    glfwSetCursorPosCallback(this.window, this.inputDispatch::glfwCursorPosCallback);
    glfwSetMouseButtonCallback(this.window, this.inputDispatch::glfwMouseButtonCallback);
    glfwSetScrollCallback(window, this.inputDispatch::glfwScrollCallback);

    // Create hand editing cursor
    this.handCursor = glfwCreateStandardCursor(GLFW_HAND_CURSOR);

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

      if (this.needsCursorUpdate) {
        glfwSetCursor(this.window, this.useCursor);
        this.needsCursorUpdate = false;
      }

      draw();

      // Copy something to the clipboard
      String copyToClipboard = this._setSystemClipboardString;
      if (copyToClipboard != null) {
        glfwSetClipboardString(this.window, copyToClipboard);
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
    glfwDestroyCursor(this.handCursor);
    this.program.dispose();
    super.dispose();
  }

  public void useHandCursor(boolean useHandCursor) {
    this.useCursor = useHandCursor ? this.handCursor : 0;
    this.needsCursorUpdate = true;
  }

  protected void importContentJar(File file, File destination) {
    System.out.println("Importing content JAR: " + destination.toString());
    try {
      Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      this.engine.addTask(() -> {
        reloadContent();
        this.ui.contextualHelpText.setValue("New content imported into " + destination.getName());
      });
    } catch (IOException e) {
      System.err.println("Error copying " + file.getName() + " to content directory");
      e.printStackTrace();
    }
  }

  @Override
  public void reloadContent() {
    super.reloadContent();
    this.ui.contextualHelpText.setValue("External content libraries reloaded");
  }

  // Prevent stacking up multiple dialogs
  private volatile boolean dialogShowing = false;

  public void showSaveProjectDialog() {
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
            getMediaFolder(LX.Media.PROJECTS).toString() + File.separator + "default.lxp",
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

  public void showOpenProjectDialog() {
    if (this.dialogShowing) {
      return;
    }
    confirmChangesSaved("open another project", () -> {
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
              new File(getMediaFolder(LX.Media.PROJECTS), ".").toString(),
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
    });
  }

  public void showOpenAudioDialog() {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(2);
          aFilterPatterns.put(stack.UTF8("*.wav"));
          aFilterPatterns.put(stack.UTF8("*.aiff"));
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_openFileDialog(
            "Open Audio File",
            new File(getMediaPath(), ".").toString(),
            aFilterPatterns,
            "Audio files (*.wav/aiff)",
            false
          );
          dialogShowing = false;
          if (path != null) {
            engine.addTask(() -> {
              engine.audio.output.file.setValue(path);
            });
          }
        }
      }
    }.start();
  }

  public void showExportModelDialog() {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(1);
          aFilterPatterns.put(stack.UTF8("*.lxm"));
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_saveFileDialog(
            "Export Model",
            getMediaFolder(LX.Media.MODELS).toString() + File.separator + "Model.lxm",
            aFilterPatterns,
            "LX Model files (*.lxm)"
         );
         dialogShowing = false;
         if (path != null) {
           engine.addTask(() -> {
             structure.exportModel(new File(path));
           });
         }
        }
      }
    }.start();
  }

  public void showImportModelDialog() {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(1);
          aFilterPatterns.put(stack.UTF8("*.lxm"));
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_openFileDialog(
            "Import Model",
            new File(getMediaFolder(LX.Media.MODELS), ".").toString(),
            aFilterPatterns,
            "LX Model files (*.lxm)",
            false
          );
          dialogShowing = false;
          if (path != null) {
            engine.addTask(() -> {
              structure.importModel(new File(path));
            });
          }
        }
      }
    }.start();

  }

  @Override
  protected void showConfirmUnsavedProjectDialog(String message, Runnable confirm) {
    showConfirmDialog(
      "Your project has unsaved changes, really " + message + "?",
      confirm
    );
  }

  protected void showConfirmDialog(String message, Runnable confirm) {
    this.ui.showContextOverlay(new UIDialogBox(this.ui,
      message,
      new String[] { "No", "Yes" },
      new Runnable[] { null, confirm }
    ));
  }

  private String _setSystemClipboardString = null;

  @Override
  public void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

}
