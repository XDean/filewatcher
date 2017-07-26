package org.wenzhe.filewatcher.gui.controller;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.util.Pair;

import org.wenzhe.filewatcher.dsl.FileType;
import org.wenzhe.filewatcher.dsl.Handler;
import org.wenzhe.filewatcher.dsl.UpdateType;
import org.wenzhe.filewatcher.gui.Util;
import org.wenzhe.filewatcher.gui.controller.item.DslItemController;

import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.undoRedo.UndoRedoSupport;

public class DslHandlerController extends ModifiableObject implements DslItemController<DslHandlerController> {

  @FXML
  ComboBox<String> fileTypeBox;

  @FXML
  ComboBox<UpdateType> updateTypeBox;

  @FXML
  Label hintLabel;

  @FXML
  TextArea codeArea;

  private Parent parent;
  private Consumer<DslHandlerController> upListener;
  private Consumer<DslHandlerController> deleteListener;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    fileTypeBox.getItems().addAll("file", "folder", "file and folder");
    fileTypeBox.getSelectionModel().select(0);

    updateTypeBox.getItems().addAll(updateTypeMap.keySet());
    updateTypeBox.getSelectionModel().select(0);
    Supplier<ListCell<UpdateType>> methodBoxListCellSupplier = () -> new ListCell<UpdateType>() {
      @Override
      protected void updateItem(UpdateType item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          return;
        }
        setText(getUpdateTypeName(item));
      }
    };
    updateTypeBox.setCellFactory(listView -> methodBoxListCellSupplier.get());
    updateTypeBox.setButtonCell(methodBoxListCellSupplier.get());
    updateTypeBox.getSelectionModel().selectedItemProperty()
        .addListener((ob, o, n) -> hintLabel.setText(getUpdateTypeHint(n)));
    hintLabel.setText(getUpdateTypeHint(updateTypeBox.getSelectionModel().getSelectedItem()));

    Util.textAreaFitHeight(codeArea);
    bindModified(fileTypeBox.getSelectionModel().selectedIndexProperty(),
        updateTypeBox.getSelectionModel().selectedIndexProperty(),
        codeArea.textProperty());

    UndoRedoSupport.getContext().bind(fileTypeBox);
    UndoRedoSupport.getContext().bind(updateTypeBox);
    UndoRedoSupport.getContext().bind(codeArea);
  }

  void setData(Handler handler, Queue<String> codeQueue) {
    fileTypeBox.getSelectionModel().select(getFileType(handler));
    updateTypeBox.getSelectionModel().select(handler.getUpdateType());
    hintLabel.setText(getUpdateTypeHint(handler.getUpdateType()));
    codeArea.setText(codeQueue.poll());
    saved();
  }

  @FXML
  void up() {
    if (upListener != null) {
      upListener.accept(this);
    }
  }

  @FXML
  void delete() {
    if (deleteListener != null) {
      deleteListener.accept(this);
    }
  }

  @FXML
  void addCommand() {
    CommandController controller = CommandController.getInstance();
    String cmd = controller.showAndWait(codeArea.getScene().getWindow());
    if (!Util.isEmtpy(cmd)) {
      codeArea.insertText(codeArea.getCaretPosition(), cmd + "\n");
    }
  }

  @FXML
  void print() {
    codeArea.insertText(codeArea.getCaretPosition(), "println \"\"\n");
  }

  @FXML
  void async() {
    codeArea.insertText(codeArea.getCaretPosition(), "async {\n}\n");
  }

  /*****************************************************/
  @Override
  public Parent getParent() {
    return parent;
  }

  @Override
  public void setParent(Parent parent) {
    this.parent = parent;
  }

  @Override
  public void setUpListener(Consumer<DslHandlerController> upListener) {
    this.upListener = upListener;
  }

  @Override
  public void setDeleteListener(Consumer<DslHandlerController> deleteListener) {
    this.deleteListener = deleteListener;
  }

  String getUpdateTypeName(UpdateType type) {
    return updateTypeMap.get(type).getKey();
  }

  String getUpdateTypeHint(UpdateType type) {
    return updateTypeMap.get(type).getValue();
  }

  /****************** For Velocity **************/
  String getFileType(Handler handler) {
    List<FileType> fileTypes = handler.getFileTypes();
    if (fileTypes.size() == 2) {
      return "file and folder";
    } else {
      FileType fileType = fileTypes.get(0);
      return fileType == FileType.FILE ? "file" : "folder";
    }
  }

  public String getFileType() {
    return fileTypeBox.getSelectionModel().getSelectedItem();
  }

  public String getUpdateType() {
    return getUpdateTypeName(updateTypeBox.getSelectionModel().getSelectedItem());
  }

  public String getCode() {
    String text = codeArea.getText();
    return hintLabel.getText() + "\n" + (Util.isEmtpy(text) ? "" : Util.tabAllLines(text));
  }

  /***************************************************/

  private static final Map<UpdateType, Pair<String, String>> updateTypeMap;
  static {
    Map<UpdateType, Pair<String, String>> map = new HashMap<>();
    map.put(UpdateType.CREATED, new Pair<>("created", "updatedFile ->"));
    map.put(UpdateType.MODIFIED, new Pair<>("modified", "updatedFile ->"));
    map.put(UpdateType.UPDATED, new Pair<>("updated", "updatedFile, updatedType ->"));
    map.put(UpdateType.DELETED, new Pair<>("deleted", "updatedFile ->"));
    updateTypeMap = Collections.unmodifiableMap(map);
  }

  @Override
  public String getControllerName() {
    return "Handler";
  }
}
