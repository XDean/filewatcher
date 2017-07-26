package org.wenzhe.filewatcher.gui.controller;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;

import org.wenzhe.filewatcher.dsl.ConditionFilter;
import org.wenzhe.filewatcher.dsl.ContainFilter;
import org.wenzhe.filewatcher.dsl.EndFilter;
import org.wenzhe.filewatcher.dsl.EqualFilter;
import org.wenzhe.filewatcher.dsl.ExtensionFilter;
import org.wenzhe.filewatcher.dsl.FileType;
import org.wenzhe.filewatcher.dsl.Filter;
import org.wenzhe.filewatcher.dsl.FilterCondition;
import org.wenzhe.filewatcher.dsl.FilterType;
import org.wenzhe.filewatcher.dsl.MatchFilter;
import org.wenzhe.filewatcher.dsl.NamePath;
import org.wenzhe.filewatcher.dsl.StartFilter;
import org.wenzhe.filewatcher.gui.Util;
import org.wenzhe.filewatcher.gui.controller.item.DslItemController;

import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.undoRedo.UndoRedoSupport;

public class DslFilterController extends ModifiableObject implements DslItemController<DslFilterController> {

  @FXML
  ComboBox<String> filterTypeBox;

  @FXML
  ComboBox<String> fileTypeBox;

  @FXML
  ComboBox<String> nameOrPathBox;

  @FXML
  ComboBox<Class<? extends FilterCondition>> methodBox;

  @FXML
  CheckBox caseSensitiveCheck;

  @FXML
  Label hintLabel;

  @FXML
  TextArea codeArea;

