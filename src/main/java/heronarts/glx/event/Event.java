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

package heronarts.glx.event;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.system.Platform;

public abstract class Event {

  public static final int SHIFT = GLFW_MOD_SHIFT;
  public static final int CONTROL = GLFW_MOD_CONTROL;
  public static final int ALT = GLFW_MOD_ALT;
  public static final int META = GLFW_MOD_SUPER;
  public static final int CAPS_LOCK = GLFW_MOD_CAPS_LOCK;
  public static final int NUM_LOCK = GLFW_MOD_NUM_LOCK;

  public final int modifiers;

  private boolean isConsumed = false;

  protected Event(int modifiers) {
    this.modifiers = modifiers;
  }

  public int getModifiers() {
    return this.modifiers;
  }

  public boolean hasModifier(int modifier) {
    return (this.modifiers & modifier) != 0;
  }

  public boolean isShiftDown() {
    return hasModifier(SHIFT);
  }

  public boolean isControlDown() {
    return hasModifier(CONTROL);
  }

  public boolean isAltDown() {
    return hasModifier(ALT);
  }

  public boolean isMetaDown() {
    return hasModifier(META);
  }

  public boolean isCommand() {
    return (Platform.get() == Platform.MACOSX) ? isMetaDown() : isControlDown();
  }

  public boolean isMultiSelect() {
    return isMetaDown() || isControlDown();
  }

  public boolean isRangeSelect() {
    return isShiftDown();
  }

  /**
   * Consume the event. Lower-priority event handlers should not perform
   * the action associated with this event.
   *
   * @return this
   */
  public Event consume() {
    this.isConsumed = true;
    return this;
  }

  public boolean isConsumed() {
    return this.isConsumed;
  }
}
