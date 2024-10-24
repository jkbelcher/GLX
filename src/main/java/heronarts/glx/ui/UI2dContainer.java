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

import java.util.Iterator;
import java.util.List;

import heronarts.glx.event.KeyEvent;

public class UI2dContainer extends UI2dComponent implements UIContainer, Iterable<UIObject> {

  public enum Layout {
    NONE,
    VERTICAL,
    HORIZONTAL,
    VERTICAL_GRID,
    HORIZONTAL_GRID
  }

  public enum ArrowKeyFocus {
    NONE,
    VERTICAL,
    HORIZONTAL
  };

  private Layout layout = Layout.NONE;

  ArrowKeyFocus arrowKeyFocus = ArrowKeyFocus.NONE;

  private float topPadding = 0, rightPadding = 0, bottomPadding = 0, leftPadding = 0;

  private float childSpacingX = 0, childSpacingY = 0;

  private float minHeight = 0, minWidth = 0;

  private UI2dContainer contentTarget;

  public static UI2dContainer newHorizontalContainer(float height) {
    return newHorizontalContainer(height, 0, (UI2dComponent[]) null);
  }

  public static UI2dContainer newHorizontalContainer(float height, float childSpacing) {
    return newHorizontalContainer(height, childSpacing, (UI2dComponent[]) null);
  }

  public static UI2dContainer newHorizontalContainer(float height, float childSpacing, UI2dComponent ... children) {
    UI2dContainer container = new UI2dContainer(0, 0, 0, height)
      .setLayout(UI2dContainer.Layout.HORIZONTAL)
      .setChildSpacing(childSpacing);
    if (children != null) {
      for (UI2dComponent child : children) {
        child.addToContainer(container);
      }
    }
    return container;
  }

  public static UI2dContainer newVerticalContainer(float width) {
    return newVerticalContainer(width, 0);
  }

  public static UI2dContainer newVerticalContainer(float width, float childSpacing) {
    return newVerticalContainer(width, childSpacing, (UI2dComponent[]) null);
  }

  public static UI2dContainer newVerticalContainer(float width, float childSpacing, UI2dComponent ... children) {
    UI2dContainer container = new UI2dContainer(0, 0, width, 0)
      .setLayout(UI2dContainer.Layout.VERTICAL)
      .setChildSpacing(childSpacing);
    if (children != null) {
      for (UI2dComponent child : children) {
        child.addToContainer(container);
      }
    }
    return container;
  }

  public UI2dContainer(float x, float y, float w, float h) {
    super(x, y, w, h);
    this.contentTarget = this;
  }

  public UI2dContainer setPadding(float padding) {
    return setPadding(padding, padding, padding, padding);
  }

  public UI2dContainer setPadding(float yPadding, float xPadding) {
    return setPadding(yPadding, xPadding, yPadding, xPadding);
  }

  public UI2dContainer setPadding(float topPadding, float rightPadding, float bottomPadding, float leftPadding) {
    boolean reflow = false;
    if (this.contentTarget.topPadding != topPadding) {
      this.contentTarget.topPadding = topPadding;
      reflow = true;
    }
    if (this.contentTarget.rightPadding != rightPadding) {
      this.contentTarget.rightPadding = rightPadding;
      reflow = true;
    }
    if (this.contentTarget.bottomPadding != bottomPadding) {
      this.contentTarget.bottomPadding = bottomPadding;
      reflow = true;
    }
    if (this.contentTarget.leftPadding != leftPadding) {
      this.contentTarget.leftPadding = leftPadding;
      reflow = true;
    }
    if (reflow) {
      this.contentTarget.reflow();
    }
    return this;
  }

  /**
   * Deprecated. Use {@link #setChildSpacing(float)} instead
   *
   * @param childMargin Child margin
   * @return this
   */
  @Deprecated
  public UI2dContainer setChildMargin(float childMargin) {
    return setChildSpacing(childMargin);
  }

  public UI2dContainer setChildSpacing(float childSpacing) {
    return setChildSpacing(childSpacing, childSpacing);
  }

  public UI2dContainer setChildSpacing(float childSpacingY, float childSpacingX) {
    if ((this.contentTarget.childSpacingX != childSpacingX) || (this.contentTarget.childSpacingY != childSpacingY)) {
      this.contentTarget.childSpacingX = childSpacingX;
      this.contentTarget.childSpacingY = childSpacingY;
      this.contentTarget.reflow();
    }
    return this;
  }

  public UI2dContainer setMinWidth(float minWidth) {
    if (this.contentTarget.minWidth != minWidth) {
      this.contentTarget.minWidth = minWidth;
      reflow();
    }
    return this;
  }

  public UI2dContainer setMinHeight(float minHeight) {
    if (this.contentTarget.minHeight != minHeight) {
      this.contentTarget.minHeight = minHeight;
      reflow();
    }
    return this;
  }

  public UI2dContainer setLayout(Layout layout) {
    if (this.contentTarget.layout != layout) {
      this.contentTarget.layout = layout;
      this.contentTarget.reflow();
    }
    return this;
  }

  public UI2dContainer setArrowKeyFocus(ArrowKeyFocus keyFocus) {
    this.contentTarget.arrowKeyFocus = keyFocus;
    return this;
  }

  public UI2dContainer addChildren(UI2dComponent ... children) {
    for (UI2dComponent child : children) {
      child.addToContainer(this);
    }
    return this;
  }

