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

import java.util.ArrayList;
import java.util.List;

import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UITimerTask;
import heronarts.lx.parameter.CompoundParameter;

public class UICompoundParameterControl extends UIParameterControl {
  private double lastParameterValue = 0;

  private final List<LXListenableParameter> modulationParameters = new ArrayList<LXListenableParameter>();

  private final UITimerTask checkRedrawTask = new UITimerTask(30, UITimerTask.Mode.FPS) {
    @Override
    public void run() {
      double parameterValue = getCompoundNormalized();
      if (parameterValue != lastParameterValue) {
        redraw();
      }
      lastParameterValue = parameterValue;
    }
  };

  private final CompoundParameter.ModulationListener modulationListener = new CompoundParameter.ModulationListener() {

    @Override
    public void modulationAdded(CompoundParameter parameter, LXCompoundModulation modulation) {

    }

    @Override
    public void modulationRemoved(CompoundParameter parameter, LXCompoundModulation modulation) {
      removeModulationParameter(modulation.range);
      removeModulationParameter(modulation.polarity);
      removeModulationParameter(modulation.enabled);
    }

    private void removeModulationParameter(LXListenableParameter parameter) {
      if (modulationParameters.contains(parameter)) {
        parameter.removeListener(redraw);
        modulationParameters.remove(parameter);
      }
    }

  };

  protected UICompoundParameterControl(float x, float y, float w, float h) {
    super(x, y, w, h);
    addLoopTask(this.checkRedrawTask);
  }

  @Override
  public UIParameterControl setParameter(LXNormalizedParameter parameter) {
    for (LXListenableParameter p : this.modulationParameters) {
      p.removeListener(this.redraw);
    }
    this.modulationParameters.clear();
    super.setParameter(parameter);
    if (parameter instanceof CompoundParameter) {
      ((CompoundParameter) parameter).addModulationListener(this.modulationListener);
    }
    return this;
  }

  protected double getCompoundNormalized() {
    if (this.parameter != null) {
      if (this.parameter instanceof CompoundParameter) {
        return ((CompoundParameter) this.parameter).getNormalized();
      } else {
        return getNormalized();
      }
    }
    return 0;
  }

  protected void registerModulation(LXCompoundModulation modulation) {
    if (!this.modulationParameters.contains(modulation.range)) {
      this.modulationParameters.add(modulation.range);
      this.modulationParameters.add(modulation.polarity);
      this.modulationParameters.add(modulation.enabled);
      modulation.range.addListener(this.redraw);
      modulation.polarity.addListener(this.redraw);
      modulation.enabled.addListener(this.redraw);

      // Colors may be shared across multiple modulations from same source component
      if (!this.modulationParameters.contains(modulation.color)) {
        this.modulationParameters.add(modulation.color);
        modulation.color.addListener(this.redraw);
      }
    }
  }

  @Override
  public List<UIContextActions.Action> getContextActions() {
    List<UIContextActions.Action> actions = super.getContextActions();
    if (this.parameter instanceof CompoundParameter) {
      final CompoundParameter cp = (CompoundParameter) this.parameter;
      if (!cp.modulations.isEmpty()) {
        actions.add(new UIContextActions.Action("Remove Modulation") {
          @Override
          public void onContextAction(UI ui) {
            ui.lx.command.perform(new LXCommand.Modulation.RemoveModulations(cp));
          }
        });
      }
    }
    return actions;
  }

  @Override
  public void dispose() {
    if (this.parameter instanceof CompoundParameter) {
      ((CompoundParameter) this.parameter).removeModulationListener(this.modulationListener);
    }
    for (LXListenableParameter parameter : this.modulationParameters) {
      parameter.removeListener(this.redraw);
    }
    this.modulationParameters.clear();
    super.dispose();
  }
}
