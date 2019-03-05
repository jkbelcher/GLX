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

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXUtils;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXCompoundModulation;

public class UIKnob extends UICompoundParameterControl implements UIFocus {

  public final static int KNOB_MARGIN = 6;
  public final static int KNOB_SIZE = 28;
  public final static int WIDTH = KNOB_SIZE + 2*KNOB_MARGIN;
  public final static int HEIGHT = KNOB_SIZE + LABEL_MARGIN + LABEL_HEIGHT;

  private final static float KNOB_INDENT = .4f;
  private final static float ARC_CENTER_X = WIDTH / 2;
  private final static float ARC_CENTER_Y = KNOB_SIZE / 2 + .5f;
  private final static float ARC_START = (float) LX.HALF_PI + KNOB_INDENT;
  private final static float ARC_RANGE = (float) LX.TWO_PI - 2 * KNOB_INDENT;
  private final static float ARC_END = ARC_START + ARC_RANGE;

  public UIKnob(LXListenableNormalizedParameter parameter) {
    this();
    setParameter(parameter);
  }

  public UIKnob() {
    this(0, 0);
  }

  public UIKnob(float x, float y) {
    this(x, y, WIDTH, KNOB_SIZE);
  }

  public UIKnob(float x, float y, float w, float h) {
    super(x, y, w, h);
    this.keyEditable = true;
    enableImmediateEdit(true);
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    // value refers to the current, possibly-modulated value of the control's parameter.
    // base is the unmodulated, base value of that parameter.
    // If unmodulated, these will be equal
    float value = (float) getCompoundNormalized();
    float base = (float) getNormalized();
    float valueEnd = ARC_START + value * ARC_RANGE;
    float baseEnd = ARC_START + base * ARC_RANGE;
    float valueStart;
    switch (this.polarity) {
    case BIPOLAR: valueStart = ARC_START + ARC_RANGE/2; break;
    default: case UNIPOLAR: valueStart = ARC_START; break;
    }

    float arcSize = KNOB_SIZE / 2;

    // Modulations!
    if (this.parameter instanceof CompoundParameter) {
      CompoundParameter compound = (CompoundParameter) this.parameter;
      for (int i = compound.modulations.size()-1; i >= 0; --i) {
        LXCompoundModulation modulation = compound.modulations.get(i);
        registerModulation(modulation);

        float modStart, modEnd;
        switch (modulation.getPolarity()) {
        case BIPOLAR:
          modStart = LXUtils.constrainf(baseEnd - modulation.range.getValuef() * ARC_RANGE, ARC_START, ARC_END);
          modEnd = LXUtils.constrainf(baseEnd + modulation.range.getValuef() * ARC_RANGE, ARC_START, ARC_END);
          break;
        default:
        case UNIPOLAR:
          modStart = baseEnd;
          modEnd = LXUtils.constrainf(modStart + modulation.range.getValuef() * ARC_RANGE, ARC_START, ARC_END);
          break;
        }

        // Light ring of value
        ColorParameter modulationColor = modulation.color;
        int modColor = ui.theme.getControlDisabledColor();
        int modColorInv = modColor;
        if (isEnabled() && modulation.enabled.isOn()) {
          modColor = modulationColor.getColor();
          modColorInv = LXColor.hsb(LXColor.h(modColor), 50, 75);
        }
        vg.fillColor(modColor);
        switch (modulation.getPolarity()) {
        case BIPOLAR:
          if (modEnd >= modStart) {
            vg.beginPath();
            vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
            vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, baseEnd, Math.min(ARC_END, modEnd+.1f));
            vg.fill();

            vg.fillColor(modColorInv);
            vg.beginPath();
            vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
            vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, modStart-.1f), baseEnd);
            vg.fill();
          } else {
            vg.beginPath();
            vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
            vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, modEnd-.1f), baseEnd);
            vg.fill();

