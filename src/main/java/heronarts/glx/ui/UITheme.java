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

package heronarts.glx.ui;

import java.io.IOException;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;

public class UITheme {

  private VGraphics.Font labelFont;
  private int labelColor = 0xffcccccc;
  private int sublabelColor = 0xff777777;

  private VGraphics.Font deviceFont;

  private VGraphics.Font windowTitleFont;
  private int windowTitleColor = 0xffcccccc;

  private int deviceBackgroundColor = 0xff404040;
  private int deviceFocusedBackgroundColor = 0xff4c4c4c;
  private int deviceSelectionColor = 0xff586658;
  private int deviceSelectionTextColor = 0xffe0e0e0;
  private int deviceBorderColor = 0xff292929;

  private int paneBackgroundColor = 0xff040404;
  private int paneInsetColor = 0xff242424;

  private int focusColor = 0xff669966;
  private int primaryColor = 0xff669966;
  private int secondaryColor = 0xff666699;
  private int attentionColor = 0xff996666;
  private int surfaceColor = 0xff669999;
  private int recordingColor = 0xffa00044;
  private int cursorColor = 0xff555555;
  private int selectionColor = 0xff333333;

  private int darkBackgroundColor = 0xff191919;
  private int darkFocusBackgroundColor = 0xff292929;

  private VGraphics.Font controlFont;
  private int controlBackgroundColor = 0xff222222;
  private int controlBorderColor = 0xff292929;
  private int controlTextColor = 0xffcccccc;
  private int controlDisabledTextColor = 0xff707070;
  private int controlDisabledColor = 0xff303030;

  private int contextBackgroundColor = controlBackgroundColor;
  private int contextBorderColor = 0xff000000;
  private int contextHighlightColor = 0xff333333;

  private int midiMappingColor = 0x33ff0000;
  private int modulationSourceMappingColor = 0x3300ff00;
  private int modulationTargetMappingColor = 0x3300cccc;

  public final VGraphics.Image iconNote;
  public final VGraphics.Image iconTempo;
  public final VGraphics.Image iconControl;
  public final VGraphics.Image iconTrigger;
  public final VGraphics.Image iconTriggerSource;
  public final VGraphics.Image iconLoop;
  public final VGraphics.Image iconMap;
  public final VGraphics.Image iconArm;
  public final VGraphics.Image iconLfo;
  public final VGraphics.Image iconLoad;
  public final VGraphics.Image iconSave;
  public final VGraphics.Image iconSaveAs;
  public final VGraphics.Image iconNew;
  public final VGraphics.Image iconOpen;
  public final VGraphics.Image iconKeyboard;
  public final VGraphics.Image iconPreferences;

  UITheme(VGraphics vg) throws IOException {
    this.controlFont = vg.loadFont("Arial Unicode MS", "Arial Unicode.ttf");
    this.controlFont.fontSize(14);
    LX.initTimer.log("P3LX: UI: Theme: controlFont");

    this.labelFont = this.deviceFont = this.windowTitleFont = vg.loadFont("Arial Black", "Arial Black.ttf");
    this.labelFont.fontSize(13);
    LX.initTimer.log("P3LX: UI: Theme: windowTitleFont");

    this.iconNote = vg.loadIcon("icon-note.png");
    this.iconTempo = vg.loadIcon("icon-tempo.png");
    this.iconControl = vg.loadIcon("icon-control.png");
    this.iconTrigger = vg.loadIcon("icon-trigger.png");
    this.iconTriggerSource = vg.loadIcon("icon-trigger-source.png");
    this.iconLoop = vg.loadIcon("icon-loop.png");
    this.iconMap = vg.loadIcon("icon-map.png");
    this.iconArm = vg.loadIcon("icon-arm.png");
    this.iconLfo = vg.loadIcon("icon-lfo.png");
    this.iconLoad = vg.loadIcon("icon-load.png");
    this.iconSave = vg.loadIcon("icon-save.png");
    this.iconSaveAs = vg.loadIcon("icon-save-as.png");
    this.iconNew = vg.loadIcon("icon-new.png");
    this.iconOpen = vg.loadIcon("icon-open.png");
    this.iconKeyboard = vg.loadIcon("icon-keyboard.png");
    this.iconPreferences = vg.loadIcon("icon-preferences.png");
    LX.initTimer.log("P3LX: UI: Theme: Icons");
  }

  /**
   * Gets the default item font
   *
   * @return The default item font
   */
  public VGraphics.Font getControlFont() {
    return this.controlFont;
  }

  /**
   * Sets the default item font
   *
   * @param font Font to use
   * @return this
   */
  public UITheme setControlFont(VGraphics.Font font) {
    this.controlFont = font;
    return this;
  }

  /**
   * Gets the default title font
   *
   * @return default title font
   */
  public VGraphics.Font getWindowTitleFont() {
    return this.windowTitleFont;
  }

  /**
   * Sets the default title font
   *
   * @param font Default title font
   * @return this
   */
  public UITheme setWindowTitleFont(VGraphics.Font font) {
    this.windowTitleFont = font;
    return this;
  }

