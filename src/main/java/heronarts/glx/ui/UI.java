/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.LXMappingEngine;
import heronarts.lx.LXModulationEngine;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiMapping;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterModulation;
import heronarts.lx.parameter.StringParameter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static org.lwjgl.bgfx.BGFX.*;


/**
 * Top-level container for all overlay UI elements.
 */
public class UI {

  public enum CoordinateSystem {
    LEFT_HANDED,
    RIGHT_HANDED;
  }

  private static UI instance = null;
  public static Thread thread;

  private class UIRoot extends UIObject implements UIContainer {

    private final View view;

    private UIRoot() {
      this.ui = UI.this;
      this.view = new View(this.ui.lx);
      this.view.setClearFlags(BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL);
      this.view.setClearColor(0);
      this.view.setScreenOrtho(getWidth(), getHeight());
    }

    protected void resize() {
      this.view.setRect(0, 0, (int) getWidth(), (int) getHeight());
      this.view.setScreenOrtho(getWidth(), getHeight());
    }

    @Override
    public float getWidth() {
      return this.ui.lx.getWindowWidth();
    }

    @Override
    public float getHeight() {
      return this.ui.lx.getWindowHeight();
    }

    @Override
    public UIObject getContentTarget() {
      return this;
    }

    @Override
    public float getContentWidth() {
      return getWidth();
    }

    @Override
    public float getContentHeight() {
      return getHeight();
    }

    @Override
    void mousePressed(MouseEvent mouseEvent, float mx, float my) {
      // If a context menu is open, we'll want to close it on mouse-press
      // unless the mouse-press is within the context menu itself
      boolean hideContext = false;
      if (contextMenuOverlay.overlayContent != null) {
        hideContext = true;
        contextMenuOverlay.mousePressed = false;
      }
      super.mousePressed(mouseEvent, mx, my);

      // Note: check
      if (!mouseEvent.isContextMenuConsumed() && hideContext && !contextMenuOverlay.mousePressed) {
        hideContextOverlay();
      }
    }

    @Override
    protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      if (topLevelKeyEventHandler != null) {
        topLevelKeyEventHandler.onKeyPressed(keyEvent, keyChar, keyCode);
      }
      if (!keyEvent.isConsumed()) {
        if (keyCode == KeyEvent.VK_Z && (keyEvent.isMetaDown() || keyEvent.isControlDown())) {
          if (keyEvent.isShiftDown()) {
            lx.command.redo();
          } else {
            lx.command.undo();
          }
        } else if (keyCode == KeyEvent.VK_TAB) {
          if (keyEvent.isShiftDown()) {
            focusPrev(keyEvent);
          } else {
            focusNext(keyEvent);
          }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          hideContextOverlay();
        }
      }
    }

