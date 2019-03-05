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
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITriggerSource;
import heronarts.glx.ui.UITriggerTarget;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

public class UIButton extends UIParameterComponent implements UIControlTarget, UITriggerSource, UITriggerTarget, UIFocus {

  protected boolean active = false;
  protected boolean isMomentary = false;

  protected int inactiveColor = UI.get().theme.getControlBackgroundColor();
  protected int activeColor = UI.get().theme.getPrimaryColor();

  private String activeLabel = "";
  private String inactiveLabel = "";

  private VGraphics.Image activeIcon = null;
  private VGraphics.Image inactiveIcon = null;

  private boolean triggerable = false;
  protected boolean enabled = true;

  private EnumParameter<? extends Object> enumParameter = null;
  private BooleanParameter booleanParameter = null;

  private float iconOffsetX = 0, iconOffsetY = 0;

  private final LXParameterListener booleanParameterListener = new LXParameterListener() {
    public void onParameterChanged(LXParameter p) {
      setActive(booleanParameter.isOn(), false);
    }
  };

  private final LXParameterListener enumParameterListener = new LXParameterListener() {
    public void onParameterChanged(LXParameter p) {
      setLabel(enumParameter.getEnum().toString());
    }
  };

  public UIButton() {
    this(0, 0, 0, 0);
  }

  public UIButton(float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(UI.get().theme.getControlBorderColor());
    setFontColor(UI.get().theme.getControlTextColor());
    setBackgroundColor(this.inactiveColor);
  }

