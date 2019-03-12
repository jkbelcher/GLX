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
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

public class UIDropMenu extends UIParameterComponent implements UIFocus, UIControlTarget, LXParameterListener {

  private DiscreteParameter parameter = null;

  public enum Direction {
    DOWN,
    UP
  };

  private Direction direction = Direction.DOWN;

  private String[] options;
  private UIContextActions.Action[] actions;

  private boolean enabled = true;

  private final UIContextMenu contextMenu;

  public UIDropMenu(float x, float y, float w, float h) {
    this(x, y, w, h, null);
  }

  public UIDropMenu(float x, float y, float w, float h, DiscreteParameter parameter) {
    super(x, y, w, h);
    this.contextMenu = new UIContextMenu(x, y, w, h);
    setParameter(parameter);
    setBackgroundColor(UI.get().theme.getControlBackgroundColor());
    setBorderColor(UI.get().theme.getControlBorderColor());
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public DiscreteParameter getParameter() {
    return this.parameter;
  }

  public UIDropMenu setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      if (!enabled) {
        setExpanded(false);
      }
      redraw();
    }
    return this;
  }

  public UIDropMenu setParameter(DiscreteParameter parameter) {
    if (this.parameter != null) {
      this.parameter.removeListener(this);
    }
    this.parameter = parameter;
    this.actions = new UIContextActions.Action[parameter.getRange()];
    for (int i = 0; i < this.actions.length; ++i) {
      final int ii = i;
      this.actions[i] = new UIContextActions.Action(String.valueOf(i)) {
        @Override
        public void onContextAction(UI ui) {
          getLX().command.perform(new LXCommand.Parameter.SetValue(parameter, ii));
        }
      };
    }
    setOptions(this.parameter.getOptions());
    this.contextMenu.setActions(this.actions);
    this.contextMenu.setHighlight(this.parameter.getValuei());
    this.parameter.addListener(this);
    redraw();
    return this;
  }

  public void onParameterChanged(LXParameter p) {
    this.contextMenu.setHighlight(this.parameter.getValuei());
    redraw();
  }

  /**
   * Sets the direction that this drop menu opens, up or down
   *
   * @param direction Direction menu should open
   * @return this
   */
  public UIDropMenu setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Sets the list of string options to display in the menu
   *
   * @param options Options array
   * @return this
   */
  public UIDropMenu setOptions(String[] options) {
    this.options = options;
    for (int i = 0; i < options.length; ++i) {
      this.actions[i].setLabel(options[i]);
    }
    return this;
  }

  @Override
  protected int getFocusSize() {
    return 4;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if (!this.enabled) {
      vg.fillColor(ui.theme.getControlDisabledColor());
      vg.beginPath();
      vg.rect(1, 1, this.width-2, this.height-2);
      vg.fill();
    }

    String text;
    if (this.options != null) {
      text = this.options[this.parameter.getValuei()];
    } else {
      text = Integer.toString(this.parameter.getValuei());
    }

    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    vg.fillColor(this.enabled ? ui.theme.getControlTextColor() : ui.theme.getControlDisabledTextColor());
    vg.beginPath();
    vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
    vg.text(4 + this.textOffsetX, this.height / 2 + this.textOffsetY, clipTextToWidth(vg, text, this.width - 12));
    vg.textAlign(VGraphics.Align.RIGHT, VGraphics.Align.MIDDLE);
    vg.text(this.width-4, this.height / 2 + this.textOffsetY, "\u25BC");
    vg.fill();
  }

  private void toggleExpanded() {
    setExpanded(!this.contextMenu.isVisible());
  }

  private void setExpanded(boolean expanded) {
    if (this.contextMenu.isVisible() != expanded) {
      if (expanded) {
        this.contextMenu.setHighlight(this.parameter.getValuei());
        if (this.direction == Direction.UP) {
          this.contextMenu.setPosition(this, 0, -this.contextMenu.getHeight());
        } else {
          this.contextMenu.setPosition(this, 0, this.height);
        }
        this.contextMenu.setWidth(this.width);
        getUI().showContextOverlay(this.contextMenu);
      } else {
        getUI().hideContextOverlay();
      }
    }
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float x, float y) {
    if (this.enabled) {
      toggleExpanded();
    }
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.enabled) {
      if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
        keyEvent.consume();
        toggleExpanded();
      } else if (keyCode == KeyEvent.VK_DOWN) {
        keyEvent.consume();
        getLX().command.perform(new LXCommand.Parameter.Increment(this.parameter));
      } else if (keyCode == KeyEvent.VK_UP) {
        keyEvent.consume();
        getLX().command.perform(new LXCommand.Parameter.Decrement(this.parameter));
      } else if (keyCode == KeyEvent.VK_ESCAPE) {
        if (this.contextMenu.isVisible()) {
          keyEvent.consume();
          setExpanded(false);
        }
      }
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