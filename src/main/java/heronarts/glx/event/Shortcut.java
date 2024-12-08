package heronarts.glx.event;

/**
 * Keyboard shortcut representing a combination of button and modifier keys.
 */
public class Shortcut extends Modifiers {

  public final int button;

  public Shortcut(int button, int modifiers) {
    super(modifiers);
    this.button = button;
  }
}
