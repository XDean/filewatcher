package org.wenzhe.filewatcher.gui.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import org.wenzhe.filewatcher.dsl.Watcher;

import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.undoRedo.UndoRedoSupport;

public class DslHeaderController extends ModifiableObject implements Initializable {

  @FXML
  ComboBox<String> actionBox;

  @FXML
  ComboBox<String> modeBox;

  @FXML
  TextField pathField;

  @FXML
  Button chooseButton;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    actionBox.getItems().addAll("start", "stop");
    modeBox.getItems().addAll("to", "recursively");
    bindModified(actionBox.getSelectionModel().selectedIndexProperty(),
        modeBox.getSelectionModel().selectedIndexProperty(),
        pathField.textProperty());
    
    UndoRedoSupport.getContext().bind(actionBox);
    UndoRedoSupport.getContext().bind(modeBox);
    UndoRedoSupport.getContext().bind(pathField);
  }

  @FXML
  void chooseDirectory() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose directory");
    File selectedFile = directoryChooser.showDialog(pathField.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    pathField.setText(selectedFile.getAbsolutePath());
  }

  void setData(Watcher watcher) {
    this.actionBox.getSelectionModel().select(watcher.isStart() ? "start" : "stop");
    this.modeBox.getSelectionModel().select(watcher.isRecursively() ? "recursively" : "to");
    this.pathField.setText(watcher.getWatchedFile().toString());
    saved();
  }

  /****************** For Velocity **************/
  public boolean isStart() {
    return actionBox.getSelectionModel().getSelectedItem().equals("start");
  }

  public boolean isRecursively() {
    return modeBox.getSelectionModel().getSelectedItem().equals("recursively");
  }

  public String getPath() {
    return pathField.getText();
  }
}
