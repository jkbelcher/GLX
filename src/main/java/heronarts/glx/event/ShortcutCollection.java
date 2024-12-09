package heronarts.glx.event;

import heronarts.lx.LX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static heronarts.glx.event.Event.ALT;
import static heronarts.glx.event.Event.CONTROL;
import static heronarts.glx.event.Event.META;
import static heronarts.glx.event.Event.SHIFT;

/**
 * Collection of keyboard shortcuts. Provides efficient comparison against
 * KeyEvents by pre-grouping shortcuts by modifier keys.
 */
public class ShortcutCollection {

  private static final int[] ALL_MODIFIERS = new int[]{SHIFT, CONTROL, ALT, META};

  private final List<Shortcut> mutableShortcuts = new ArrayList<Shortcut>();
  public final List<Shortcut> shortcuts = Collections.unmodifiableList(this.mutableShortcuts);

  // Key lookups by modifier
  private final Map<Integer, List<Shortcut>> map = new HashMap<Integer, List<Shortcut>>();

  public ShortcutCollection() {
    this(ALL_MODIFIERS);
  }

  public ShortcutCollection(int[] modifiers) {
    initialize(modifiers);
  }

  /**
   * Create a dictionary entry for every bitmask combination of modifier keys
   */
  private void initialize(int[] modifiers) {
    final int n = modifiers.length;
    // Loop over all possible 2^n combinations
    for (int i = 0; i < (1 << n); i++) {
      int combinedMask = 0;
      for (int j = 0; j < n; j++) {
        if ((i & (1 << j)) != 0) {
          combinedMask |= modifiers[j];
        }
      }
      // Using ArrayList in case there are duplicates of a button.
      // A duplicate should throw a warning but the collection should be able to handle it.
      this.map.put(combinedMask, new ArrayList<Shortcut>());
    }
  }

  public ShortcutCollection add(Shortcut... shortcuts) {
    for (Shortcut shortcut : shortcuts) {
      Objects.requireNonNull(shortcut);
      if (this.shortcuts.contains(shortcut)) {
        throw new UnsupportedOperationException("Shortcut object already exists in collection: " + shortcut);
      }
      List<Shortcut> mapEntry = this.map.get(shortcut.modifiers);
      if (mapEntry != null) {
        // Warn for duplicates
        for (Shortcut existing : mapEntry) {
          if (existing.key == shortcut.key) {
            LX.warning("Shortcut " + shortcut + " conflicts with " + existing);
          }
        }
        mapEntry.add(shortcut);
        this.mutableShortcuts.add(shortcut);
      } else {
        LX.error("Unknown modifier key combination: " + shortcut.modifiers);
      }
    }
    return this;
  }

  public boolean remove(Shortcut shortcut) {
    if (this.mutableShortcuts.remove(shortcut)) {
      this.map.get(shortcut.modifiers).remove(shortcut);
      return true;
    }
    return false;
  }

  /**
   * Retrieve a list of all shortcuts in this collection
   */
  public List<Shortcut> getAll() {
    return this.shortcuts;
  }

  /**
   * Returns the first Shortcut in the collection that matches the KeyEvent.
   *
   * @param event A KeyEvent for comparison.
   * @return The first matching shortcut, or null if none is found.
   */
  public Shortcut match(KeyEvent event) {
    return map.getOrDefault(event.modifiers, Collections.emptyList())
              .stream()
              .filter(shortcut -> shortcut.key == event.keyCode)
              .findFirst()
              .orElse(null);
  }
}