  /**
   * Label font
   *
   * @return font
   */
  public VGraphics.Font getLabelFont() {
    return this.labelFont;
  }

  /**
   * Set label font
   *
   * @param labelFont font
   * @return this
   */
  public UITheme setLabelFont(VGraphics.Font labelFont) {
    this.labelFont = labelFont;
    return this;
  }

  public VGraphics.Font getDeviceFont() {
    return this.deviceFont;
  }

  /**
   * Gets the default text color
   *
   * @return default text color
   */
  public int getWindowTitleColor() {
    return this.windowTitleColor;
  }

  /**
   * Sets the default text color
   *
   * @param color Color
   * @return this UI
   */
  public UITheme setWindowTitleColor(int color) {
    this.windowTitleColor = color;
    return this;
  }

  /**
   * Gets background color
   *
   * @return background color
   */
  public int getDeviceBackgroundColor() {
    return this.deviceBackgroundColor;
  }

  /**
   * Sets default background color
   *
   * @param color color
   * @return this UI
   */
  public UITheme setDeviceBackgroundColor(int color) {
    this.deviceBackgroundColor = color;
    return this;
  }

  /**
   * Gets background color
   *
   * @return background color
   */
  public int getDeviceFocusedBackgroundColor() {
    return this.deviceFocusedBackgroundColor;
  }

  /**
   * Sets default background color
   *
   * @param color color
   * @return this UI
   */
  public UITheme setDeviceFocusedBackgroundColor(int color) {
    this.deviceFocusedBackgroundColor = color;
    return this;
  }

  /**
   * Sets device selection color
   *
   * @param deviceSelectionColor color
   * @return this UI
   */
  public UITheme setDeviceSelectionColor(int deviceSelectionColor) {
    this.deviceSelectionColor = deviceSelectionColor;
    return this;
  }

  /**
   * Gets device selection color
   *
   * @return device selection color
   */
  public int getDeviceSelectionTextColor() {
    return this.deviceSelectionTextColor;
  }

  /**
   * Sets device selection color
   *
   * @param deviceSelectionTextColor color
   * @return this UI
   */
  public UITheme setDeviceSelectionTextColor(int deviceSelectionTextColor) {
    this.deviceSelectionTextColor = deviceSelectionTextColor;
    return this;
  }

  /**
   * Gets device selection color
   *
   * @return device selection color
   */
  public int getDeviceSelectionColor() {
    return this.deviceSelectionColor;
  }

  /**
   * Gets device border color
   *
   * @return device border color
   */
  public int getDeviceBorderColor() {
    return this.deviceBorderColor;
  }

  /**
   * Sets default border color
   *
   * @param color color
   * @return this UI
   */
  public UITheme setDeviceBorderColor(int color) {
    this.deviceBorderColor = color;
    return this;
  }

  /**
   * Get context background color
   *
   * @return Context background color
   */
  public int getContextBackgroundColor() {
    return this.contextBackgroundColor;
  }

  /**
   * Set context background color
   *
   * @param contextBackgroundColor the color
   * @return this
   */
  public UITheme setContextBackgroundColor(int contextBackgroundColor) {
    this.contextBackgroundColor = contextBackgroundColor;
    return this;
  }

  /**
   * Get context border color
   *
   * @return Context border color
   */
  public int getContextBorderColor() {
    return this.contextBorderColor;
  }

  /**
   * Set context border color
   *
   * @param contextBorderColor the color
   * @return this
   */
  public UITheme setContextBorderColor(int contextBorderColor) {
    this.contextBorderColor = contextBorderColor;
    return this;
  }

  /**
   * Gets border color
   *
   * @return border color
   */
  public int getPaneBackgroundColor() {
    return this.paneBackgroundColor;
  }

  /**
   * Sets default border color
   *
   * @param color color
   * @return this UI
   */
  public UITheme setPaneBackgroundColor(int color) {
    this.paneBackgroundColor = color;
    return this;
  }

  /**
   * Gets pane inset color
   *
   * @return Pane inset color
   */
  public int getPaneInsetColor() {
    return this.paneInsetColor;
  }

