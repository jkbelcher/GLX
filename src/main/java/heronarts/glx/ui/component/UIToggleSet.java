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

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXUtils;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

public class UIToggleSet extends UIParameterComponent implements UIFocus, UIControlTarget, LXParameterListener {

  private String[] options = null;

  private int[] boundaries = null;

  private int value = -1;

  private boolean evenSpacing = false;

  private DiscreteParameter parameter = null;

  public UIToggleSet() {
    this(0, 0, 0, 0);
  }

  public UIToggleSet(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  protected void onResize() {
    computeBoundaries();
  }

  public UIToggleSet setOptions(String[] options) {
    if (this.options != options) {
      this.options = options;
      this.value = 0;
      this.boundaries = new int[options.length];
      computeBoundaries();
      redraw();
    }
    return this;
  }

  @Override
  public DiscreteParameter getParameter() {
    return this.parameter;
  }

  public UIToggleSet setParameter(DiscreteParameter parameter) {
    if (this.parameter != parameter) {
      if (this.parameter != null) {
        this.parameter.removeListener(this);
      }
      this.parameter = parameter;
      if (this.parameter != null) {
        this.parameter.addListener(this);
        String[] options = this.parameter.getOptions();
        if (options != null) {
          setOptions(options);
        }
        setValue(this.parameter.getValuei(), false);
      }
    }
    return this;
  }

  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.parameter) {
      setValue(this.parameter.getValuei(), false);
    }
  }

  private void computeBoundaries() {
    if (this.boundaries == null) {
      return;
    }
    if (this.evenSpacing) {
      for (int i = 0; i < this.boundaries.length; ++i) {
        this.boundaries[i] = (int) ((i + 1) * (this.width-1) / this.boundaries.length);
      }
    } else {
      int totalLength = 0;
      for (String s : this.options) {
        totalLength += s.length();
      }
      int lengthSoFar = 0;
      for (int i = 0; i < this.options.length; ++i) {
        lengthSoFar += this.options[i].length();
        this.boundaries[i] = (int) (lengthSoFar * (this.width-1) / totalLength);
      }
    }
  }

  public UIToggleSet setEvenSpacing() {
    if (!this.evenSpacing) {
      this.evenSpacing = true;
      computeBoundaries();
      redraw();
    }
    return this;
  }

  public int getValueIndex() {
    return this.value;
  }

  public String getValue() {
    return this.options[this.value];
  }

  public UIToggleSet setValue(String value) {
    for (int i = 0; i < this.options.length; ++i) {
      if (this.options[i] == value) {
        return setValue(i);
      }
    }

    // That string doesn't exist
    String optStr = "{";
    for (String str : this.options) {
      optStr += str + ",";
    }
    optStr = optStr.substring(0, optStr.length() - 1) + "}";
    throw new IllegalArgumentException("Not a valid option in UIToggleSet: "
        + value + " " + optStr);
  }

  public UIToggleSet setValue(int value) {
    return setValue(value, true);
  }

  private UIToggleSet setValue(int value, boolean pushToParameter) {
    if (this.value != value) {
      if (value < 0 || value >= this.options.length) {
        throw new IllegalArgumentException("Invalid index to setValue(): "
            + value);
      }
      this.value = value;
      if (this.parameter != null && pushToParameter) {
        getLX().command.perform(new LXCommand.Parameter.SetValue(this.parameter, value));
      }
      onToggle(this.value);
      redraw();
    }
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.fillColor(ui.theme.getControlBackgroundColor());
    vg.strokeColor(ui.theme.getControlBorderColor());
    vg.rect(.5f, .5f, this.width-1, this.height-1);
    vg.fill();
    vg.stroke();

    vg.beginPath();
    vg.strokeColor(ui.theme.getControlBorderColor());
    for (int b : this.boundaries) {
      vg.line(b + .5f, .5f, b + .5f, this.height - 1);
    }
    vg.stroke();

    // Active item
    if (this.value >= 0) {
      int leftBoundary = (this.value > 0) ? this.boundaries[this.value - 1] : 0;
      int rightBoundary = this.boundaries[this.value];
      vg.beginPath();
      vg.fillColor(ui.theme.getPrimaryColor());
      vg.rect(leftBoundary + 1, 1, rightBoundary - leftBoundary - 1, this.height - 2);
      vg.fill();
    }

    // Text labels
    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
    vg.beginPath();
    int leftBoundary = 0;
    for (int i = 0; i < this.options.length; ++i) {
      boolean isActive = (i == this.value);
      vg.fillColor(isActive ? UI.WHITE : ui.theme.getControlTextColor());
      vg.text((leftBoundary + this.boundaries[i]) / 2.f, this.height/2, this.options[i]);
      leftBoundary = this.boundaries[i];
    }
    vg.fill();
  }

  protected void onToggle(int value) {
    onToggle(getValue());
  }

  protected void onToggle(String option) {
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    for (int i = 0; i < this.boundaries.length; ++i) {
      if (mx < this.boundaries[i]) {
        setValue(i);
        break;
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_DOWN)) {
      keyEvent.consume();
      setValue(LXUtils.constrain(this.value - 1, 0, this.options.length - 1));
    } else if ((keyCode == KeyEvent.VK_RIGHT) || (keyCode == KeyEvent.VK_UP)) {
      keyEvent.consume();
      setValue(LXUtils.constrain(this.value + 1, 0, this.options.length - 1));
    }
  }

  @Override
  public LXParameter getControlTarget() {
    if (isMappable() && this.parameter != null && this.parameter.getParent() != null) {
      return this.parameter;
    }
    return null;
  }

}
