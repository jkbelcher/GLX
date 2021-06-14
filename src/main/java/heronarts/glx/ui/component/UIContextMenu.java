/**
 * Copyright 2018- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dScrollContext;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.vg.VGraphics;

public class UIContextMenu extends UI2dContainer {

  private static final float DEFAULT_ROW_HEIGHT = 18;
  public static final float DEFAULT_WIDTH = 120;

  private UIContextActions.Action[] actions = new UIContextActions.Action[0];

  private int highlight = -1;
  private float rowHeight = DEFAULT_ROW_HEIGHT;
  private float padding = 0;
  private float maxHeight = -1;
  private float fullHeight = 0;
  private boolean scrollMode = false;

  private UI2dScrollContext scrollPane = null;

  public UIContextMenu(float x, float y, float w, float h) {
    super(x, y, w, h);
    setVisible(false);
    setBackgroundColor(UI.get().theme.getContextBackgroundColor());
    setBorderColor(UI.get().theme.getContextBorderColor());
  }

  @Override
  public UIContextMenu setPadding(float padding) {
    if (this.padding != padding) {
      this.padding = padding;
      updateSize();
    }
    return this;
  }

  public UIContextMenu setRowHeight(float rowHeight) {
    if (this.rowHeight != rowHeight) {
      this.rowHeight = rowHeight;
      updateSize();
    }
    return this;
  }

  public UIContextMenu setMaxHeight(float maxHeight) {
    this.maxHeight = maxHeight;
    updateSize();
    return this;
  }

  private void setScrollMode(boolean scrollMode) {
    this.scrollMode = scrollMode;
    if (this.scrollMode) {
      if (this.scrollPane == null) {
        this.scrollPane = new UI2dScrollContext(UI.get(), (int) this.padding, (int) this.padding, (int) (this.width - 2*this.padding), (int) (this.maxHeight - 2*this.padding)) {
          @Override
          protected void onDraw(UI ui, VGraphics vg) {
            drawItems(ui, vg, this.width, 0);
          }
        };
        this.scrollPane.setScrollHeight(this.fullHeight - 2*this.padding);
        this.scrollPane.addToContainer(this);
      } else {
        this.scrollPane.setSize(this.width - 2*this.padding, this.maxHeight - 2*this.padding);
        this.scrollPane.setPosition(this.padding, this.padding);
        this.scrollPane.setScrollHeight(this.fullHeight - 2*this.padding);
        this.scrollPane.setVisible(true);
      }
    } else {
      if (this.scrollPane != null) {
        this.scrollPane.setVisible(false);
      }
    }
  }

  public UIContextMenu setActions(UIContextActions.Action[] actions) {
    this.actions = actions;
    updateSize();
    return this;
  }

  private void updateSize() {
    this.fullHeight = this.actions.length * this.rowHeight + 2 * this.padding + 2;
    if ((this.maxHeight > 0) && (this.fullHeight > this.maxHeight)) {
      setHeight(this.maxHeight);
      setScrollMode(true);
    } else {
      setHeight(this.fullHeight);
      setScrollMode(false);
    }
  }

  public UIContextMenu setHighlight(int highlight) {
    if (this.highlight != highlight) {
      if (highlight >= 0 && highlight < this.actions.length) {
        this.highlight = highlight;
      } else {
        this.highlight = -1;
      }
      redraw();
    }
    return this;
  }

  /**
   * Subclasses may override to draw some other kind of drop menu
   *
   * @param ui UI context
   * @param vg PGraphics context
   */
  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    if (this.padding > 0) {
      vg.beginPath();
      vg.fillColor(ui.theme.getDeviceFocusedBackgroundColor());
      vg.rect(0, 0, this.width, this.height, getBorderRounding());
      vg.fill();
      vg.beginPath();
      vg.fillColor(getBackgroundColor());
      vg.rect(this.padding, this.padding, this.width - 2 * this.padding, this.height - 2*this.padding, 2);
      vg.fill();
    }

    if (!this.scrollMode) {
      drawItems(ui, vg, this.width, this.padding);
    }
  }

  private void drawItems(UI ui, VGraphics vg, float width, float padding) {

    if (this.highlight >= 0) {
      vg.beginPath();
      vg.rect(padding + 2, padding + 2 + this.highlight * this.rowHeight, width - 2 * padding - 4, this.rowHeight - 2, 2);
      vg.fillColor(ui.theme.getContextHighlightColor());
      vg.fill();
    }

    float yp = 1;
    for (UIContextActions.Action action : this.actions) {
      vg.beginPath();
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      vg.fillColor(ui.theme.getControlTextColor());
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
      vg.text(padding + 4, padding + yp + this.rowHeight / 2, clipTextToWidth(vg, action.getLabel(), width - 6 - 2 * padding));
      vg.fill();
      yp += this.rowHeight;
    }


  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyCode == KeyEvent.VK_UP) {
      keyEvent.consume();
      setHighlight((this.highlight + this.actions.length - 1) % this.actions.length);
    } else if (keyCode == KeyEvent.VK_DOWN) {
      keyEvent.consume();
      setHighlight((this.highlight + 1) % this.actions.length);
    } else if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      if (this.highlight >= 0) {
        this.actions[this.highlight].onContextAction(getUI());
      }
      getUI().hideContextOverlay();
    } else if (keyCode == KeyEvent.VK_ESCAPE) {
      keyEvent.consume();
      getUI().hideContextOverlay();
    }
  }

  @Override
  public void onMouseOut(MouseEvent mouseEvent) {
    setHighlight(-1);
  }

  @Override
  public void onMouseMoved(MouseEvent mouseEvent, float x, float y) {
    float scrollY = this.scrollMode ? this.scrollPane.getScrollY() : 0;
    setHighlight((int) ((y - scrollY - this.padding - 1) / this.rowHeight));
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float x, float y) {
    float scrollY = this.scrollMode ? this.scrollPane.getScrollY() : 0;
    int index = (int) ((y - scrollY - this.padding - 1) / this.rowHeight);
    if (index >= 0 && index < this.actions.length) {
      this.actions[index].onContextAction(getUI());
    }
    getUI().hideContextOverlay();
  }

}
