package org.wenzhe.filewatcher.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Pair;

import org.wenzhe.filewatcher.gui.config.Context;
import org.wenzhe.filewatcher.gui.controller.MainFrameController;

public class Main extends Application {

  public static void main(String[] args) {
    Context.loadClass();
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {

    Pair<MainFrameController, Pane> pair = Util.renderFxml(MainFrameController.class);
    Pane root = pair.getValue();
    pair.getKey().setStage(stage);

    Scene scene = Util.createScene(root);

    stage.setTitle("File Watcher GUI");
    stage.setScene(scene);
    stage.setMaximized(true);
    stage.show();

    stage.getIcons().add(new Image(Main.class.getClassLoader().getResourceAsStream("icon/icon.png")));
  }
}
