package heronarts.glx.event;

import org.lwjgl.system.Platform;

/**
 * Keyboard shortcut representing a combination of character key and modifier keys.
 */
public class Shortcut extends Modifiers {

  public final int button;

  public Shortcut(int button) {
    this(button, 0);
  }

  public Shortcut(int button, int modifiers) {
    super(modifiers);
    this.button = button;
  }

  /**
   * Create a new shortcut containing the platform-specific command key.
   *
   * @param button Shortcut character key
   * @return
   */
  public static Shortcut command(int button) {
    return command(button, 0);
  }

  /**
   * Create a new shortcut containing the platform-specific command key
   * in addition to any passed modifiers.
   *
   * @param button    Shortcut character key
   * @param modifiers Shortcut modifiers. Command will be added to this mask.
   * @return a new Shortcut containing the Command key
   */
  public static Shortcut command(int button, int modifiers) {
    return new Shortcut(
      button,
      modifiers | ((Platform.get() == Platform.MACOSX) ? META : CONTROL)
    );
  }

}