  private Parent parent;
  private Consumer<DslFilterController> upListener;
  private Consumer<DslFilterController> deleteListener;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    filterTypeBox.getItems().addAll("include", "exclude");
    filterTypeBox.getSelectionModel().select(0);
    fileTypeBox.getItems().addAll("by", "file", "folder");
    fileTypeBox.getSelectionModel().select(0);
    nameOrPathBox.getItems().addAll("name", "path");
    nameOrPathBox.getSelectionModel().select(0);
    methodBox.getItems().addAll(filterMethodMap.keySet());
    Supplier<ListCell<Class<? extends FilterCondition>>> methodBoxListCellSupplier = () -> new ListCell<Class<? extends FilterCondition>>() {
      @Override
      protected void updateItem(Class<? extends FilterCondition> item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          return;
        }
        setText(getFilterName(item));
      }
    };
    methodBox.setCellFactory(listView -> methodBoxListCellSupplier.get());
    methodBox.setButtonCell(methodBoxListCellSupplier.get());
    methodBox.getSelectionModel().selectedItemProperty()
        .addListener((observable, o, n) -> {
          if (isShowHint(n)) {
            hintLabel.setVisible(true);
            caseSensitiveCheck.setVisible(false);
          } else {
            hintLabel.setVisible(false);
            caseSensitiveCheck.setVisible(isFilterCases(n));
          }
        });
    methodBox.getSelectionModel().select(1);
    methodBox.getSelectionModel().select(0);

    Util.textAreaFitHeight(codeArea);

    bindModified(filterTypeBox.getSelectionModel().selectedIndexProperty(),
        fileTypeBox.getSelectionModel().selectedIndexProperty(),
        nameOrPathBox.getSelectionModel().selectedIndexProperty(),
        methodBox.getSelectionModel().selectedIndexProperty(),
        caseSensitiveCheck.selectedProperty(),
        codeArea.textProperty());

    UndoRedoSupport.getContext().bind(caseSensitiveCheck);
    UndoRedoSupport.getContext().bind(filterTypeBox);
    UndoRedoSupport.getContext().bind(nameOrPathBox);
    UndoRedoSupport.getContext().bind(fileTypeBox);
    UndoRedoSupport.getContext().bind(methodBox);
    UndoRedoSupport.getContext().bind(codeArea);
  }

  void setData(Filter filter, Queue<String> codeQueue) {
    filterTypeBox.getSelectionModel().select(filter.getFilterType() == FilterType.INCLUDE ? "include" : "exclude");
    fileTypeBox.getSelectionModel().select(getFileType(filter));
    nameOrPathBox.getSelectionModel().select(filter.getNameOrPath() == NamePath.NAME ? "name" : "path");
    methodBox.getSelectionModel().select(filter.getFilterCondition().getClass());
    caseSensitiveCheck.setVisible(isFilterCases(filter.getFilterCondition().getClass()));
    caseSensitiveCheck.setSelected(!filter.isIgnoreCase());
    hintLabel.setVisible(isShowHint(filter.getFilterCondition().getClass()));
    Function<? super FilterCondition, String> getter = getFilterCodeGetter(filter.getFilterCondition().getClass());
    String code = null;
    if (getter != null) {
      code = getter.apply(filter.getFilterCondition());
    } else {
      code = codeQueue.poll();
    }
    codeArea.setText(code);
    saved();
  }

  @Override
  public void setUpListener(Consumer<DslFilterController> upListener) {
    this.upListener = upListener;
  }

  @Override
  public void setDeleteListener(Consumer<DslFilterController> deleteListener) {
    this.deleteListener = deleteListener;
  }

  @Override
  public void setParent(Parent p) {
    this.parent = p;
  }

  @Override
  public Parent getParent() {
    return parent;
  }

  String getFileType(Filter filter) {
    List<FileType> fileTypes = filter.getFileTypes();
    if (fileTypes.size() == 2) {
      return "by";
    } else {
      FileType fileType = fileTypes.get(0);
      return fileType == FileType.FILE ? "file" : "folder";
    }
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

  String getFilterName(Class<? extends FilterCondition> clz) {
    return filterMethodMap.get(clz).first;
  }

  boolean isFilterCases(Class<? extends FilterCondition> clz) {
    return (filterMethodMap.get(clz).second & 0x01) != 0;
  }

  boolean isShowHint(Class<? extends FilterCondition> clz) {
    return (filterMethodMap.get(clz).second & 0x10) != 0;
  }

  Function<? super FilterCondition, String> getFilterCodeGetter(Class<? extends FilterCondition> clz) {
    return filterMethodMap.get(clz).third;
  }

  /****************** For Velocity **************/
  public boolean isInclude() {
    return filterTypeBox.getSelectionModel().getSelectedItem().equals("include");
  }

  public String getFileType() {
    return fileTypeBox.getSelectionModel().getSelectedItem();
  }

  public String getNamePath() {
    return nameOrPathBox.getSelectionModel().getSelectedItem();
  }

  public boolean isCaseSensitive() {
    return !caseSensitiveCheck.isVisible() || caseSensitiveCheck.isSelected();
  }

  public String getMethod() {
    return getFilterName(methodBox.getSelectionModel().getSelectedItem());
  }

  public boolean isCodeWrapped() {
    if (getFilterCodeGetter(methodBox.getSelectionModel().getSelectedItem()) == null) {
      return true;
    } else {
      return false;
    }
  }

  public String getCode() {
    String text = codeArea.getText();
    if (isShowHint(methodBox.getSelectionModel().getSelectedItem())) {
      return hintLabel.getText() + "\n" + (Util.isEmtpy(text) ? "" : Util.tabAllLines(text));
    } else {
      return Util.isEmtpy(text) ? (getFilterCodeGetter(methodBox.getSelectionModel().getSelectedItem()) == null ? ""
          : "\"\"") : text;
    }
  }

  /**************************************************************/
  // The second integer in TripValue marked "when hint" and "casesSenesitive" at
  // HEX bit
  private static final Map<Class<? extends FilterCondition>, TripValue<String, Integer, Function<? super FilterCondition, String>>> filterMethodMap;

  static {
    Map<Class<? extends FilterCondition>, TripValue<String, Integer, Function<? super FilterCondition, String>>> map = new HashMap<>();
    map.put(ExtensionFilter.class, new TripValue<>("extension", 0x00,
        f -> combineValuesWithQuote(((ExtensionFilter) f).getExtensions())));
    map.put(ConditionFilter.class, new TripValue<>("when", 0x10, null));
    map.put(EqualFilter.class, new TripValue<>("equalsTo", 0x01,
        f -> combineValuesWithQuote(((EqualFilter) f).getValues())));
    map.put(MatchFilter.class, new TripValue<>("matches", 0x00,
        f -> String.format("\"%s\"", ((MatchFilter) f).getPattern().toString())));
    map.put(StartFilter.class, new TripValue<>("startsWith", 0x01,
        f -> combineValuesWithQuote(((StartFilter) f).getValues())));
    map.put(EndFilter.class, new TripValue<>("endsWith", 0x01,
        f -> combineValuesWithQuote(((EndFilter) f).getValues())));
    map.put(ContainFilter.class, new TripValue<>("contains", 0x01,
        f -> combineValuesWithQuote(((ContainFilter) f).getValues())));
    filterMethodMap = Collections.unmodifiableMap(map);
  }

  private static String combineValuesWithQuote(String... strs) {
    if (strs == null || strs.length == 0) {
      return "\"\"";
    }
    StringBuilder sb = new StringBuilder();
    for (String str : strs) {
      sb.append(String.format("\"%s\", ", str));
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  private static class TripValue<A, B, C> {
    final A first;
    final B second;
    final C third;

    public TripValue(A first, B second, C third) {
      super();
      this.first = first;
      this.second = second;
      this.third = third;
    }
  }

  @Override
  public String getControllerName() {
    return "Filter";
  }
}
