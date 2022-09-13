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
import org.lwjgl.system.macosx.ObjCRuntime;

import heronarts.glx.shader.Shape;
import heronarts.glx.shader.Tex2d;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIDialogBox;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXEngine;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.utils.LXUtils;

public class GLX extends LX {

  private static final int MIN_WINDOW_WIDTH = 820;
  private static final int MIN_WINDOW_HEIGHT = 480;

  private long window;

  private long handCursor;
  private long useCursor = 0;
  private boolean needsCursorUpdate = false;

  private int displayWidth = -1;
  private int displayHeight = -1;
  private int windowWidth = 1280;
  private int windowHeight = 720;
  private int frameBufferWidth = 0;
  private int frameBufferHeight = 0;
  private float uiWidth = 0;
  private float uiHeight = 0;

  float systemContentScaleX = 1;
  float systemContentScaleY = 1;

  float uiZoom = 1;

  float cursorScaleX = 1;
  float cursorScaleY = 1;

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

  public static class Flags extends LX.Flags {
    public String windowTitle = "GLX";
    public boolean useOpenGL = false;
  }

  public final Flags flags;

  protected GLX(Flags flags) throws IOException {
    super(flags);
    this.flags = flags;

    if (this.flags.useOpenGL) {
      this.bgfxRenderer = BGFX_RENDERER_TYPE_OPENGL;
    }

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
    log("Starting LX Engine...");
    this.engine.setInputDispatch(this.inputDispatch);
    this.engine.start();

    // Enter the core rendering loop
    log("Bootstrap complete, running main loop.");
    loop();

    // Stop the LX engine
    log("Stopping LX engine...");
    this.engine.stop();

    // TODO(mcslee): join the LX engine thread? make sure it's really
    // done before cleaning up the window assets? doesn't seem necessary

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
    log("Done with main thread, GLX shutdown complete. Thanks for playing. <3");
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

  public boolean isOpenGL() {
    return this.bgfxRenderer == BGFX_RENDERER_TYPE_OPENGL;
  }

  public float getUIWidth() {
    return this.uiWidth;
  }

  public float getUIHeight() {
    return this.uiHeight;
  }

  public int getFrameBufferWidth() {
    return this.frameBufferWidth;
  }

  public int getFrameBufferHeight() {
    return this.frameBufferHeight;
  }

  public float getUIZoom() {
    return this.uiZoom;
  }

  public float getUIContentScaleX() {
    return this.systemContentScaleX * this.uiZoom;
  }

  public float getUIContentScaleY() {
    return this.systemContentScaleY * this.uiZoom;
  }

  public float getSystemContentScaleX() {
    return this.systemContentScaleX;
  }

  public float getSystemContentScaleY() {
    return this.systemContentScaleY;
  }

  private void initializeWindow() {
    GLFWErrorCallback.createPrint(System.err).set();

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit()) {
      throw new RuntimeException("Unable to initialize GLFW");
    }

    // Grab uiZoom from preferences
    this.uiZoom = this.preferences.uiZoom.getValuef() / 100f;
    this.preferences.uiZoom.addListener((p) -> { setUIZoom(this.preferences.uiZoom.getValuef() / 100f); });

    // Configure GLFW
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);

    // Create GLFW window
    this.window = glfwCreateWindow(
      this.windowWidth,
      this.windowHeight,
      this.flags.windowTitle,
      NULL,
      NULL
    );
    if (this.window == NULL) {
      throw new RuntimeException("Failed to create the GLFW window");
    }

