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
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;

public class UIContextButton extends UI2dComponent implements UIFocus {

  private String label = "";
  private final UIContextMenu contextMenu;

  public UIContextButton(float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(UI.get().theme.getControlBorderColor());
    setFontColor(UI.get().theme.getControlTextColor());
    setBackgroundColor(UI.get().theme.getControlBackgroundColor());
    this.contextMenu = new UIContextMenu(0, 0, UIContextMenu.DEFAULT_WIDTH, 0);
  }

  public UIContextButton setContextActions(UIContextActions.Action[] contextActions) {
    this.contextMenu.setActions(contextActions);
    return this;
  }

  public UIContextButton setLabel(String label) {
    if (!this.label.equals(label)) {
      this.label = label;
      redraw();
    }
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if ((this.label != null) && (this.label.length() > 0)) {
      int fontColor = this.mouseDown ? UI.WHITE : getFontColor();
      vg.fillColor(fontColor);
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      if (this.textAlignVertical == VGraphics.Align.MIDDLE) {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
        vg.text(this.width / 2 + this.textOffsetX, this.height / 2 + this.textOffsetY, this.label);
        vg.fill();
      } else {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER);
        vg.text(this.width / 2 + this.textOffsetX, (int) (this.height * .75) + this.textOffsetY, this.label);
        vg.fill();
      }
    }
  }

  private boolean mouseDown = false;

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    showMenu();
    this.mouseDown = true;
    redraw();
  }

  @Override
  public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    this.mouseDown = false;
    redraw();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.contextMenu.isVisible()) {
      keyEvent.consume();
      this.contextMenu.onKeyPressed(keyEvent, keyChar, keyCode);
    } else if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
      keyEvent.consume();
      showMenu();
    }
  }

  private void showMenu() {
    this.contextMenu.setWidth(Math.max(UIContextMenu.DEFAULT_WIDTH, this.width));
    this.contextMenu.setPosition(this, 0, this.height);
    this.contextMenu.setHighlight(0);
    getUI().showContextOverlay(this.contextMenu);
  }
}
