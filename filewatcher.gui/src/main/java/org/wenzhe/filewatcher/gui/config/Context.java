package org.wenzhe.filewatcher.gui.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.gui.Util;

import rx.observables.JavaFxObservable;
import xdean.jex.config.Config;
import xdean.jfx.ex.support.skin.SkinManager;
import xdean.jfx.ex.support.skin.SkinStyle;

/**
 * LOG, CONFIG, SKIN
 * 
 * @author XDean
 *
 */
@Slf4j
public class Context {
  public static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".fw", "gui");
  public static final Path CONFIG_PATH = HOME_PATH.resolve("config.properties");
  public static final Path DEFAULT_CONFIG_PATH = Paths.get("/default_config.properties");

  public static final SkinManager SKIN = new SkinManager();
  static {
    prepare();
    loadConfig();
    loadSkin();
  }

  public static void loadClass() {
  }

  private static void prepare() {
    try {
      if (Files.notExists(HOME_PATH)) {
        Files.createDirectory(HOME_PATH);
      }
    } catch (IOException e) {
      log.error(String.format("Create %s fail.", HOME_PATH), e);
    }
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      log.error("Uncaught: ", e);
      Platform.runLater(() -> Util.showMessageDialog(null, "ERROR", e.toString()));
    });
  }

  private static void loadConfig() {
    Config.locate(CONFIG_PATH, DEFAULT_CONFIG_PATH);
  }

  private static void loadSkin() {
    // load default skins
    for (SkinStyle ss : DefaultSkin.values()) {
      SKIN.addSkin(ss);
    }
    // load skin files in /skin
    Path skinFolder = HOME_PATH.resolve("skin");
    try {
      if (Files.notExists(skinFolder)) {
        Files.createDirectory(skinFolder);
      } else {
        Files.newDirectoryStream(skinFolder).forEach(path -> {
          String fileName = path.getFileName().toString();
          if (!Files.isDirectory(path) && (fileName.endsWith(".css") ||
              fileName.endsWith(".bss"))) {
            String url = path.toUri().toString();
            String name = Util.onlyUpperFirst(fileName.substring(0, fileName.length() - 4));
            SKIN.addSkin(new SkinStyle() {
              @Override
              public String getURL() {
                return url;
              }

              @Override
              public String getName() {
                return name;
              }
            });
          }
        });
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    SKIN.getSkinList().stream().map(SkinStyle::getURL).map(s -> "loaded skin: " +
        s).forEach(log::debug);

    SKIN.changeSkin(DefaultSkin.CLASSIC);
    String configSkin = Config.getProperty(ConfigKey.SKIN, null);
    if (configSkin != null) {
      SKIN.getSkinList().stream()
          .filter(s -> s.getName().equals(configSkin))
          .findAny()
          .ifPresent(s -> SKIN.changeSkin(s));
    }
    JavaFxObservable.fromObservableValue(SKIN.skinProperty())
        .subscribe(skin -> Config.setProperty(ConfigKey.SKIN, skin.getName()));
  }
}