            vg.fillColor(modColorInv);
            vg.beginPath();
            vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
            vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, baseEnd, Math.min(ARC_END, modStart+.1f));
            vg.fill();
          }
          break;
        case UNIPOLAR:
          vg.beginPath();
          vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
          vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, Math.min(modStart, modEnd)-.1f), Math.min(ARC_END, Math.max(modStart, modEnd)+.1f));
          vg.fill();
          break;
        }
        arcSize -= 3;
        vg.fillColor(ui.theme.getDeviceBackgroundColor());
        vg.beginPath();
        vg.circle(ARC_CENTER_X, ARC_CENTER_Y, arcSize);
        vg.fill();
        arcSize -= 1;
        if (arcSize < 6) {
          break;
        }

      }
    }

    // Outer fill
    vg.fillColor(ui.theme.getControlBackgroundColor());
    vg.beginPath();
    vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
    vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, ARC_START, ARC_END);
    vg.fill();

    // Compute colors for base/value fills
    int baseColor;
    int valueColor;
    if (isEnabled() && isEditable()) {
      baseColor = ui.theme.getPrimaryColor();
      valueColor = getModulatedValueColor(baseColor);
    } else {
      int disabled = ui.theme.getControlDisabledColor();
      baseColor = disabled;
      valueColor = disabled;
    }

    // Value indication
    if (valueStart != baseEnd) {
      vg.fillColor(baseColor);
      vg.beginPath();
      vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
      vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.min(valueStart, baseEnd), Math.max(valueStart, baseEnd));
      vg.closePath();
      vg.fill();
    }

    if (baseEnd != valueEnd) {
      vg.fillColor(valueColor);
      vg.beginPath();
      vg.moveTo(ARC_CENTER_X, ARC_CENTER_Y);
      vg.arc(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.min(baseEnd, valueEnd), Math.max(baseEnd, valueEnd));
      vg.closePath();
      vg.fill();
    }

    // Center tick mark for bipolar knobs
    if (this.polarity == LXParameter.Polarity.BIPOLAR) {
      vg.strokeColor(0xff333333);
      vg.beginPath();
      vg.line(ARC_CENTER_X, ARC_CENTER_Y, ARC_CENTER_X, ARC_CENTER_Y - arcSize/2);
      vg.stroke();
    }

    // Center dot
    vg.fillColor(0xff333333);
    vg.beginPath();
    vg.circle(ARC_CENTER_X, ARC_CENTER_Y, 4);
    vg.fill();

    super.onDraw(ui, vg);
  }

  private double dragValue;

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);

    this.dragValue = getNormalized();
    if (isEditable() && (this.parameter != null) && (mouseEvent.getCount() > 1)) {
      LXCompoundModulation modulation = getModulation(mouseEvent.isShiftDown());
      if (modulation != null && (mouseEvent.isControlDown() || mouseEvent.isMetaDown())) {
        getLX().command.perform(new LXCommand.Parameter.Reset(modulation.range));
      } else {
        getLX().command.perform(new LXCommand.Parameter.Reset(this.parameter));
      }
    }
  }

  private LXCompoundModulation getModulation(boolean secondary) {
    if (this.parameter != null && this.parameter instanceof CompoundParameter) {
      CompoundParameter compound = (CompoundParameter) this.parameter;
      int size = compound.modulations.size();
      if (size > 0) {
        if (secondary && (size > 1)) {
          return compound.modulations.get(size-2);
        } else {
          return compound.modulations.get(size-1);
        }
      }
    }
    return null;
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (!isEnabled() || !isEditable()) {
      return;
    }

    float delta = dy / 100.f;
    LXCompoundModulation modulation = getModulation(mouseEvent.isShiftDown());
    if (modulation != null && (mouseEvent.isMetaDown() || mouseEvent.isControlDown())) {
      modulation.range.setValue(modulation.range.getValue() - delta);
    } else {
      if (mouseEvent.isShiftDown()) {
        delta /= 10;
      }
      this.dragValue = LXUtils.constrain(this.dragValue - delta, 0, 1);
      setNormalized(this.dragValue);
    }
  }

}
