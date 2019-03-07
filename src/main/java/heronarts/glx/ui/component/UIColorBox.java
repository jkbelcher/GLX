/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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
import heronarts.glx.ui.UIFocus;
import heronarts.lx.color.ColorParameter;

public class UIColorBox extends UI2dComponent implements UIFocus {

  private final ColorParameter parameter;

  public UIColorBox(UI ui, final ColorParameter parameter, float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(ui.theme.getControlBorderColor());
    setBackgroundColor(parameter.getColor());
    this.parameter = parameter;
    parameter.addListener((p) -> {
      setBackgroundColor(parameter.getColor());
    });
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (mouseEvent.getCount() == 2) {
      this.parameter.hue.setValue(Math.random() * 360);
    }
  }

  @Override
  public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    mouseEvent.consume();
    this.parameter.hue.setValue((parameter.hue.getValue() + 360 + 2*dx + 2*dy) % 360);
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_DOWN) {
      this.parameter.hue.setValue((this.parameter.hue.getValue() + 300) % 360);
    } else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_UP) {
      this.parameter.hue.setValue((this.parameter.hue.getValue() + 60) % 360);
    } else if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
      this.parameter.hue.setValue(Math.random() * 360);
    }
  }
}