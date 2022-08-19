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
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameter;

public abstract class UIInputBox extends UIParameterComponent implements UIFocus {

  public interface ProgressIndicator {
    public boolean hasProgress();
    public double getProgress();
  }

  private static final int TEXT_MARGIN = 2;

  protected boolean enabled = true;
  protected boolean editable = true;

  protected boolean mouseEditable = true;

  protected boolean editing = false;
  protected String editBuffer = "";

  protected boolean hasFill = false;
  protected int fillColor = 0;

  protected boolean returnKeyEdit = true;

  private boolean immediateEdit = false;
  private boolean mousePressEdit = false;

  private ProgressIndicator progressMeter;
  private int progressPixels = 0;
  private boolean hasProgressColor = false;
  private int progressColor = 0;

  protected FillStyle fillStyle = FillStyle.UNDERLINE;

  public enum FillStyle {
    UNDERLINE,
    FULL
  };

  protected UIInputBox() {
    this(0, 0, 0, 0);
  }

  protected UIInputBox(float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(UI.get().theme.getControlBorderColor());
    setBackgroundColor(UI.get().theme.getControlBackgroundColor());
    setTextAlignment(VGraphics.Align.CENTER);
  }

  public UIInputBox setProgressColor(boolean hasProgressColor) {
    this.hasProgressColor = hasProgressColor;
    redraw();
    return this;
  }

  public UIInputBox setProgressColor(int progressColor) {
    this.hasProgressColor = true;
    this.progressColor = progressColor;
    redraw();
    return this;
  }