    @Override
    protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
      if (topLevelKeyEventHandler != null) {
        topLevelKeyEventHandler.onKeyReleased(keyEvent, keyChar, keyCode);
      }
    }

    private void redraw() {
      for (UIObject child : this.mutableChildren) {
        if (child instanceof UI2dComponent) {
          ((UI2dComponent) child).redraw();
        }
      }
    }

    private UIObject findCurrentFocus() {
      UIObject currentFocus = this;
      while (currentFocus.focusedChild != null) {
        currentFocus = currentFocus.focusedChild;
      }
      return currentFocus;
    }

    private UIObject findNextFocusable() {
      // Identify the deepest focused object
      UIObject focus = findCurrentFocus();

      // Check if it has a child that is eligible for focus
      UIObject focusableChild = findNextFocusableChild(focus, 0);
      if (focusableChild != null) {
        return focusableChild;
      }

      // Work up the tree, trying siblings at each level
      while (focus.parent != null) {
        int focusIndex = focus.parent.mutableChildren.indexOf(focus);
        focusableChild = findNextFocusableChild(focus.parent, focusIndex + 1);
        if (focusableChild != null) {
          return focusableChild;
        }
        focus = focus.parent;
      }

      // We ran out! Loop around from the front...
      return findNextFocusableChild(this, 0);
    }

    private UIObject findNextFocusableChild(UIObject focus, int startIndex) {
      for (int i = startIndex; i < focus.mutableChildren.size(); ++i) {
        UIObject child = focus.mutableChildren.get(i);
        if (child.isVisible()) {
          if (child instanceof UITabFocus) {
            return child;
          }
          UIObject recurse = findNextFocusableChild(child, 0);
          if (recurse != null) {
            return recurse;
          }
        }
      }
      return null;
    }

    private UIObject findPrevFocusable() {
      // Identify the deepest focused object
      UIObject focus = findCurrentFocus();

      // Check its previous siblings, depth-first
      while (focus.parent != null) {
        int focusIndex = focus.parent.mutableChildren.indexOf(focus);
        UIObject focusableChild = findPrevFocusableChild(focus.parent, focusIndex - 1);
        if (focusableChild != null) {
          return focusableChild;
        }
        if (focus.parent instanceof UITabFocus) {
          return focus.parent;
        }
        focus = focus.parent;
      }

      // We failed! Wrap around to the end
      return findPrevFocusableChild(this, this.mutableChildren.size() - 1);
    }

    private UIObject findPrevFocusableChild(UIObject focus, int startIndex) {
      for (int i = startIndex; i >= 0; --i) {
        UIObject child = focus.mutableChildren.get(i);
        if (child.isVisible()) {
          UIObject recurse = findPrevFocusableChild(child, child.mutableChildren.size() - 1);
          if (recurse != null) {
            return recurse;
          }
          if (child instanceof UITabFocus) {
            return child;
          }
        }
      }
      return null;
    }

    // Limit the number of nanovg buffers we'll render in a single pass
    private static final int MAX_NVG_VIEWS_PER_PASS = 30;

    private final Stack<UI2dContext> renderStack = new Stack<UI2dContext>();

    public void draw() {
      this.renderStack.clear();

      // First pass, we determine which UI2dContexts need rendering, and push
      // them all onto a stack. Each will need its own BGFX view because they have
      // unique framebuffers.
      for (UIObject child : this.mutableChildren) {
        if (child instanceof UI2dContext) {
          ((UI2dContext) child).populateRenderStack(renderStack);
        }
      }

      // Now we have all of our UI2dContexts ready to go, render all of them
      // as necessary.
      short viewId = 0;
      while (!renderStack.isEmpty() && (viewId < MAX_NVG_VIEWS_PER_PASS)) {
        renderStack.pop().setView(viewId++).render(vg);
      }

      // Draw any 3d contexts
      for (UIObject child : this.mutableChildren) {
        if (child instanceof UI3dContext) {
          UI3dContext context3d = (UI3dContext) child;
          context3d.view.setId(viewId++);
          context3d.draw(this.ui, context3d.view);
        }
      }

      // Finally, draw all 2d overlays onto the root view
      this.view.bind(viewId++);
      for (UIObject child : this.mutableChildren) {
        if (child instanceof UI2dContext) {
          ((UI2dContext) child).draw(this.ui, this.view);
        }
      }
    }
  }

  /**
   * Redraw may be called from any thread
   */
  private final List<UI2dComponent> threadSafeRedrawQueue =
    Collections.synchronizedList(new ArrayList<UI2dComponent>());

  /**
   * Objects to redraw on current pass thru animation thread
   */
  private final List<UI2dComponent> glfwThreadRedrawList =
    new ArrayList<UI2dComponent>();

  public class Timer {
    public long drawNanos = 0;
  }

  public final Timer timer = new Timer();

  public final GLX lx;
  public final VGraphics vg;

  private UIRoot root;

  public final StringParameter contextualHelpText =
    new StringParameter("Contextual Help")
    .setDescription("Parameter for contextual help messages in the bottom bar");

  protected CoordinateSystem coordinateSystem = CoordinateSystem.LEFT_HANDED;

  private static final long INIT_RUN = -1;
  private long lastMillis = INIT_RUN;

  private UIEventHandler topLevelKeyEventHandler = null;

  private class UIContextOverlay extends UI2dContext {

    private boolean mousePressed = false;

    private UI2dComponent overlayContent = null;

    public UIContextOverlay() {
      super(UI.this, 0, 0, 0, 0);
      this.parent = root;
      setUI(UI.this);
      setBackgroundColor(0);
    }

    private void setContent(UI2dComponent overlayContent) {
      if (this.overlayContent != null) {
        this.overlayContent.setVisible(false);
        this.overlayContent.removeFromContainer();
        root.mutableChildren.remove(this);
      }
      this.overlayContent = overlayContent;
      if (overlayContent != null) {
        setSize(overlayContent.getWidth(), overlayContent.getHeight());
        float x = 0;
        float y = 0;
        UIObject component = overlayContent;
        while (component != root && component != null) {
          x += component.getX();
          y += component.getY();
          if (component instanceof UI2dScrollContext) {
            UI2dScrollContext scrollContext = (UI2dScrollContext) component;
            x += scrollContext.getScrollX();
            y += scrollContext.getScrollY();
          }
          component = component.getParent();
        }
        setPosition(x, y);
        overlayContent.setVisible(true);
        overlayContent.setPosition(0, 0);
        overlayContent.addToContainer(this);
        root.mutableChildren.add(this);
      }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent, float mx, float my) {
      super.mousePressed(mouseEvent, mx, my);
      this.mousePressed = true;
    }
  }

  /**
   * Drop menu overlay object
   */
  private UIContextOverlay contextMenuOverlay;

  /**
   * UI look and feel
   */
  public final UITheme theme;

  /**
   * White color
   */
  public final static int WHITE = 0xffffffff;

  /**
   * Black color
   */
  public final static int BLACK = 0xff000000;

  boolean midiMapping = false;
  boolean modulationSourceMapping = false;
  boolean modulationTargetMapping = false;
  boolean triggerSourceMapping = false;
  boolean triggerTargetMapping = false;
  LXModulationEngine modulationEngine = null;
  LXParameterModulation highlightParameterModulation = null;

  private UIControlTarget controlTarget = null;
  private UITriggerSource triggerSource = null;
  private UIModulationSource modulationSource = null;

  public UI(final GLX lx) throws IOException {
    if (UI.instance != null) {
      throw new IllegalStateException("May not instantiate multiple copies of UI");
    }

    UI.instance = this;
    UI.thread = Thread.currentThread();

    this.lx = lx;
    this.vg = lx.vg;

    this.theme = new UITheme(this.vg);
    LX.initTimer.log("P3LX: UI: Theme");

    this.root = new UIRoot();
    this.contextMenuOverlay = new UIContextOverlay();
    LX.initTimer.log("P3LX: UI: Root");

    lx.addProjectListener(new LX.ProjectListener() {
      @Override
      public void projectChanged(File file, Change change) {
        switch (change) {
        case NEW:
          contextualHelpText.setValue("Created new project");
          break;
        case SAVE:
          contextualHelpText.setValue("Saved project file: " + file.getName());
          break;
        case OPEN:
          contextualHelpText.setValue("Opened project file: " + file.getName());
          break;
        }
      }
    });

    lx.engine.mapping.mode.addListener((p) -> {

      this.midiMapping = lx.engine.mapping.getMode() == LXMappingEngine.Mode.MIDI;
      this.modulationSourceMapping = lx.engine.mapping.getMode() == LXMappingEngine.Mode.MODULATION_SOURCE;
      this.modulationTargetMapping = lx.engine.mapping.getMode() == LXMappingEngine.Mode.MODULATION_TARGET;
      this.triggerSourceMapping = lx.engine.mapping.getMode() == LXMappingEngine.Mode.TRIGGER_SOURCE;
      this.triggerTargetMapping = lx.engine.mapping.getMode() == LXMappingEngine.Mode.TRIGGER_TARGET;

      if (this.midiMapping) {
        this.contextualHelpText.setValue("Click on a control target to MIDI map, eligible controls are highlighted");
      } else if (this.modulationSourceMapping) {
        this.contextualHelpText.setValue("Click on a modulation source, eligible sources are highlighted ");
      } else if (this.modulationTargetMapping) {
        LXNormalizedParameter sourceParameter = modulationSource.getModulationSource();
        if (sourceParameter == null) {
          this.contextualHelpText.setValue("You are somehow mapping a non-existent source parameter, choose a destination");
        } else {
          this.contextualHelpText.setValue("Select a modulation destination for " + LXComponent.getCanonicalLabel(sourceParameter) + ", eligible targets are highlighted");
        }
      } else if (this.triggerSourceMapping) {
        this.contextualHelpText.setValue("Click on a trigger source, eligible sources are highlighted ");
      } else if (this.triggerTargetMapping) {
        this.contextualHelpText.setValue("Select a trigger destination for " + LXComponent.getCanonicalLabel(triggerSource.getTriggerSource()) + ", eligible targets are highlighted");
      } else {
        this.contextualHelpText.setValue("");
      }

      this.root.redraw();
    });

    lx.engine.midi.addMappingListener(new LXMidiEngine.MappingListener() {

      @Override
      public void mappingRemoved(LXMidiEngine engine, LXMidiMapping mapping) {
      }

      @Override
      public void mappingAdded(LXMidiEngine engine, LXMidiMapping mapping) {
        if (midiMapping) {
          contextualHelpText.setValue("Successfully mapped MIDI Ch." + (mapping.channel+1) + " " + mapping.getDescription() + " to " + LXComponent.getCanonicalLabel(mapping.parameter));
        }
      }
    });

    lx.command.errorChanged.addListener((p) -> {
      String error = lx.command.getError();
      if (error != null) {
        showContextOverlay(new UIDialogBox(this, error, () -> { lx.command.popError(); }));
      }
    });
  }

  public UI setHighlightParameterModulation(LXParameterModulation highlightParameterModulation) {
    if (this.highlightParameterModulation != highlightParameterModulation) {
      this.highlightParameterModulation = highlightParameterModulation;
      this.root.redraw();
    }
    return this;
  }

  public UI setCoordinateSystem(CoordinateSystem coordinateSystem) {
    this.coordinateSystem = coordinateSystem;
    return this;
  }

  public void reflow() {
    // Subclasses may override this method for top-level UI changes
  }

  public static UI get() {
    return UI.instance;
  }

  public void focusPrev(Event event) {
    UIObject focusTarget = this.root.findPrevFocusable();
    if (focusTarget != null) {
      focusTarget.focus(event);
    }
  }

  public void focusNext(Event event) {
    UIObject focusTarget = this.root.findNextFocusable();
    if (focusTarget != null) {
      focusTarget.focus(event);
    }
  }

  private boolean isMapping() {
    return this.midiMapping || this.modulationSourceMapping || this.modulationTargetMapping || this.triggerSourceMapping || this.triggerTargetMapping;
  }

  void setMouseoverHelpText(String helpText) {
    if (!isMapping()) {
      this.contextualHelpText.setValue(helpText);
    }
  }

  void clearMouseoverHelpText() {
    if (!isMapping()) {
      this.contextualHelpText.setValue("");
    }
  }

  /**
   * Sets an object to handle top-level input events
   *
   * @param eventHandler Event handler
   * @return this
   */
  public UI setTopLevelKeyEventHandler(UIEventHandler eventHandler) {
    this.topLevelKeyEventHandler = eventHandler;
    return this;
  }

  UI setControlTarget(UIControlTarget controlTarget) {
    this.lx.engine.mapping.setControlTarget(controlTarget.getControlTarget());
    LXParameter midiParameter = controlTarget.getControlTarget();
    if (midiParameter == null) {
      this.contextualHelpText.setValue("Press a MIDI key or controller to map a non-existent parameter?");
    } else {
      this.contextualHelpText.setValue("Press a MIDI key or controller to map " + LXComponent.getCanonicalLabel(midiParameter));
    }
    if (this.controlTarget != controlTarget) {
      if (this.controlTarget != null) {
        ((UI2dComponent) this.controlTarget).redraw();
      }
      this.controlTarget = controlTarget;
      if (this.controlTarget != null) {
        ((UI2dComponent) this.controlTarget).redraw();
      }
    }
    return this;
  }

  UIControlTarget getControlTarget() {
    return this.controlTarget;
  }

  public UI mapTriggerSource(UITriggerSource triggerSource) {
    this.modulationEngine = this.lx.engine.modulation;
    this.triggerSource = triggerSource;
    this.lx.engine.mapping.setMode(triggerSource == null ? LXMappingEngine.Mode.OFF : LXMappingEngine.Mode.TRIGGER_TARGET);
    return this;
  }

  UITriggerSource getTriggerSource() {
    return this.triggerSource;
  }

  public UI mapModulationSource(UIModulationSource modulationSource) {
    return mapModulationSource(this.lx.engine.modulation, modulationSource);
  }

  public UI mapModulationSource(LXModulationEngine modulationEngine, UIModulationSource modulationSource) {
    this.modulationEngine = modulationEngine;
    this.modulationSource = modulationSource;
    this.lx.engine.mapping.setMode(modulationSource == null ? LXMappingEngine.Mode.OFF : LXMappingEngine.Mode.MODULATION_TARGET);
    return this;
  }

  UIModulationSource getModulationSource() {
    return this.modulationSource;
  }

  /**
   * Add a task to be performed on every loop of the UI engine.
   *
   * @param loopTask Task to perform on every UI loop
   * @return this
   */
  public UI addLoopTask(LXLoopTask loopTask) {
    this.root.addLoopTask(loopTask);
    return this;
  }

  /**
   * Remove a task from the UI engine
   *
   * @param loopTask Task to stop performing on every UI loop
   * @return this
   */
  public UI removeLoopTask(LXLoopTask loopTask) {
    this.root.removeLoopTask(loopTask);
    return this;
  }

  /**
   * Add a 2d context to this UI
   *
   * @param layer UI layer
   * @return this
   */
  public UI addLayer(UI2dContext layer) {
    layer.addToContainer(this.root);
    return this;
  }

  /**
   * Remove a 2d context from this UI
   *
   * @param layer UI layer
   * @return this UI
   */
  public UI removeLayer(UI2dContext layer) {
    layer.removeFromContainer();
    return this;
  }

  /**
   * Add a 3d context to this UI
   *
   * @param layer 3d context
   * @return this UI
   */
  public UI addLayer(UI3dContext layer) {
    this.root.mutableChildren.add(layer);
    layer.parent = this.root;
    layer.setUI(this);
    return this;
  }

  public UI removeLayer(UI3dContext layer) {
    if (layer.parent != this.root) {
      throw new IllegalStateException("Cannot remove 3d layer which is not present");
    }
    this.root.mutableChildren.remove(layer);
    layer.parent = null;
    return this;
  }

  /**
   * Brings a layer to the top of the UI stack
   *
   * @param layer UI layer
   * @return this UI
   */
  public UI bringToTop(UI2dContext layer) {
    this.root.mutableChildren.remove(layer);
    this.root.mutableChildren.add(layer);
    return this;
  }

  public UI hideContextOverlay() {
    showContextOverlay(null);
    return this;
  }

  public UI showContextOverlay(UI2dComponent contextOverlay) {
    this.contextMenuOverlay.setContent(contextOverlay);
    return this;
  }

  void redraw(UI2dComponent object) {
    this.threadSafeRedrawQueue.add(object);
  }

  public int getWidth() {
    return this.lx.getWindowWidth();
  }

  public int getHeight() {
    return this.lx.getWindowHeight();
  }

  public void resize() {
    this.root.resize();
    onResize();
  }

  /**
   * Draws the UI
   */
  public final void draw() {

    beginDraw();

    long drawStart = System.nanoTime();

    long nowMillis = System.currentTimeMillis();
    if (this.lastMillis == INIT_RUN) {
      // Initial frame is arbitrarily 16 milliseconds (~60 fps)
      this.lastMillis = nowMillis - 16;
    }
    double deltaMs = nowMillis - this.lastMillis;
    this.lastMillis = nowMillis;

    // Run loop tasks through the UI tree
    this.root.loop(deltaMs);

    // Iterate through all objects that need redraw state marked
    this.glfwThreadRedrawList.clear();
    synchronized (this.threadSafeRedrawQueue) {
      this.glfwThreadRedrawList.addAll(this.threadSafeRedrawQueue);
      this.threadSafeRedrawQueue.clear();
    }
    for (UI2dComponent object : this.glfwThreadRedrawList) {
      object._redraw();
    }

    // Draw from the root
    this.root.draw();

    endDraw();

    this.timer.drawNanos = System.nanoTime() - drawStart;
  }

  protected void beginDraw() {
    // Subclasses may override...
  }

  protected void endDraw() {
    // Subclasses may override...
  }

  protected void onResize() {
    // Subclasses may override
  }

  public void mouseEvent(MouseEvent mouseEvent) {
    switch (mouseEvent.getAction()) {
    case SCROLL:
      this.root.mouseScroll(mouseEvent, mouseEvent.x, mouseEvent.y, mouseEvent.dx, mouseEvent.dy);
      return;
    case PRESS:
      this.root.mousePressed(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    case RELEASE:
      this.root.mouseReleased(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    case DRAG:
      this.root.mouseDragged(mouseEvent, mouseEvent.x, mouseEvent.y, mouseEvent.dx, mouseEvent.dy);
      break;
    case MOVE:
      this.root.mouseMoved(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    }
  }

  public void keyEvent(KeyEvent keyEvent) {
    _engineThreadDefaultKeyEvent(keyEvent);

    char keyChar = keyEvent.getKeyChar();
    int keyCode = keyEvent.getKeyCode();
    switch (keyEvent.getAction()) {
    case RELEASE:
      this.root.keyReleased(keyEvent, keyChar, keyCode);
      break;
    case PRESS:
    case REPEAT:
      this.root.keyPressed(keyEvent, keyChar, keyCode);
      break;
    default:
      throw new RuntimeException("Invalid keyEvent type: " + keyEvent.getAction());
    }
  }

  private void _engineThreadDefaultKeyEvent(KeyEvent keyEvent) {
    int keyCode = keyEvent.getKeyCode();
    KeyEvent.Action action = keyEvent.getAction();
    if (action == KeyEvent.Action.PRESS) {
      switch (keyCode) {
      case KeyEvent.VK_S:
        if (keyEvent.isCommand()) {
          if (keyEvent.isShiftDown() || lx.getProject() == null) {
            lx.showSaveProjectDialog();
          } else {
            lx.saveProject();
          }
        }
        break;
      case KeyEvent.VK_O:
        if (keyEvent.isCommand()) {
          lx.showOpenProjectDialog();
        }
        break;
      }
    }
  }
}
