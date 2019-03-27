/**
 * Copyright 2019- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui;

import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;

public class UIDialogBox extends UI2dContainer implements UIMouseFocus {

  private static final int PADDING = 8;
  private static final int WIDTH = 280;
  private static final int HEIGHT = 80;

  private static final int OPTION_WIDTH = 60;
  private static final int OPTION_PADDING = 4;
  private static final int OPTION_SPACING = OPTION_WIDTH + OPTION_PADDING;

  private static final int BUTTON_ROW = 24;

  public UIDialogBox(UI ui, String message) {
    this(ui, message, new String[] { "Okay" }, null);
  }

  public UIDialogBox(UI ui, String message, String[] options, Runnable[] callbacks) {
    super((ui.getWidth() - WIDTH) / 2, (ui.getHeight() - 2*HEIGHT) / 2, WIDTH, HEIGHT);
    setBackgroundColor(ui.theme.getDeviceFocusedBackgroundColor());
    setBorderColor(UI.BLACK);
    setBorderRounding(4);

    new UILabel(PADDING, PADDING, this.width - 2*PADDING, this.height - 2*PADDING - BUTTON_ROW)
    .setLabel(message)
    .setBreakLines(true)
    .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.TOP)
    .setTextOffset(0, 8)
    .addToContainer(this);

    float yp = this.height - BUTTON_ROW;
    float xp = this.width / 2 - options.length * OPTION_SPACING / 2 + OPTION_PADDING / 2;
    for (int i = 0; i < options.length; ++i) {
      final int ii = i;
      new UIButton(xp, yp, OPTION_WIDTH, 16) {
        @Override
        protected void onClick() {
          getUI().hideContextOverlay();
          if ((callbacks != null) && (callbacks[ii] != null)) {
            callbacks[ii].run();
          }
        }
      }
      .setMomentary(true)
      .setLabel(options[i])
      .setBorderRounding(8)
      .addToContainer(this);
      xp += OPTION_SPACING;
    }
  }
}
