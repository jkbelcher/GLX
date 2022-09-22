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
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;

public abstract class UIInputBox extends UIParameterComponent implements UIFocus {

  public interface ProgressIndicator {
    public boolean hasProgress();
    public double getProgress();
  }

  private static final int TEXT_MARGIN = 2;

  protected boolean enabled = true;

  protected boolean editable = true;
  protected boolean mouseEditable = true;

  protected boolean returnKeyEdit = true;
  private boolean immediateEdit = false;
  private boolean mousePressEdit = false;

  protected boolean editing = false;

  private String editBuffer = "";
  private int editCursor = -1;
  private int editRangeStart = -1;
  private int editRangeEnd = -1;

  private double editCursorBasis = 1;

  protected boolean hasFill = false;
  protected int fillColor = 0;

  private ProgressIndicator progressMeter;
  private int progressPixels = 0;
  private boolean hasProgressColor = false;
  private UIColor progressColor = UIColor.NONE;

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
    setBorderColor(UI.get().theme.controlBorderColor);
    setBackgroundColor(UI.get().theme.controlBackgroundColor);
    setTextAlignment(VGraphics.Align.CENTER);

    // Animate cursor while editing
    addLoopTask(deltaMs -> {
      if (this.editing) {
        this.editCursorBasis = (this.editCursorBasis + deltaMs * .002f) % 1.;
        redraw();
      }
    });
  }

  public UIInputBox setProgressColor(boolean hasProgressColor) {
    this.hasProgressColor = hasProgressColor;
    redraw();
    return this;
  }

  public UIInputBox setProgressColor(int progressColor) {
    return setProgressColor(new UIColor(progressColor));
  }

  public UIInputBox setProgressColor(UIColor progressColor) {
    if (!this.hasProgressColor || (this.progressColor != progressColor)) {
      this.hasProgressColor = true;
      this.progressColor = progressColor;
      redraw();
    }
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

  protected abstract void saveEditBuffer(String editBuffer);

  protected String getEditBuffer() {
    return this.editBuffer;
  }

  protected String getEditRange() {
    if (this.editRangeStart != this.editRangeEnd) {
      return this.editBuffer.substring(this.editRangeStart, this.editRangeEnd);
    }
    return this.editBuffer;
  }

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
    edit(getInitialEditBufferValue());
  }

  public void edit(String editBufferValue) {
    if (!this.editable) {
      throw new IllegalStateException("May not edit a non-editable UIInputBox");
    }
    if (this.enabled && !this.editing) {
      this.editing = true;
      this.editBuffer = editBufferValue;
      editCursor(this.editBuffer.length());
    }
    redraw();
  }

  private void editAppend(char append) {
    this.editBuffer =
      this.editBuffer.substring(0, this.editRangeStart) +
      append +
      this.editBuffer.substring(this.editRangeEnd);
    editCursor(this.editRangeEnd + 1);
    onEditChange(this.editBuffer);
    redraw();
  }

  protected void editAppend(String append) {
    this.editBuffer =
      this.editBuffer.substring(0, this.editRangeStart) +
      append +
      this.editBuffer.substring(this.editRangeEnd);
    editCursor(this.editRangeEnd + append.length());
    onEditChange(this.editBuffer);
    redraw();
  }

  private void editRange(int cursor) {
    cursor = LXUtils.constrain(cursor, 0, this.editBuffer.length());
    if (this.editRangeStart == this.editRangeEnd) {
      // No range selected, now we're starting a range
      if (cursor > this.editRangeStart) {
        this.editCursor = this.editRangeEnd = cursor;
      } else {
        this.editCursor = this.editRangeStart = cursor;
      }
    } else if (this.editCursor == this.editRangeStart) {
      if (cursor < this.editRangeEnd) {
        this.editCursor = this.editRangeStart = cursor;
      } else {
        this.editCursor = this.editRangeEnd = cursor;
      }
    } else {
      if (cursor > this.editRangeStart) {
        this.editCursor = this.editRangeEnd = cursor;
      } else {
        this.editCursor = this.editRangeStart = cursor;
      }
    }
    if (this.editRangeStart > this.editRangeEnd) {
      throw new IllegalStateException("Cannot have editRangeStart(" + editRangeStart + ") > editRangeEnd(" + editRangeEnd + ")");
    }
  }

  private void editCursor(int cursor) {
    this.editRangeStart = this.editRangeEnd = this.editCursor = LXUtils.constrain(cursor, 0, this.editBuffer.length());
  }

  protected String getInitialEditBufferValue() {
    return "";
  }

  @Override
  protected void onBlur() {
    super.onBlur();
    if (this.editing) {
      this.editing = false;
      saveEditBuffer(this.editBuffer);
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
      vg.strokeColor(this.hasProgressColor ? this.progressColor : ui.theme.primaryColor);
      vg.stroke();
    }

    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    if (this.editing) {
      vg.beginPath();
      vg.fillColor(ui.theme.editTextBackgroundColor);
      vgRoundedRect(vg);
      vg.fill();
    } else {
      if (!this.enabled) {
        vg.beginPath();
        vg.fillColor(ui.theme.controlDisabledColor);
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
      vg.fillColor(ui.theme.editTextColor);
    } else if (!this.enabled) {
      vg.fillColor(ui.theme.controlDisabledTextColor);
    } else {
      vg.fillColor(hasFontColor() ? getFontColor() : ui.theme.controlTextColor);
    }

    final float leftPadding = 2;

    String displayString = this.editing ? this.editBuffer : getValueString();
    if (displayString != null) {
      displayString = clipTextToWidth(vg, displayString, this.width - TEXT_MARGIN);
      if (this.textAlignHorizontal == VGraphics.Align.LEFT) {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
        vg.text(leftPadding, this.height / 2 + 1, displayString);
        vg.fill();
      } else {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
        vg.text(this.width / 2, this.height / 2 + 1, displayString);
        vg.fill();
      }
    }

    if (this.editing) {
      // Draw cursor, clamp range as redraw may happen after string is updated but before cursor moved
      final int length = (displayString != null) ? displayString.length() : 0;
      final int editCursor = LXUtils.constrain(this.editCursor, 0, length);
      final int editRangeStart = LXUtils.constrain(this.editRangeStart, 0, length);
      final int editRangeEnd = LXUtils.constrain(this.editRangeEnd, 0, length);

      float fullWidth = 0;
      if (this.textAlignHorizontal != VGraphics.Align.LEFT) {
        fullWidth = (displayString != null) ? vg.textWidth(displayString) : 0;
      }

      if (editRangeStart != editRangeEnd) {
        float rangeStartWidth = 0, rangeEndWidth = 0;
        if (displayString != null) {
          if (editRangeStart != 0) {
            rangeStartWidth = vg.textWidth(displayString.substring(0, editRangeStart));
          }
          if (editRangeEnd != 0) {
            rangeEndWidth = vg.textWidth(displayString.substring(0, editRangeEnd));
          }
        }
        float rangeStartX = 0, rangeEndX = 0;
        if (this.textAlignHorizontal == VGraphics.Align.LEFT) {
          rangeStartX = leftPadding + rangeStartWidth;
          rangeEndX = leftPadding + rangeEndWidth;
        } else {
          rangeStartX = (this.width - fullWidth) / 2f + rangeStartWidth;
          rangeEndX = (this.width - fullWidth) / 2f + rangeEndWidth;
        }

        vg.beginPath();
        vg.fillColor(ui.theme.editTextColor.mask(0x55));
        vg.rect(rangeStartX, 1, rangeEndX - rangeStartX, this.height - 2);
        vg.fill();
      }

      float cursorWidth = 0, cursorX = 0;
      if ((displayString != null) && (editCursor > 0)) {
        cursorWidth = vg.textWidth(displayString.substring(0, editCursor));
      }
      if (this.textAlignHorizontal == VGraphics.Align.LEFT) {
        cursorX = leftPadding + cursorWidth;
      } else {
        cursorX = (this.width - fullWidth) / 2f + cursorWidth;
      }

      vg.beginPath();
      vg.strokeColor(ui.theme.editTextColor.mask((int) LXUtils.lerp(255, 100, this.editCursorBasis)));
      vg.line(cursorX, 1, cursorX, this.height-1);
      vg.stroke();
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
            editAppend(keyChar);
            onEditChange(this.editBuffer);
            redraw();
          }
        } else if (keyEvent.isEnter()) {
          keyEvent.consume();
          this.editing = false;
          saveEditBuffer(this.editBuffer);
          redraw();
        } else if (keyEvent.isCommand(KeyEvent.VK_A)) {
          keyEvent.consume();
          this.editRangeStart = 0;
          this.editCursor = editRangeEnd = this.editBuffer.length();
          redraw();
        } else if (keyCode == KeyEvent.VK_BACKSPACE) {
          keyEvent.consume();
          if (this.editBuffer.length() > 0) {
            if (keyEvent.isShiftDown() || keyEvent.isControlDown() || keyEvent.isMetaDown()) {
              this.editBuffer = "";
              editCursor(0);
              onEditChange(this.editBuffer);
            } else {
              if (this.editRangeEnd != this.editRangeStart) {
                // Range deletion
                this.editBuffer =
                  this.editBuffer.substring(0, this.editRangeStart) +
                  this.editBuffer.substring(this.editRangeEnd);
                  editCursor(this.editRangeStart);
              } else {
                // Single char deletion from cursor position
                this.editBuffer =
                  this.editBuffer.substring(0, this.editCursor - 1) +
                  this.editBuffer.substring(this.editCursor);
                editCursor(this.editCursor - 1);
              }
              onEditChange(this.editBuffer);
            }
            redraw();
          }
        } else if (keyCode == KeyEvent.VK_LEFT) {
          keyEvent.consume();
          int cursor = this.editCursor - 1;
          if (keyEvent.isCommand()) {
            cursor = 0;
          } else if (keyEvent.isAltDown()) {
            while (cursor > 0 && Character.isLetterOrDigit(this.editBuffer.charAt(cursor - 1))) {
              --cursor;
            }
          }
          if (keyEvent.isShiftDown()) {
            editRange(cursor);
          } else {
            editCursor(cursor);
          }
          redraw();
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          keyEvent.consume();
          int cursor = this.editCursor + 1;
          if (keyEvent.isCommand()) {
            cursor = this.editBuffer.length();
          } else if (keyEvent.isAltDown()) {
            while (cursor < this.editBuffer.length() && Character.isLetterOrDigit(this.editBuffer.charAt(cursor))) {
              ++cursor;
            }
          }
          if (keyEvent.isShiftDown()) {
            editRange(cursor);
          } else {
            editCursor(cursor);
          }
          redraw();
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
          String editBuffer = getInitialEditBufferValue();
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
