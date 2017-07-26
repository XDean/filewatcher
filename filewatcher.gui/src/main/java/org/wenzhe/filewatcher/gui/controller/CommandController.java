package org.wenzhe.filewatcher.gui.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.gui.Util;

@Slf4j
public class CommandController implements Initializable {

  private static CommandController INSTANCE;

  public static CommandController getInstance() {
    if (INSTANCE == null) {
      synchronized (CommandController.class) {
        if (INSTANCE == null) {
          try {
            Pair<CommandController, Pane> pair = Util.renderFxml(CommandController.class);
            INSTANCE = pair.getKey();
          } catch (IOException e) {
            log.error("Init Command Controller fail.", e);
            System.exit(1);
          }
        }
      }
    }
    return INSTANCE;
  }

  @FXML
  private Pane topPane;

  @FXML
  private RadioButton syncButton;

  @FXML
  private TextField workDirectoryField;

  @FXML
  private TextField pathField;

  @FXML
  private TextField environmentField;

  @FXML
  private TextArea commandArea;

  private volatile boolean done;

  private volatile Stage stage;
  private volatile Scene scene;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    scene = Util.createScene(topPane);
    pathField.setContextMenu(new ContextMenu(Util.getSimpleMenuItem("clear", null, e -> pathField.clear())));
  }

  @FXML
  private void selectWorkDirecotry() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose directory");
    File selectedFile = directoryChooser.showDialog(workDirectoryField.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    workDirectoryField.setText("\"" + selectedFile.getAbsolutePath() + "\"");
  }

  @FXML
  private void addPath() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose directory");
    File selectedFile = directoryChooser.showDialog(pathField.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    addPath(selectedFile.getAbsolutePath());
  }

  @FXML
  private void done() {
    done = true;
    stage.close();
  }

  @FXML
  private void cancel() {
    done = false;
    stage.close();
  }

  public void addPath(String path) {
    if (pathField.getText().length() > 0) {
      pathField.appendText(", ");
    }
    pathField.appendText("\"" + path + "\"");
  }

  public String showAndWait(Window owner) {
    commandArea.clear();
    stage = Util.createStage();
    stage.initModality(Modality.WINDOW_MODAL);
    stage.initOwner(owner);
    stage.setScene(scene);
    stage.showAndWait();

    if (done) {
      return Util.toTextByVelocity("command.vm", "command", this);
    } else {
      return "";
    }
  }

  /************************************************/
  public boolean isSync() {
    return syncButton.isSelected();
  }

  public String getWorkDirectory() {
    return workDirectoryField.getText();
  }

  public String getPath() {
    return pathField.getText();
  }

  public String getCommand() {
    return commandArea.getText();
  }

  public Map<String, String> getEnvironment() {
    String text = environmentField.getText();
    try {
      Map<String, String> collect = Stream.of(text.split(","))
          .map(String::trim)
          .map(s -> s.split("="))
          .collect(Collectors.toMap(ss -> ss[0].trim(), ss -> ss[1].trim()));
      return collect;
    } catch (Exception e) {
      log.error("Text cannot parse to key-value pair.", text);
    }
    return Collections.emptyMap();
  }
}
