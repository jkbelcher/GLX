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

public abstract class Event extends Modifiers {

  private static class Virtual extends Event {
    private Virtual() {
      super(0);
    }

    @Override
    public Event consume() {
      // This event can never actually be consumed,
      // because it doesn't exist!
      return this;
    }
  }

  public static final Event NONE = new Virtual();
  public static final Event SIBLING_REMOVED = new Virtual();

  /**
   * Value of glfwGetTime() in seconds when the event occurred
   */
  public final double glfwTime;

  /**
   * Java System.nanoTime() when the event occurred
   */
  public final long nanoTime;

  private boolean isConsumed = false;

  protected Event(int modifiers) {
    this(modifiers, glfwGetTime(), System.nanoTime());
  }

  protected Event(int modifiers, double glfwTime, long nanoTime) {
    super(modifiers);
    this.glfwTime = glfwTime;
    this.nanoTime = nanoTime;
  }

  /**
   * Gets the time of the event in seconds, as returned by
   * the GLFW timer
   *
   * @return Time in seconds, returned by GLFW
   */
  public double getTime() {
    return this.glfwTime;
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