  public UIInputBox setProgressIndicator(ProgressIndicator meter) {
    boolean addLoopTask = this.progressMeter == null;
    this.progressMeter = meter;
    if (addLoopTask) {
      addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
        @Override
        public void run() {
          if (progressMeter != null && progressMeter.hasProgress()) {
            int newProgress = (int) (progressMeter.getProgress() * (width-5));
            if (newProgress != progressPixels) {
              progressPixels = newProgress;
              redraw();
            }
          } else {
            if (progressPixels != 0) {
              progressPixels = 0;
              redraw();
            }
          }
        }
      });
    }
    return this;
  }

  public UIInputBox setReturnKeyEdit(boolean returnKeyEdit) {
    this.returnKeyEdit = returnKeyEdit;
    return this;
  }

  public UIInputBox enableImmediateEdit(boolean immediateEdit) {
    this.immediateEdit = immediateEdit;
    return this;
  }

  public UIInputBox enableMousePressEdit(boolean mousePressEdit) {
    this.mousePressEdit = mousePressEdit;
    return this;
  }

  protected abstract String getValueString();

  protected abstract void saveEditBuffer();

  /**
   * Subclasses may override to handle editing changes
   *
   * @param editBuffer New value being actively edited
   */
  protected /* abstract */ void onEditChange(String editBuffer) {}

  public boolean isEditable() {
    return this.editable;
  }

  public UIInputBox setEditable(boolean editable) {
    if (this.editable != editable) {
      if (this.editing) {
        this.editing = false;
        redraw();
      }
      this.editable = editable;
    }
    return this;
  }

  public UIInputBox setMouseEditable(boolean mouseEditable) {
    this.mouseEditable = mouseEditable;
    return this;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public UIInputBox setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      if (this.editing && !this.enabled) {
        this.editing = false;
      }
      redraw();
    }
    return this;
  }

  public void edit() {
    edit(getEditBufferValue());
  }

  public void edit(String editBufferValue) {
    if (!this.editable) {
      throw new IllegalStateException("May not edit a non-editable UIInputBox");
    }
    if (this.enabled && !this.editing) {
      this.editing = true;
      this.editBuffer = editBufferValue;
    }
    redraw();
  }

  protected String getEditBufferValue() {
    return "";
  }

  @Override
  protected void onBlur() {
    super.onBlur();
    if (this.editing) {
      this.editing = false;
      saveEditBuffer();
      redraw();
    }
  }

  protected double getFillWidthNormalized() {
    return 0;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    if (this.progressPixels > 0) {
      vg.beginPath();
      vg.line(2, this.height-2, 2 + this.progressPixels, this.height-2);
      vg.strokeColor(this.hasProgressColor ? this.progressColor : ui.theme.getPrimaryColor());
      vg.stroke();
    }

    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    if (this.editing) {
      vg.beginPath();
      vg.fillColor(0, 0, 0);
      vgRoundedRect(vg);
      vg.fill();
    } else {
      if (!this.enabled) {
        vg.beginPath();
        vg.fillColor(ui.theme.getControlDisabledColor());
        vg.rect(1, 1, this.width-2, this.height-2);
        vg.fill();
      }
      if (this.hasFill) {
        if (this.fillStyle == FillStyle.UNDERLINE) {
          int fillWidth = (int) (getFillWidthNormalized() * (this.width-5));
          if (fillWidth > 0) {
            vg.beginPath();
            vg.strokeColor(this.fillColor);
            vg.line(2, this.height-2, 2 + fillWidth, this.height-2);
            vg.stroke();
          }
        } else if (this.fillStyle == FillStyle.FULL) {
          int fillWidth = (int) (getFillWidthNormalized() * (this.width-2));
          vg.beginPath();
          vg.fillColor(this.fillColor);
          vg.rect(1, 1, fillWidth, this.height - 2);
          vg.fill();
        }
      }
    }

    if (this.editing) {
      vg.fillColor(ui.theme.getPrimaryColor());
    } else if (!this.enabled) {
      vg.fillColor(ui.theme.getControlDisabledTextColor());
    } else {
      vg.fillColor(hasFontColor() ? getFontColor() : ui.theme.getControlTextColor());
    }

    String displayString = this.editing ? this.editBuffer : getValueString();
    if (displayString != null) {
      displayString = clipTextToWidth(vg, displayString, this.width - TEXT_MARGIN);
      if (this.textAlignHorizontal == VGraphics.Align.LEFT) {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
        vg.text(2, this.height / 2 + 1, displayString);
        vg.fill();
      } else {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
        vg.text(this.width / 2, this.height / 2 + 1, displayString);
        vg.fill();
      }
    }
  }

  protected abstract boolean isValidCharacter(char keyChar);

  /**
   * Subclasses may optionally override to decrement value in response to arrows.
   * Decrement is invoked for the left or down arrow keys.
   *
   * @param keyEvent Key event
   */
  protected void decrementValue(KeyEvent keyEvent) {}

  /**
   * Subclasses may optionally override to decrement value in response to arrows.
   * Increment is invoked for the right or up keys.
   *
   * @param keyEvent Key event
   */
  protected void incrementValue(KeyEvent keyEvent) {}

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.editable) {
      if (this.editing) {
        // Editing!
        if (isValidCharacter(keyChar)) {
          if (!keyEvent.isCommand()) {
            keyEvent.consume();
            this.editBuffer += keyChar;
            onEditChange(this.editBuffer);
            redraw();
          }
        } else if (keyEvent.isEnter()) {
          keyEvent.consume();
          this.editing = false;
          saveEditBuffer();
          redraw();
        } else if (keyCode == KeyEvent.VK_BACKSPACE) {
          keyEvent.consume();
          if (this.editBuffer.length() > 0) {
            if (keyEvent.isShiftDown() || keyEvent.isControlDown() || keyEvent.isMetaDown()) {
              this.editBuffer = "";
              onEditChange(this.editBuffer);
            } else {
              this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
              onEditChange(this.editBuffer);
            }
            redraw();
          }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          keyEvent.consume();
          this.editing = false;
          onEditChange(getValueString());
          redraw();
        }
      } else if (this.enabled) {
        // Not editing
        if (this.immediateEdit && isValidCharacter(keyChar) && !keyEvent.isCommand()) {
          keyEvent.consume();
          edit(Character.toString(keyChar));
          onEditChange(this.editBuffer);
        } else if (this.immediateEdit && keyCode == KeyEvent.VK_BACKSPACE) {
          String editBuffer = getEditBufferValue();
          if (!editBuffer.isEmpty()) {
            keyEvent.consume();
            edit(editBuffer.substring(0, editBuffer.length() - 1));
            onEditChange(this.editBuffer);
          }
        } else if (keyEvent.isEnter()) {
          if (this.returnKeyEdit) {
            keyEvent.consume();
            edit();
          }
        } else if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_DOWN)) {
          decrementValue(keyEvent);
        } else if ((keyCode == KeyEvent.VK_RIGHT) || (keyCode == KeyEvent.VK_UP)) {
          incrementValue(keyEvent);
        }
      }
    }
  }

  /**
   * Subclasses may optionally implement to change value based upon mouse click+drag in the box.
   *
   * @param mouseEvent Mouse event
   * @param offset Units of mouse movement, positive or negative
   */
  protected void incrementMouseValue(MouseEvent mouseEvent, int offset) {}

  private float dAccum = 0;

  private LXCommand.Parameter.SetValue mouseDragSetValue = null;

  protected void setValueCommand(double value) {
    if (this.mouseDragSetValue != null) {
      getLX().command.perform(this.mouseDragSetValue.update(value));
    } else if (getParameter() != null){
      if (this.useCommandEngine) {
        getLX().command.perform(new LXCommand.Parameter.SetValue(getParameter(), value));
      } else {
        getParameter().setValue(value);
      }
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    this.dAccum = 0;
    LXParameter parameter = getParameter();
    if (parameter != null) {
      if (this.useCommandEngine) {
        this.mouseDragSetValue = new LXCommand.Parameter.SetValue(parameter, 0);
      }
    }
    if (this.enabled && this.editable && !this.editing && this.mousePressEdit) {
      mouseEvent.consume();
      edit();
      redraw();
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    this.mouseDragSetValue = null;
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (this.enabled && this.editable && this.mouseEditable) {
      mouseEvent.consume();
      this.dAccum -= dy;
      int offset = (int) (this.dAccum / 5);
      this.dAccum = this.dAccum - (offset * 5);
      if (!this.editing) {
        incrementMouseValue(mouseEvent, offset);
      }
    }
  }

}
