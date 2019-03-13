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
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.vg.VGraphics;

public class UIContextMenu extends UI2dComponent {

  private static final float DEFAULT_ROW_HEIGHT = 18;
  public static final float DEFAULT_WIDTH = 120;

  private UIContextActions.Action[] actions = new UIContextActions.Action[0];

  private int highlight = -1;
  private float rowHeight = DEFAULT_ROW_HEIGHT;
  private float padding = 0;

  public UIContextMenu(float x, float y, float w, float h) {
    super(x, y, w, h);
    setVisible(false);
    setBackgroundColor(UI.get().theme.getContextBackgroundColor());
    setBorderColor(UI.get().theme.getContextBorderColor());
  }

  public UIContextMenu setPadding(float padding) {
    if (this.padding != padding) {
      this.padding = padding;
      updateHeight();
    }
    return this;
  }

  public UIContextMenu setRowHeight(float rowHeight) {
    if (this.rowHeight != rowHeight) {
      this.rowHeight = rowHeight;
      updateHeight();
    }
    return this;
  }

  public UIContextMenu setActions(UIContextActions.Action[] actions) {
    this.actions = actions;
    updateHeight();
    return this;
  }

  private void updateHeight() {
    setHeight(this.actions.length * this.rowHeight + 2 * this.padding + 2);
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
  public void onDraw(UI ui, VGraphics vg) {
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

    if (this.highlight >= 0) {
      vg.beginPath();
      vg.rect(this.padding + 2, this.padding + 2 + this.highlight * this.rowHeight, this.width - 2 * this.padding - 4, this.rowHeight - 2, 2);
      vg.fillColor(ui.theme.getContextHighlightColor());
      vg.fill();
    }

    float yp = 1;
    for (UIContextActions.Action action : this.actions) {
      vg.beginPath();
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      vg.fillColor(ui.theme.getControlTextColor());
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
      vg.text(this.padding + 4, this.padding + yp + this.rowHeight / 2, clipTextToWidth(vg, action.getLabel(), this.width - 6 - 2 * this.padding));
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
    } else if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
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
    setHighlight((int) ((y - this.padding - 1) / this.rowHeight));
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float x, float y) {
    int index = (int) ((y - this.padding - 1) / this.rowHeight);
    if (index >= 0 && index < this.actions.length) {
      this.actions[index].onContextAction(getUI());
    }
    getUI().hideContextOverlay();
  }

}