  /**
   * Sets default border color
   *
   * @param color color
   * @return this UI
   */
  public UITheme setPaneInsetColor(int color) {
    this.paneInsetColor = color;
    return this;
  }

  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getPrimaryColor() {
    return this.primaryColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setPrimaryColor(int color) {
    this.primaryColor = color;
    return this;
  }

  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getAttentionColor() {
    return this.attentionColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setAttentionColor(int color) {
    this.attentionColor = color;
    return this;
  }

  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getSurfaceColor() {
    return this.surfaceColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setSurfaceColor(int color) {
    this.surfaceColor = color;
    return this;
  }


  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getRecordingColor() {
    return this.recordingColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setRecordingColor(int color) {
    this.recordingColor = color;
    return this;
  }

  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getCursorColor() {
    return this.cursorColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setCursorColor(int color) {
    this.cursorColor = color;
    return this;
  }

  /**
   * Gets highlight color
   *
   * @return Highlight color
   */
  public int getSelectionColor() {
    return this.selectionColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setSelectionColor(int color) {
    this.selectionColor = color;
    return this;
  }

  /**
   * Gets dark background color
   *
   * @return Dark background color
   */
  public int getDarkBackgroundColor() {
    return this.darkBackgroundColor;
  }

  /**
   * Sets dark background color
   *
   * @param color Color
   * @return this
   */
  public UITheme setDarkBackgroundColor(int color) {
    this.darkBackgroundColor = color;
    return this;
  }

  /**
   * Gets dark background color
   *
   * @return Dark background color
   */
  public int getDarkFocusBackgroundColor() {
    return this.darkFocusBackgroundColor;
  }

  /**
   * Sets dark background color
   *
   * @param color Color
   * @return this
   */
  public UITheme setDarkFocusBackgroundColor(int color) {
    this.darkFocusBackgroundColor = color;
    return this;
  }

  /**
   * Gets focus color
   *
   * @return focus color
   */
  public int getFocusColor() {
    return this.focusColor;
  }

  /**
   * Sets highlight color
   *
   * @param color Color
   * @return this
   */
  public UITheme setFocusColor(int color) {
    this.focusColor = color;
    return this;
  }

  /**
   * Get active color
   *
   * @return Selection color
   */
  public int getSecondaryColor() {
    return this.secondaryColor;
  }

  /**
   * Set active color
   *
   * @param color Color
   * @return this
   */
  public UITheme setSecondaryColor(int color) {
    this.secondaryColor = color;
    return this;
  }

  /**
   * Get disabled color
   *
   * @return Disabled color
   */
  public int getControlDisabledColor() {
    return this.controlDisabledColor;
  }

  /**
   * Set disabled color
   *
   * @param color Color
   * @return this
   */
  public UITheme setControlDisabldColor(int color) {
    this.controlDisabledColor = color;
    return this;
  }

  /**
   * Get control background color
   *
   * @return color
   */
  public int getControlBackgroundColor() {
    return controlBackgroundColor;
  }

  /**
   * Set control background color
   *
   * @param controlBackgroundColor Color to set
   * @return this
   */
  public UITheme setControlBackgroundColor(int controlBackgroundColor ) {
    this.controlBackgroundColor = controlBackgroundColor;
    return this;
  }

  /**
   * Get control border color
   *
   * @return color
   */
  public int getControlBorderColor() {
    return this.controlBorderColor;
  }

  /**
   * Set control border color
   *
   * @param controlBorderColor color
   * @return this
   */
  public UITheme setControlBorderColor(int controlBorderColor) {
    this.controlBorderColor = controlBorderColor;
    return this;
  }


  /**
   * Get control highlight color
   *
   * @return color
   */
  public int getContextHighlightColor() {
    return this.contextHighlightColor;
  }

  /**
   * Set control highlight color
   *
   * @param contextHighlightColor color
   * @return this
   */
  public UITheme setContextHighlightColor(int contextHighlightColor) {
    this.contextHighlightColor = contextHighlightColor;
    return this;
  }

  /**
   * Control text color
   *
   * @return the controlTextColor
   */
  public int getControlTextColor() {
    return this.controlTextColor;
  }

  /**
   * Control text color
   *
   * @return the controlDisabledTextColor
   */
  public int getControlDisabledTextColor() {
    return this.controlDisabledTextColor;
  }

  /**
   * Set control text color
   *
   * @param controlTextColor color
   * @return this
   */
  public UITheme setControlTextColor(int controlTextColor) {
    this.controlTextColor = controlTextColor;
    return this;
  }

  /**
   * Default text color
   *
   * @return color
   */
  public int getLabelColor() {
    return this.labelColor;
  }

  /**
   * Set default text color
   *
   * @param labelColor color
   * @return this
   */
  public UITheme setLabelColor(int labelColor) {
    this.labelColor = labelColor;
    return this;
  }

  /**
   * Default text color
   *
   * @return color
   */
  public int getSublabelColor() {
    return this.sublabelColor;
  }

  /**
   * Set default text color
   *
   * @param sublabelColor color
   * @return this
   */
  public UITheme setSublabelColor(int sublabelColor) {
    this.sublabelColor = sublabelColor;
    return this;
  }

  public int getMidiMappingColor() {
    return this.midiMappingColor;
  }

  public UITheme setMidiMappingColor(int midiMappingColor) {
    this.midiMappingColor = midiMappingColor;
    return this;
  }

  public int getModulationSourceMappingColor() {
    return this.modulationSourceMappingColor;
  }

  public UITheme setModulationSourceMappingColor(int modulationSourceMappingColor) {
    this.modulationSourceMappingColor = modulationSourceMappingColor;
    return this;
  }

  public int getModulationTargetMappingColor() {
    return this.modulationTargetMappingColor;
  }

  public UITheme setModulationTargetMappingColor(int modulationTargetMappingColor) {
    this.modulationTargetMappingColor = modulationTargetMappingColor;
    return this;
  }

}
