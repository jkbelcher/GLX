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
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.utils.LXUtils;

public class UIColorPicker extends UI2dComponent {

  public enum Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
  };

  private Corner corner = Corner.BOTTOM_RIGHT;

  private final ColorParameter color;
  private final UIColorOverlay uiColorOverlay;

  public UIColorPicker(ColorParameter color) {
    this(UIKnob.WIDTH, UIKnob.WIDTH, color);
  }

  public UIColorPicker(float w, float h, ColorParameter color) {
    this(0, 0, w, h, color);
  }

  public UIColorPicker(float x, float y, float w, float h, ColorParameter color) {
    super(x, y, w, h);
    setBorderColor(UI.get().theme.getControlBorderColor());
    setBackgroundColor(color.getColor());

    this.color = color;
    this.uiColorOverlay = new UIColorOverlay();

    // Redraw with color in real-time, if modulated
    addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
      @Override
      protected void run() {
        setBackgroundColor(LXColor.hsb(
          color.hue.getValuef(),
          color.saturation.getValuef(),
          color.brightness.getValuef()
        ));
      }
    });
  }

  public UIColorPicker setCorner(Corner corner) {
    this.corner = corner;
    return this;
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    final float overlap = 6;

    switch (this.corner) {
    case BOTTOM_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), this.height - overlap);
      break;
    case BOTTOM_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, this.height - overlap);
      break;
    case TOP_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), overlap - this.uiColorOverlay.getHeight());
      break;
    case TOP_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, overlap - this.uiColorOverlay.getHeight());
      break;
    }
    getUI().showContextOverlay(this.uiColorOverlay);
  }

  private class UIColorOverlay extends UI2dContainer {
    UIColorOverlay() {
      super(0, 0, 240, UISwatch.HEIGHT + 8);
      setBackgroundColor(UI.get().theme.getDeviceBackgroundColor());
      setBorderColor(UI.get().theme.getControlBorderColor());
      setBorderRounding(6);

      new UISwatch().addToContainer(this);

      float xp = UISwatch.WIDTH;
      float yp = 16;
      new UIDoubleBox(xp, yp, 60, color.hue).addToContainer(this);
      new UILabel(xp, yp + 16, 60, "Hue").setTextAlignment(VGraphics.Align.CENTER).addToContainer(this);

      yp += 40;

      new UIDoubleBox(xp, yp, 60, color.saturation).addToContainer(this);
      new UILabel(xp, yp + 16, 60, "Sat").setTextAlignment(VGraphics.Align.CENTER).addToContainer(this);

      yp += 40;

      new UIDoubleBox(xp, yp, 60, color.brightness).addToContainer(this);
      new UILabel(xp, yp + 16, 60, "Bright").setTextAlignment(VGraphics.Align.CENTER).addToContainer(this);

    }

    private class UISwatch extends UI2dComponent implements UIFocus{

      private static final float PADDING = 8;

      private static final float GRID_X = PADDING;
      private static final float GRID_Y = PADDING;

      private static final float GRID_WIDTH = 120;
      private static final float GRID_HEIGHT = 120;

      private static final float BRIGHT_SLIDER_X = 140;
      private static final float BRIGHT_SLIDER_Y = PADDING;
      private static final float BRIGHT_SLIDER_WIDTH = 16;
      private static final float BRIGHT_SLIDER_HEIGHT = GRID_HEIGHT;

      private static final float WIDTH = BRIGHT_SLIDER_X + BRIGHT_SLIDER_WIDTH + 2*PADDING;
      private static final float HEIGHT = GRID_HEIGHT + 2*PADDING;

      public UISwatch() {
        super(4, 4, WIDTH, HEIGHT);
        color.addListener((p) -> { redraw(); });
        setFocusCorners(false);
      }

      @Override
      public void onDraw(UI ui, VGraphics vg) {
        final int xStops = 6;
        final int yStops = 40;
        final float xStep = GRID_WIDTH / xStops;
        final float yStep = GRID_HEIGHT / yStops;

        float hue = color.hue.getBaseValuef();
        float saturation = color.saturation.getBaseValuef();
        float brightness = color.brightness.getBaseValuef();

        // Main color grid
        for (int y = 0; y < yStops; ++y) {
          for (int x = 0; x < xStops; ++x) {
            vg.fillLinearGradient(GRID_X + x * xStep, 0, GRID_X + (x+1) * xStep, 0,
              LXColor.hsb(x * 360 / xStops, 100f - y * 100f / yStops, brightness),
              LXColor.hsb((x+1) * 360 / xStops, 100f - y * 100f / yStops, brightness));
            vg.beginPath();
            vg.rect(GRID_X + x * xStep - .5f, GRID_Y + y * yStep - .5f, xStep + 1, yStep + 1);
            vg.fill();
          }
        }

        // Brightness slider
        vg.fillLinearGradient(BRIGHT_SLIDER_X, BRIGHT_SLIDER_Y, BRIGHT_SLIDER_X, BRIGHT_SLIDER_HEIGHT,
          LXColor.hsb(hue, saturation, 100),
          LXColor.hsb(hue, saturation, 0)
        );
        vg.beginPath();
        vg.rect(BRIGHT_SLIDER_X, BRIGHT_SLIDER_Y, BRIGHT_SLIDER_WIDTH, BRIGHT_SLIDER_HEIGHT);
        vg.fill();

        // Color square
        vg.beginPath();
        vg.strokeColor(brightness < 50 ? 0xffffffff : 0xff000000);
        vg.ellipse(
          GRID_X + hue / 360 * GRID_WIDTH,
          GRID_Y + (1 - saturation / 100) * GRID_HEIGHT,
          4,
          4
        );
        vg.stroke();

        // Brightness triangle
        vg.beginPath();
        vg.fillColor(0xffcccccc);
        float xp = BRIGHT_SLIDER_X;
        float yp = BRIGHT_SLIDER_Y + (1 - brightness / 100) * BRIGHT_SLIDER_HEIGHT;
        vg.moveTo(xp, yp);
        vg.lineTo(xp - 6, yp - 4);
        vg.lineTo(xp - 6, yp + 4);
        vg.closePath();
        vg.moveTo(xp + BRIGHT_SLIDER_WIDTH, yp);
        vg.lineTo(xp + BRIGHT_SLIDER_WIDTH + 6, yp + 4);
        vg.lineTo(xp + BRIGHT_SLIDER_WIDTH + 6, yp - 4);
        vg.closePath();
        vg.fill();
      }

      private boolean draggingBrightness = false;

      @Override
      public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
        mouseEvent.consume();
        this.draggingBrightness = (mx > GRID_X + GRID_WIDTH);
        if (!this.draggingBrightness) {
          setHueSaturation(mx, my);
        }
      }

      private void setHueSaturation(float mx, float my) {
        mx = LXUtils.clampf(mx - GRID_X, 0, GRID_WIDTH);
        my = LXUtils.clampf(my - GRID_Y, 0, GRID_WIDTH);
        color.hue.setValue(mx / GRID_WIDTH * 360);
        color.saturation.setValue(100 - my / GRID_HEIGHT * 100);
      }

      @Override
      public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
        mouseEvent.consume();
        if (this.draggingBrightness) {
          if (dy != 0) {
            float brightness = color.brightness.getBaseValuef();
            brightness = LXUtils.clampf(brightness - 100 * dy / BRIGHT_SLIDER_HEIGHT, 0, 100);
            color.brightness.setValue(brightness);
          }
        } else {
          setHueSaturation(mx, my);
        }
      }

      @Override
      public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
        float inc = keyEvent.isShiftDown() ? 10 : 2;
        if (keyCode == KeyEvent.VK_UP) {
          keyEvent.consume();
          color.saturation.setValue(LXUtils.clampf(color.saturation.getBaseValuef() + inc, 0, 100));
        } else if (keyCode == KeyEvent.VK_DOWN) {
          keyEvent.consume();
          color.saturation.setValue(LXUtils.clampf(color.saturation.getBaseValuef() - inc, 0, 100));
        } else if (keyCode == KeyEvent.VK_LEFT) {
          keyEvent.consume();
          color.hue.setValue(LXUtils.clampf(color.hue.getBaseValuef() - 3*inc, 0, 360));
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          keyEvent.consume();
          color.hue.setValue(LXUtils.clampf(color.hue.getBaseValuef() + 3*inc, 0, 360));
        }
      }

    }
  }


}