    // Detect window/framebuffer sizes and content scale
    try (MemoryStack stack = MemoryStack.stackPush()) {

      // NOTE: content scale is different across platforms. On a Retina Mac,
      // content scale will be 2x and the framebuffer will have dimensions
      // that are twice that of the window. On Windows, content-scaling is
      // a setting that might be 125%, 150%, etc. - we'll have to look at
      // the window and framebuffer sizes to figure this all out
      FloatBuffer xScale = stack.mallocFloat(1);
      FloatBuffer yScale = stack.mallocFloat(1);
      glfwGetWindowContentScale(this.window, xScale, yScale);
      this.systemContentScaleX = xScale.get(0);
      this.systemContentScaleY = yScale.get(0);

      // The window size is in terms of "OS window size" - best thought of
      // as an abstract setting which may or may not exactly correspond to
      // pixels (e.g. a Mac retina display may have 2x as many pixels)
      IntBuffer xSize = stack.mallocInt(1);
      IntBuffer ySize = stack.mallocInt(1);
      glfwGetWindowSize(this.window, xSize, ySize);
      this.windowWidth = xSize.get(0);
      this.windowHeight = ySize.get(0);

      // See what is in the framebuffer. A retina Mac probably supplies
      // 2x the dimensions on framebuffer relative to window.
      glfwGetFramebufferSize(this.window, xSize, ySize);
      this.frameBufferWidth = xSize.get(0);
      this.frameBufferHeight = ySize.get(0);

      long primaryMonitor = glfwGetPrimaryMonitor();
      if (primaryMonitor == NULL) {
        error("Running on a system with no monitor, is this intended?");
      } else {
        IntBuffer xPos = stack.mallocInt(1);
        IntBuffer yPos = stack.mallocInt(1);
        glfwGetMonitorWorkarea(primaryMonitor, xPos, yPos, xSize, ySize);
        this.displayWidth = xSize.get();
        this.displayHeight = ySize.get();
      }
      log("GLX display: " + this.displayWidth + "x" + this.displayHeight);

      // Okay, let's figure out how many "virtual pixels" the GLX UI should
      // be. Note that on a Mac with 2x retina display, contentScale will be
      // 2, but the framebuffer will have dimensions twice that of the window.
      // So we should end up with uiWidth/uiHeight matching the window.
      // But on Windows it's a different situation, if contentScale > 100%
      // then we're going to "scale down" our number of UI pixels and draw them
      // into a larger framebuffer.
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;

      // To make things even trickier... keep in mind that the OS specifies cursor
      // movement relative to its window size. We need to scale those onto our
      // virtual UI window size.
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;

      // Set UI Zoom bounds based upon content scaling
      this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.systemContentScaleX), 201);

      // TODO(mcslee): nanovg test