  public UIButton setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      redraw();
    }
    return this;
  }

  public UIButton setTriggerable(boolean triggerable) {
    this.triggerable = triggerable;
    return this;
  }

  @Override
  public String getDescription() {
    if (this.booleanParameter != null) {
      return UIParameterControl.getDescription(this.booleanParameter);
    }
    if (this.enumParameter != null) {
      return UIParameterControl.getDescription(this.enumParameter);
    }
    return super.getDescription();
  }

  @Override
  public LXListenableNormalizedParameter getParameter() {
    return (this.booleanParameter != null) ? this.booleanParameter : this.enumParameter;
  }

  private void removeParameter() {
    if (this.booleanParameter != null) {
      this.booleanParameter.removeListener(this.booleanParameterListener);
      this.booleanParameter = null;
    }
    if (this.enumParameter != null) {
      this.enumParameter.removeListener(this.enumParameterListener);
      this.enumParameter = null;
    }
  }

  public UIButton setParameter(EnumParameter<? extends Object> parameter) {
    removeParameter();
    if (parameter != null) {
      this.enumParameter = parameter;
      this.enumParameter.addListener(this.enumParameterListener);
      setActive(false);
      setMomentary(true);
      setLabel(this.enumParameter.getEnum().toString());
    }
    return this;
  }

  public UIButton setParameter(BooleanParameter parameter) {
    removeParameter();
    if (parameter != null) {
      this.booleanParameter = parameter;
      this.booleanParameter.addListener(this.booleanParameterListener);
      setMomentary(this.booleanParameter.getMode() == BooleanParameter.Mode.MOMENTARY);
      setActive(this.booleanParameter.isOn(), false);
    }
    return this;
  }

  public UIButton setMomentary(boolean momentary) {
    this.isMomentary = momentary;
    return this;
  }

  public UIButton setIconOffset(float iconOffsetX, float iconOffsetY) {
    boolean redraw = false;
    if (this.iconOffsetX != iconOffsetX) {
      this.iconOffsetX = iconOffsetX;
      redraw = true;
    }
    if (this.iconOffsetY != iconOffsetY) {
      this.iconOffsetY = iconOffsetY;
      redraw = true;
    }
    if (redraw) {
      redraw();
    }
    return this;
  }

  public UIButton setIconOffsetX(float iconOffsetX) {
    if (this.iconOffsetX != iconOffsetX) {
      this.iconOffsetX = iconOffsetX;
      redraw();
    }
    return this;
  }

  public UIButton setIconOffsetY(float iconOffsetY) {
    if (this.iconOffsetY != iconOffsetY) {
      this.iconOffsetY = iconOffsetY;
      redraw();
    }
    return this;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    if (!this.enabled) {
      vg.beginPath();
      vg.fillColor(ui.theme.getControlDisabledColor());
      vg.rect(1, 1, this.width-2, this.height-2);
      vg.fill();
    }

    VGraphics.Image icon = this.active ? this.activeIcon : this.inactiveIcon;
    if (icon != null) {
      if (!this.active) {
        icon.setTint(getFontColor());
      }
      vg.beginPath();
      vg.image(icon, this.width/2 - icon.width/2 + this.iconOffsetX, this.height/2 - icon.height/2 + this.iconOffsetY);
      vg.fill();
      icon.noTint();
    } else {
      String label = this.active ? this.activeLabel : this.inactiveLabel;
      if ((label != null) && (label.length() > 0)) {
        vg.fillColor(this.active ? UI.WHITE : getFontColor());
        vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
        if (this.textAlignVertical == VGraphics.Align.MIDDLE) {
          vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
          vg.beginPath();
          vg.text(this.width / 2 + this.textOffsetX, this.height / 2 + this.textOffsetY, label);
          vg.fill();
        } else {
          vg.beginPath();
          vg.textAlign(VGraphics.Align.CENTER);
          vg.text(this.width / 2 + this.textOffsetX, (int) (this.height * .75) + this.textOffsetY, label);
          vg.fill();
        }
      }
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      setActive(this.isMomentary ? true : !this.active);
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      if (this.isMomentary) {
        setActive(false);
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_SPACE) || (keyCode == KeyEvent.VK_ENTER)) {
      if (this.enabled) {
        setActive(this.isMomentary ? true : !this.active);
      }
      keyEvent.consume();
    }
  }

  @Override
  protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_SPACE) || (keyCode == KeyEvent.VK_ENTER)) {
      if (this.enabled && this.isMomentary) {
        setActive(false);
      }
      keyEvent.consume();
    }
  }

  public boolean isActive() {
    return this.active;
  }

  public UIButton setActive(boolean active) {
    return setActive(active, true);
  }

  protected UIButton setActive(boolean active, boolean pushToParameter) {
    if (this.active != active) {
      this.active = active;
      setBackgroundColor(active ? this.activeColor : this.inactiveColor);
      if (pushToParameter) {
        if (this.enumParameter != null) {
          if (active) {
            getLX().command.perform(new LXCommand.Parameter.Increment(this.enumParameter));
          }
        } else if (this.booleanParameter != null) {
          if (this.isMomentary) {
            this.booleanParameter.setValue(active);
          } else {
            getLX().command.perform(new LXCommand.Parameter.SetNormalized(this.booleanParameter, active));
          }

        }
      }
      onToggle(active);
      redraw();
    }
    return this;
  }

  public UIButton toggle() {
    return setActive(!this.active);
  }

  /**
   * Subclasses may override this to handle changes to the button's state
   *
   * @param active Whether button is active
   */
  protected void onToggle(boolean active) {
  }

  public UIButton setActiveColor(int activeColor) {
    if (this.activeColor != activeColor) {
      this.activeColor = activeColor;
      if (this.active) {
        setBackgroundColor(activeColor);
      }
    }
    return this;
  }

  public UIButton setInactiveColor(int inactiveColor) {
    if (this.inactiveColor != inactiveColor) {
      this.inactiveColor = inactiveColor;
      if (!this.active) {
        setBackgroundColor(inactiveColor);
      }
    }
    return this;
  }

  public UIButton setLabel(String label) {
    setActiveLabel(label);
    setInactiveLabel(label);
    return this;
  }

  public UIButton setActiveLabel(String activeLabel) {
    if (!this.activeLabel.equals(activeLabel)) {
      this.activeLabel = activeLabel;
      if (this.active) {
        redraw();
      }
    }
    return this;
  }

  public UIButton setInactiveLabel(String inactiveLabel) {
    if (!this.inactiveLabel.equals(inactiveLabel)) {
      this.inactiveLabel = inactiveLabel;
      if (!this.active) {
        redraw();
      }
    }
    return this;
  }

  public UIButton setIcon(VGraphics.Image icon) {
    setActiveIcon(icon);
    setInactiveIcon(icon);
    return this;
  }

  public UIButton setActiveIcon(VGraphics.Image activeIcon) {
    if (this.activeIcon != activeIcon) {
      this.activeIcon = activeIcon;
      if (this.active) {
        redraw();
      }
    }
    return this;
  }

  public UIButton setInactiveIcon(VGraphics.Image inactiveIcon) {
    if (this.inactiveIcon != inactiveIcon) {
      this.inactiveIcon = inactiveIcon;
      if (!this.active) {
        redraw();
      }
    }
    return this;
  }

  @Override
  public LXParameter getControlTarget() {
    if (isMappable()) {
      if (this.enumParameter != null) {
        if (this.enumParameter.getParent() != null) {
          return this.enumParameter;
        }
      } else {
        return getTriggerParameter();
      }
    }
    return null;
  }

  @Override
  public BooleanParameter getTriggerSource() {
    return this.triggerable ? getTriggerParameter() : null;
  }

  @Override
  public BooleanParameter getTriggerTarget() {
    return this.triggerable ? getTriggerParameter() : null;
  }

  private BooleanParameter getTriggerParameter() {
    if (this.booleanParameter != null && this.booleanParameter.getParent() != null) {
      return this.booleanParameter;
    }
    return null;
  }

}
