package heronarts.glx.ui.component;

import java.util.ArrayList;
import java.util.List;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.lx.command.LXCommand;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;

public abstract class UIParameterComponent extends UI2dComponent implements UIContextActions {
  protected UIParameterComponent(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public LXParameter getParameter() {
    return null;
  }

  public String getOscAddress() {
    LXParameter parameter = getParameter();
    if (parameter != null) {
      return LXOscEngine.getOscAddress(parameter);
    }
    return null;
  }

  @Override
  public List<Action> getContextActions() {
    List<Action> actions = new ArrayList<Action>();
    LXParameter parameter = getParameter();
    if (parameter != null && !(parameter instanceof BooleanParameter)) {
      actions.add(new UIContextActions.Action.ResetParameter(parameter));
    }
    String oscAddress = getOscAddress();
    if (oscAddress != null) {
      actions.add(new UIContextActions.Action.CopyOscAddress(oscAddress));
    }
    return actions;
  }

  private LXCommand.Parameter.SetNormalized mouseEditCommand = null;

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    LXParameter parameter = getParameter();
    if (parameter != null && parameter instanceof LXNormalizedParameter) {
      this.mouseEditCommand = new LXCommand.Parameter.SetNormalized((LXNormalizedParameter) parameter);
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    this.mouseEditCommand = null;
  }

  protected void setNormalizedCommand(double newValue) {
    if (this.mouseEditCommand != null) {
      getLX().command.perform(this.mouseEditCommand.update(newValue));
    } else {
      LXParameter parameter = getParameter();
      if (parameter != null && parameter instanceof LXNormalizedParameter) {
        getLX().command.perform(new LXCommand.Parameter.SetNormalized((LXNormalizedParameter) parameter, newValue));
      }
    }
  }

}