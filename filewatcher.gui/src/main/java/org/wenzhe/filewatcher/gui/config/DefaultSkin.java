package org.wenzhe.filewatcher.gui.config;

import org.wenzhe.filewatcher.gui.Util;

import xdean.jfx.ex.support.skin.SkinStyle;

public enum DefaultSkin implements SkinStyle {
  BASE("base"),
  CLASSIC("classic"),
  DARK("dark"),
  METAL("metal");

  private static final String CSS_PATH = "/css/skin/";

  private String path;

  private DefaultSkin(String name) {
    try {
      this.path = DefaultSkin.class.getResource(CSS_PATH + name + ".css").toExternalForm();
    } catch (NullPointerException e) {// If the resource not exist
      this.path = DefaultSkin.class.getResource(CSS_PATH + name + ".bss").toExternalForm();
    }
  }

  @Override
  public String getURL() {
    return path;
  }

  @Override
  public String getName() {
    return Util.onlyUpperFirst(toString());
  }
}