  protected void reflow() {
    if (this.layout == Layout.VERTICAL) {
      float y = this.topPadding;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          component.setY(y + component.topMargin);
          y += component.topMargin + component.getHeight() + component.bottomMargin + this.childSpacingY;
        }
      }
      y += this.bottomPadding;
      setContentHeight(Math.max(this.minHeight, y - this.childSpacingY));
    } else if (this.layout == Layout.HORIZONTAL) {
      float x = this.leftPadding;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          component.setX(x + component.leftMargin);
          x += component.leftMargin + component.getWidth() + component.rightMargin + this.childSpacingX;
        }
      }
      x += this.rightPadding;
      setContentWidth(Math.max(this.minWidth, x - this.childSpacingX));
    } else if (this.layout == Layout.VERTICAL_GRID) {
      float x = this.leftPadding;
      float y = this.topPadding;
      float w = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          if (y + component.topMargin + component.getHeight() > getContentHeight()) {
            x += w + this.childSpacingX;
            y = this.topPadding;
            w = 0;
          }
          component.setPosition(x + component.leftMargin, y + component.topMargin);
          w = Math.max(w, component.getWidth() + component.leftMargin + component.rightMargin);
          y += component.topMargin + component.getHeight() + component.bottomMargin + this.childSpacingY;
        }
      }
      setContentWidth(Math.max(this.minWidth, x + w + this.rightPadding));
    } else if (this.layout == Layout.HORIZONTAL_GRID) {
      float x = this.leftPadding;
      float y = this.topPadding;
      float h = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          if (x + component.leftMargin + component.getWidth() > getContentWidth()) {
            y += h + this.childSpacingY;
            x = this.leftPadding;
            h = 0;
          }
          component.setPosition(x + component.leftMargin, y + component.topMargin);
          h = Math.max(h, component.topMargin + component.getHeight() + component.bottomMargin);
          x += component.leftMargin + component.getWidth() + component.rightMargin + this.childSpacingX;
        }
      }
      setContentHeight(Math.max(this.minHeight, y + h + this.bottomPadding));
    }
  }

  protected UI2dContainer setContentTarget(UI2dContainer contentTarget) {
    if (this.mutableChildren.contains(contentTarget)) {
      throw new IllegalStateException("contentTarget already belongs to container: " + contentTarget);
    }
    this.contentTarget = contentTarget;
    this.mutableChildren.add(contentTarget);
    contentTarget.parent = this;
    contentTarget.setUI(this.ui);
    redraw();
    return this;
  }

  protected UI2dContainer addTopLevelComponent(UI2dComponent child) {
    if (child.parent != null) {
      child.removeFromContainer();
    }
    this.mutableChildren.add(child);
    child.parent = this;
    child.setUI(this.ui);
    redraw();
    return this;
  }

  /**
   * Returns the object that elements are added to when placed in this container.
   * In most cases, it will be "this" - but some elements have special subcontainers.
   *
   * @return Element
   */
  @Override
  public UI2dContainer getContentTarget() {
    return this.contentTarget;
  }

  @Override
  public float getContentWidth() {
    return getContentTarget().getWidth();
  }

  @Override
  public float getContentHeight() {
    return getContentTarget().getHeight();
  }

  public UI2dContainer setContentWidth(float w) {
    return setContentSize(w, getContentHeight());
  }

  public UI2dContainer setContentHeight(float h) {
    return setContentSize(getContentWidth(), h);
  }

  public UI2dContainer setContentSize(float w, float h) {
    this.contentTarget.setSize(w, h);
    return this;
  }

  public UI2dContainer removeAllChildren() {
    UI2dContainer contentTarget = getContentTarget();
    for (UIObject child : contentTarget.mutableChildren) {
      ((UI2dComponent) child).parent = null;
    }
    contentTarget.mutableChildren.clear();
    reflow();
    return this;
  }

  @Override
  public Iterator<UIObject> iterator() {
    return this.contentTarget.mutableChildren.iterator();
  }

  public List<UIObject> getChildren() {
    return this.contentTarget.mutableChildren;
  }

  public UI2dComponent getChild(int i) {
    return (UI2dComponent) this.contentTarget.mutableChildren.get(i);
  }

  private void keyFocus(KeyEvent keyEvent, int delta) {
    if (this.children.size() > 0) {
      UIObject focusedChild = getFocusedChild();
      if (focusedChild == null) {
        for (UIObject object : this.children) {
          if (object.isVisible() && (object instanceof UIKeyFocus)) {
            object.focus(keyEvent);
            break;
          }
        }
      } else {
        int index = this.children.indexOf(focusedChild);
        while (true) {
          index += delta;
          if (index < 0 || index >= this.children.size()) {
            break;
          }
          UIObject object = this.children.get(index);
          if (object.isVisible() && (object instanceof UIKeyFocus)) {
            object.focus(keyEvent);
            break;
          }
        }
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    super.onKeyPressed(keyEvent, keyChar, keyCode);
    if (this.arrowKeyFocus == ArrowKeyFocus.VERTICAL) {
      if (keyCode == KeyEvent.VK_UP) {
        keyEvent.consume();
        keyFocus(keyEvent, -1);
      } else if (keyCode == KeyEvent.VK_DOWN) {
        keyEvent.consume();
        keyFocus(keyEvent, 1);
      }
    } else if (this.arrowKeyFocus == ArrowKeyFocus.HORIZONTAL) {
      if (keyCode == KeyEvent.VK_LEFT) {
        keyEvent.consume();
        keyFocus(keyEvent, -1);
      } else if (keyCode == KeyEvent.VK_RIGHT) {
        keyEvent.consume();
        keyFocus(keyEvent, 1);
      }
    }
  }
}
