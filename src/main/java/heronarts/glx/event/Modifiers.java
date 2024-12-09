package heronarts.glx.event;

import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CAPS_LOCK;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_NUM_LOCK;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER;

/**
 * Modifier keys state and utilities.
 */
public abstract class Modifiers {

  public static final int SHIFT = GLFW_MOD_SHIFT;
  public static final int CONTROL = GLFW_MOD_CONTROL;
  public static final int ALT = GLFW_MOD_ALT;
  public static final int META = GLFW_MOD_SUPER;
  public static final int CAPS_LOCK = GLFW_MOD_CAPS_LOCK;
  public static final int NUM_LOCK = GLFW_MOD_NUM_LOCK;

  /**
   * Bitmask of modifier keys held
   */
  public final int modifiers;

  protected Modifiers(int modifiers) {
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

  public static int getCommand() {
    return (Platform.get() == Platform.MACOSX) ? META : CONTROL;
  }

  public boolean isMultiSelect() {
    return isMetaDown() || isControlDown();
  }

  public boolean isRangeSelect() {
    return isShiftDown();
  }

}
