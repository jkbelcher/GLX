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
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.glx;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.system.Platform;

import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.lx.LXEngine;

public class InputDispatch implements LXEngine.Dispatch {

  private final GLX lx;

  private int modifiers = 0;
  private boolean mouseDragging = false;
  private double cursorX = 0, cursorY = 0;
  private boolean macosControlClick = false;
  private KeyEvent keyEvent = null;

  private final List<Event> lxThreadEventQueue = new ArrayList<Event>();
  private final List<Event> glfwThreadEventQueue = Collections.synchronizedList(new ArrayList<Event>());

  InputDispatch(GLX lx) {
    this.lx = lx;
  }

  void glfwKeyCallback(long window, int key, int scancode, int action, int mods) {
    this.modifiers = mods;
    this.glfwThreadEventQueue.add(this.keyEvent = new KeyEvent(key, scancode, action, mods));
  }

  void glfwCharCallback(long window, int codepoint) {
    if (this.keyEvent != null) {
      this.keyEvent.setKeyChar((char) codepoint);
    }
  }

  void glfwCursorPosCallback(long window, double x, double y) {
    double dx = x - this.cursorX;
    double dy = y - this.cursorY;
    MouseEvent.Action action = this.mouseDragging ? MouseEvent.Action.DRAG : MouseEvent.Action.MOVE;
    this.glfwThreadEventQueue.add(new MouseEvent(action, (float) x, (float) y, (float) dx, (float) dy, this.modifiers));
    this.cursorX = x;
    this.cursorY = y;
  };

  void glfwMouseButtonCallback(long window, int button, int action, int mods) {
    // On MacOS, ctrl-click with LEFT behaves as RIGHT
    if (Platform.get() == Platform.MACOSX && (button == GLFW_MOUSE_BUTTON_LEFT)) {
      if ((action == GLFW_PRESS) && ((mods & GLFW_MOD_CONTROL) != 0)) {
        button = GLFW_MOUSE_BUTTON_RIGHT;
        this.macosControlClick = true;
      }

      // Even if ctrl-is not down anymore, the next release
      if ((action == GLFW_RELEASE) && this.macosControlClick) {
        button = GLFW_MOUSE_BUTTON_RIGHT;
        this.macosControlClick = false;
      }
    }

    // Keep track of dragging state
    if (button == GLFW_MOUSE_BUTTON_LEFT) {
      if (action == GLFW_PRESS) {
        this.mouseDragging = true;
      } else if (action == GLFW_RELEASE) {
        this.mouseDragging = false;
      }
    }

    this.glfwThreadEventQueue.add(new MouseEvent(action, button, (float) this.cursorX, (float) this.cursorY, mods));
  }

  void glfwScrollCallback(long window, double dx, double dy) {
    if (Platform.get() == Platform.MACOSX) {
      dx *= this.lx.xContentScale;
      dy *= this.lx.yContentScale;
    }
    this.glfwThreadEventQueue.add(new MouseEvent(MouseEvent.Action.SCROLL, (float) this.cursorX, (float) this.cursorY, (float) dx, (float) dy, this.modifiers));
  }

  public static final double POLL_TIMEOUT = 1/60.;

  void poll() {
    glfwWaitEventsTimeout(POLL_TIMEOUT);
  }

  /**
   * Invoked by the LXEngine thread to handle input events.
   */
  @Override
  public void dispatch() {
    this.lxThreadEventQueue.clear();

    // Lock on the glfw input event queue, in case it's in the middle of polling...
    synchronized (this.glfwThreadEventQueue) {
      // Shallow copy all the glfw events onto an LX-owned thread, then clear it
      this.lxThreadEventQueue.addAll(this.glfwThreadEventQueue);
      this.glfwThreadEventQueue.clear();
    }

    // Release the lock and process events on the LX thread! This is to avoid a lot of
    // horrible message queue passing... the event handlers in UI objects are allowed
    // to directly call the LX engine interfaces to change parameter values, etc. without
    // fucking up the engine thread state
    for (Event event : this.lxThreadEventQueue) {
      if (event instanceof MouseEvent) {
        this.lx.ui.mouseEvent((MouseEvent) event);
      } else if (event instanceof KeyEvent) {
        this.lx.ui.keyEvent((KeyEvent) event);
      } else {
        throw new IllegalStateException("Illegal event type in queue: " + event);
      }
    }
  }

}
