package heronarts.glx.event;

import org.lwjgl.system.Platform;

/**
 * Keyboard shortcut representing a combination of character key and modifier keys.
 */
public class Shortcut extends Modifiers {

  public final int key;

  public Shortcut(int key) {
    this(key, 0);
  }

  public Shortcut(int key, int modifiers) {
    super(modifiers);
    this.key = key;
  }

  /**
   * Create a new shortcut containing the platform-specific command key.
   *
   * @param key Shortcut character key
   * @return
   */
  public static Shortcut command(int key) {
    return command(key, 0);
  }

  /**
   * Create a new shortcut containing the platform-specific command key
   * in addition to any passed modifiers.
   *
   * @param key    Shortcut character key
   * @param modifiers Shortcut modifiers. Command will be added to this mask.
   * @return a new Shortcut containing the Command key
   */
  public static Shortcut command(int key, int modifiers) {
    return new Shortcut(
      key,
      modifiers | ((Platform.get() == Platform.MACOSX) ? META : CONTROL)
    );
  }

}