//      this.frameBufferWidth = this.windowWidth;
//      this.frameBufferHeight = this.windowHeight;
//      this.contentScaleX = 1.25f;
//      this.contentScaleY = 1.25f;
//      this.uiWidth = this.frameBufferWidth / this.contentScaleX;
//      this.uiHeight = this.frameBufferHeigh t / this.contentScaleY;
//      this.cursorScaleX = this.uiWidth / this.windowWidth;
//      this.cursorScaleY = this.uiHeight / this.windowHeight;

      log("GLX window: " + this.windowWidth + "x" + this.windowHeight);
      log("GLX frame: " + this.frameBufferWidth + "x" + this.frameBufferHeight);
      log("GLX ui: " + this.uiWidth + "x" + this.uiHeight);
      log("GLX content: " + this.systemContentScaleX + "x" + this.systemContentScaleY);
      log("GLX cursor: " + this.cursorScaleX + "x" + this.cursorScaleY);
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
          this.inputDispatch.onFocus(xPos.get(0) * this.cursorScaleX, yPos.get(0) * this.cursorScaleY);
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
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      this.preferences.setWindowSize(this.windowWidth, this.windowHeight);
    });

    glfwSetWindowContentScaleCallback(this.window, (window, contentScaleX, contentScaleY) -> {
      this.systemContentScaleX = contentScaleX;
      this.systemContentScaleY = contentScaleY;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.systemContentScaleX), 201);
      ui.resize();
      draw();
    });

    glfwSetFramebufferSizeCallback(this.window, (window, width, height) -> {
      this.frameBufferWidth = width;
      this.frameBufferHeight = height;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      bgfx_reset(this.frameBufferWidth, this.frameBufferHeight, BGFX_RESET_VSYNC, this.bgfxFormat);
      ui.resize();
      draw();
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
          error(x, "Exception on drop-file handler: " + x.getLocalizedMessage());
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
      BGFXInit init = BGFXInit.malloc(stack);
      bgfx_init_ctor(init);
      init
        .type(this.bgfxRenderer)
        .vendorId(BGFX_PCI_ID_NONE)
        .deviceId((short) 0)
        .callback(null)
        .allocator(null)
        .resolution(res -> res.width(this.frameBufferWidth).height(this.frameBufferHeight).reset(BGFX_RESET_VSYNC));
      if (!bgfx_init(init)) {
        throw new RuntimeException("Error initializing bgfx renderer");
      }
      this.bgfxFormat = init.resolution().format();
      this.bgfxRenderer = bgfx_get_renderer_type();
    }
    String rendererName = bgfx_get_renderer_name(this.bgfxRenderer);
    if ("NULL".equals(rendererName)) {
      throw new RuntimeException("Error identifying bgfx renderer");
    }
    log("Using BGFX renderer: " + rendererName);
  }

  private boolean setWindowSizeLimits = true;

  private void setUIZoom(float uiScale) {
    this.uiZoom = uiScale;
    this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
    this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
    this.cursorScaleX = this.uiWidth / this.windowWidth;
    this.cursorScaleY = this.uiHeight / this.windowHeight;
    this.vg.notifyContentScaleChanged();
    this.ui.resize();
    this.ui.redraw();
    this.setWindowSizeLimits = true;
  }

  protected void setWindowSize(int windowWidth, int windowHeight) {
    glfwSetWindowSize(this.window, windowWidth, windowHeight);
  }

  private void initializePlatformData() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      BGFXPlatformData platformData = BGFXPlatformData.calloc(stack);
      switch (Platform.get()) {
      case LINUX:
        platformData.ndt(GLFWNativeX11.glfwGetX11Display());
        platformData.nwh(GLFWNativeX11.glfwGetX11Window(this.window));
        break;
      case MACOSX:
//        platformData.ndt(NULL);
//        platformData.nwh(GLFWNativeCocoa.glfwGetCocoaWindow(this.window));

        // NOTE(mcslee): nasty hacks to fix bgfx/M1 bug
        // https://github.com/LWJGL/lwjgl3/issues/619
        long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        long layer = org.lwjgl.system.JNI.invokePPP(ObjCRuntime.objc_getClass("CAMetalLayer"), ObjCRuntime.sel_getUid("alloc"), objc_msgSend);
        org.lwjgl.system.JNI.invokePPP(layer, ObjCRuntime.sel_getUid("init"), objc_msgSend);

        long contentView = org.lwjgl.system.JNI.invokePPP(GLFWNativeCocoa.glfwGetCocoaWindow(window), ObjCRuntime.sel_getUid("contentView"), objc_msgSend);
        org.lwjgl.system.JNI.invokePPPV(contentView, ObjCRuntime.sel_getUid("setLayer:"), layer, objc_msgSend);

        platformData.nwh(layer);

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
    final int FRAME_PERF_LOG = 300;
    long before = System.currentTimeMillis();
    long now;
    int frameCount = 0;
    long drawNanos = 0;
    boolean failed = false;

    while (!glfwWindowShouldClose(this.window)) {

      // Update window size limits
      if (this.setWindowSizeLimits) {
        this.setWindowSizeLimits = false;
        int minWindowWidth = (int) (MIN_WINDOW_WIDTH / this.cursorScaleX);
        int minWindowHeight = (int) (MIN_WINDOW_HEIGHT / this.cursorScaleY);
        glfwSetWindowSizeLimits(this.window, minWindowWidth, minWindowHeight, GLFW_DONT_CARE, GLFW_DONT_CARE);
        if (this.windowWidth < minWindowWidth || this.windowHeight < minWindowHeight) {
          glfwSetWindowSize(
            this.window,
            LXUtils.max(this.windowWidth, minWindowWidth),
            LXUtils.max(this.windowHeight, minWindowHeight)
          );
        }
      }

      // Poll for input events
      this.inputDispatch.poll();

      if (this.needsCursorUpdate) {
        glfwSetCursor(this.window, this.useCursor);
        this.needsCursorUpdate = false;
      }

      if (!failed) {
        long drawStart = System.nanoTime();
        try {
          draw();
        } catch (Throwable x) {
          error(x, "UI THREAD FAILURE: Unhandled error in GLX.draw(): " + x.getLocalizedMessage());
          fail(x);

          // The above should have set a UI failure window to be drawn...
          // Take one last whack at re-drawing. This may very well fail and
          // throw an uncaught error or exception, so be it.
          try {
            draw();
          } catch (Throwable ignored) {
            // Yeah, we thought that may happen.
          }

          failed = true;
        }
        drawNanos += (System.nanoTime() - drawStart);
        if (!failed && (++frameCount == FRAME_PERF_LOG)) {
          frameCount = 0;
          now = System.currentTimeMillis();
          GLX.log("UI thread healthy, running at: " + FRAME_PERF_LOG * 1000f / (now - before) + "fps, average draw time: " + (drawNanos / FRAME_PERF_LOG / 1000) + "us");
          before = now;
          drawNanos = 0;
        }
      }

      // Copy something to the clipboard
      final String copyToClipboard = this._setSystemClipboardString;
      if (copyToClipboard != null) {
        glfwSetClipboardString(this.window, copyToClipboard);
        this._getSystemClipboardString = copyToClipboard;
        this._setSystemClipboardString = null;
      } else {
        String str = glfwGetClipboardString(NULL);
        if ((str != null) && !str.equals(this._getSystemClipboardString)) {
          this._getSystemClipboardString = str;
          this.clipboard.setItem(new LXTextValue(str), false);
        }
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

    // NOTE: destroy the whole UI first, rip down all the listeners
    // before disposing of the engine itself
    this.ui.dispose();

    super.dispose();
  }

  public void useHandCursor(boolean useHandCursor) {
    this.useCursor = useHandCursor ? this.handCursor : 0;
    this.needsCursorUpdate = true;
  }

  protected void importContentJar(File file, File destination) {
    log("Importing content JAR: " + destination.toString());
    try {
      Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      this.engine.addTask(() -> {
        reloadContent();
        this.ui.contextualHelpText.setValue("New content imported into " + destination.getName());
        this.ui.showContextDialogMessage("Added content JAR: " + destination.getName());
      });
    } catch (IOException e) {
      error(e, "Error copying " + file.getName() + " to content directory");
    }
  }

  public void reloadContent() {
    this.registry.reloadContent();
    pushStatusMessage("External content libraries reloaded");
  }

  public void showSaveProjectDialog() {
    showSaveFileDialog(
      "Save Project",
      "Project File",
      new String[] { "lxp" },
      getMediaFolder(LX.Media.PROJECTS).toString() + File.separator + "default.lxp",
      (path) -> { saveProject(new File(path)); }
    );
  }

  public void showOpenProjectDialog() {
    if (this.dialogShowing) {
      return;
    }
    confirmChangesSaved("open another project", () -> {
      showOpenFileDialog(
        "Open Project",
        "Project File",
        new String[] { "lxp" },
        new File(getMediaFolder(LX.Media.PROJECTS), ".").toString(),
        (path) -> { openProject(new File(path)); }
      );
    });
  }

  public void showSaveScheduleDialog() {
    showSaveFileDialog(
      "Save Schedule",
      "Schedule File",
      new String[] { "lxs" },
      getMediaFolder(LX.Media.PROJECTS).toString() + File.separator + "default.lxs",
      (path) -> { this.scheduler.saveSchedule(new File(path)); }
    );
  }

  public void showAddScheduleEntryDialog() {
    if (this.dialogShowing) {
      return;
    }
    showOpenFileDialog(
      "Add Project to Schedule",
      "Project File",
      new String[] { "lxp" },
      new File(getMediaFolder(LX.Media.PROJECTS), ".").toString(),
      (path) -> { this.scheduler.addEntry(new File(path)); }
    );
  }

  public void showOpenScheduleDialog() {
    if (this.dialogShowing) {
      return;
    }
    showOpenFileDialog(
      "Open Schedule",
      "Schedule File",
      new String[] { "lxs" },
      new File(getMediaFolder(LX.Media.PROJECTS), ".").toString(),
      (path) -> { this.scheduler.openSchedule(new File(path)); }
    );
  }

  public interface FileDialogCallback {
    public void fileDialogCallback(String path);
  }

  // Prevent stacking up multiple dialogs
  private volatile boolean dialogShowing = false;

  /**
   * Show a save file dialog
   *
   * @param dialogTitle Dialog title
   * @param fileType File type description
   * @param extensions Valid file extensions
   * @param defaultPath Default file path
   * @param success Callback on successful invocation
   */
  public void showSaveFileDialog(String dialogTitle, String fileType, String[] extensions, String defaultPath, FileDialogCallback success) {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(extensions.length);
          for (String extension : extensions) {
            aFilterPatterns.put(stack.UTF8("*." + extension));
          }
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_saveFileDialog(
            dialogTitle,
            defaultPath,
            aFilterPatterns,
            fileType + " (*." + String.join("/", extensions) + ")"
          );
          dialogShowing = false;
          if (path != null) {
            final int index = path.indexOf('.');
            if (index < 0) {
              path = path + "." + extensions[0];
            } else if (index == path.length() - 1) {
              path = path + extensions[0];
            }
            final String finalPath = path;
            engine.addTask(() -> {
              success.fileDialogCallback(finalPath);
            });
          }
        }
      }
    }.start();
  }

  /**
   * Show an open file dialog
   *
   * @param dialogTitle Dialog title
   * @param fileType File type description
   * @param extensions Valid file extensions
   * @param defaultPath Default file path
   * @param success Callback on successful invocation
   */
  public void showOpenFileDialog(String dialogTitle, String fileType, String[] extensions, String defaultPath, FileDialogCallback success) {
    if (this.dialogShowing) {
      return;
    }
    new Thread() {
      @Override
      public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          PointerBuffer aFilterPatterns = stack.mallocPointer(extensions.length);
          for (String extension : extensions) {
            aFilterPatterns.put(stack.UTF8("*." + extension));
          }
          aFilterPatterns.flip();
          dialogShowing = true;
          String path = tinyfd_openFileDialog(
            dialogTitle,
            defaultPath,
            aFilterPatterns,
            fileType + " (*." + String.join("/", extensions) + ")",
            false
          );
          dialogShowing = false;
          if (path != null) {
            engine.addTask(() -> {
              success.fileDialogCallback(path);
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

  public void showConfirmDialog(String message, Runnable confirm) {
    this.ui.showContextOverlay(new UIDialogBox(this.ui,
      message,
      new String[] { "No", "Yes" },
      new Runnable[] { null, confirm }
    ));
  }

  private String _getSystemClipboardString = null;
  private String _setSystemClipboardString = null;

  @Override
  public void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

  private static final String GLX_PREFIX = "GLX";

  public static void log(String message) {
    LX._log(GLX_PREFIX, message);
  }

  public static void error(Exception x, String message) {
    LX._error(GLX_PREFIX, x, message);
  }

  public static void error(String message) {
    LX._error(GLX_PREFIX, message);
  }

}
