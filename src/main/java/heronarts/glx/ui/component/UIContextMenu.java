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

  private static final float ROW_HEIGHT = 18;
  public static final float DEFAULT_WIDTH = 120;

  private UIContextActions.Action[] actions;

  private int highlight = -1;

  public UIContextMenu(float x, float y, float w, float h) {
    super(x, y, w, h);
    setVisible(false);
    setBackgroundColor(UI.get().theme.getContextBackgroundColor());
    setBorderColor(UI.get().theme.getContextBorderColor());
  }

  public UIContextMenu setActions(UIContextActions.Action[] actions) {
    this.actions = actions;
    setHeight(this.actions.length * ROW_HEIGHT);
    return this;
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
    if (this.highlight >= 0) {
      vg.beginPath();
      vg.rect(0, this.highlight * ROW_HEIGHT, this.width, ROW_HEIGHT);
      vg.fillColor(ui.theme.getContextHighlightColor());
      vg.fill();
    }

    float yp = 0;
    for (UIContextActions.Action action : this.actions) {
      vg.beginPath();
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      vg.fillColor(ui.theme.getControlTextColor());
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
      vg.text(4, yp + ROW_HEIGHT / 2, clipTextToWidth(vg, action.getLabel(), this.width - 6));
      vg.fill();
      yp += ROW_HEIGHT;
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
    setHighlight((int) (y / ROW_HEIGHT));
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float x, float y) {
    int index = (int) (y / ROW_HEIGHT);
    if (index >= 0 && index < this.actions.length) {
      this.actions[index].onContextAction(getUI());
    }
    getUI().hideContextOverlay();
  }

}
