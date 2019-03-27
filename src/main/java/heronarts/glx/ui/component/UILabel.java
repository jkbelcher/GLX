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

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.vg.VGraphics;

/**
 * A simple text label object. Draws a string aligned top-left to its x-y
 * position.
 */
public class UILabel extends UI2dComponent {

  private int topPadding = 0;
  private int rightPadding = 0;
  private int leftPadding = 0;
  private int bottomPadding = 0;
  private boolean breakLines = false;

  /**
   * Label text
   */
  private String label = "";

  public UILabel() {
    this(0, 0, 0, 0);
  }

  public UILabel(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UILabel setBreakLines(boolean breakLines) {
    if (this.breakLines != breakLines) {
      this.breakLines = breakLines;
      redraw();
    }
    return this;
  }

  /**
   * Sets padding on all 4 sides
   *
   * @param padding Padding
   * @return this
   */
  public UILabel setPadding(int padding) {
    return setPadding(padding, padding, padding, padding);
  }

  /**
   * Sets padding on top and sides, CSS style
   *
   * @param topBottom Top bottom padding
   * @param leftRight Left right padding
   * @return this
   */
  public UILabel setPadding(int topBottom, int leftRight) {
    return setPadding(topBottom, leftRight, topBottom, leftRight);
  }

  /**
   * Sets padding on all 4 sides
   *
   * @param topPadding Top padding
   * @param rightPadding Right padding
   * @param bottomPadding Bottom padding
   * @param leftPadding Left padding
   * @return this
   */
  public UILabel setPadding(int topPadding, int rightPadding, int bottomPadding, int leftPadding) {
    boolean redraw = false;
    if (this.topPadding != topPadding) {
      this.topPadding = topPadding;
      redraw = true;
    }
    if (this.rightPadding != rightPadding) {
      this.rightPadding = rightPadding;
      redraw = true;
    }
    if (this.bottomPadding != bottomPadding) {
      this.bottomPadding = bottomPadding;
      redraw = true;
    }
    if (this.leftPadding != leftPadding) {
      this.leftPadding = leftPadding;
      redraw = true;
    }
    if (redraw) {
      redraw();
    }
    return this;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    vg.fontFace(hasFont() ? getFont() : ui.theme.getLabelFont());
    vg.fillColor(hasFontColor() ? getFontColor() : ui.theme.getLabelColor());
    float tx = this.leftPadding, ty = this.topPadding;
    switch (this.textAlignHorizontal) {
    case CENTER:
      tx = this.width / 2;
      break;
    case RIGHT:
      tx = this.width - this.rightPadding;
      break;
    default:
      break;
    }
    switch (this.textAlignVertical) {
    case BASELINE:
      ty = this.height - this.bottomPadding;
      break;
    case BOTTOM:
      ty = this.height - this.bottomPadding;
      break;
    case MIDDLE:
      ty = this.height / 2;
      break;
    default:
      break;
    }
    if (this.breakLines) {
      vg.beginPath();
      vg.textAlign(this.textAlignHorizontal, this.textAlignVertical);
      vg.textBox(tx + this.textOffsetX, ty + this.textOffsetY, this.width - this.leftPadding - this.rightPadding, this.label);
      vg.fill();
    } else {
      String str = clipTextToWidth(vg, this.label, this.width - this.leftPadding - this.rightPadding);
      vg.beginPath();
      vg.textAlign(this.textAlignHorizontal, this.textAlignVertical);
      vg.text(tx + this.textOffsetX, ty + this.textOffsetY, str);
      vg.fill();
    }
  }

  public UILabel setLabel(String label) {
    if (this.label != label) {
      this.label = label;
      redraw();
    }
    return this;
  }

  @Override
  public String getDescription() {
    String description = super.getDescription();
    return (description != null) ? description : this.label;
  }
}
