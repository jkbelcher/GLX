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

package heronarts.glx.ui;

import heronarts.glx.View;

/**
 * A component in a UI3dContext. Draws itself and may draw children.
 */
public abstract class UI3dComponent extends UIObject {

  private UI3dContext context;

  @Override
  public boolean contains(float x, float y) {
    return false;
  }

  @Override
  public float getWidth() {
    return -1;
  }

  @Override
  public float getHeight() {
    return -1;
  }

  void setContext(UI3dContext context) {
    this.context = context;
  }

  public UI3dContext getContext() {
    return this.context;
  }

  /**
   * Adds a child to this component
   *
   * @param child Child component
   * @return this
   */
  public final UI3dComponent addChild(UI3dComponent child) {
    this.mutableChildren.add(child);
    return this;
  }

  /**
   * Removes a child from this component
   *
   * @param child Child component
   * @return this
   */
  public final UI3dComponent removeChild(UI3dComponent child) {
    this.mutableChildren.remove(child);
    return this;
  }

  /**
   * Draw the given component into the View context
   * @param ui
   * @param view
   */
  public final void draw(UI ui, View view) {
    onDraw(ui, view);
    for (UIObject child : this.children) {
      ((UI3dComponent) child).draw(ui, view);
    }
  }

  protected void onDraw(UI ui, View view) {
    // subclasses may override
  }
}
