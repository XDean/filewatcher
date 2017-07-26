package org.wenzhe.filewatcher.gui.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;
import org.wenzhe.filewatcher.dsl.Filter;
import org.wenzhe.filewatcher.dsl.Handler;
import org.wenzhe.filewatcher.dsl.Watcher;
import org.wenzhe.filewatcher.gui.Util;
import org.wenzhe.filewatcher.gui.controller.item.DslItemContainer;

import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.undoRedo.UndoRedoSupport;
import xdean.jfx.ex.support.undoRedo.Undoable;

@Slf4j
public class WorkPaneController extends ModifiableObject implements Initializable, Undoable {

  private static final ReadOnlyDoubleWrapper SPLIT_POS_PROPERTY = new ReadOnlyDoubleWrapper(0.8);

  private static final int GUI_TAB = 0;
  private static final int SRC_TAB = 1;

  @FXML
  SplitPane splitPane;

  @FXML
  TabPane tabPane;

  @FXML
  ScrollPane guiScrollPane;

  @FXML
  Pane headerContainer;

  @FXML
  Pane filterContainer;

  @FXML
  Pane handlerContainer;

  @FXML
  TextArea sourceArea;

  @FXML
  TextArea logArea;

  private Watcher watcher;
  private Queue<String> codeQueue;

  private UndoRedoSupport undoRedoSupport;
  private DslHeaderController header;
  private DslItemContainer<DslFilterController> filterItemContainer;
  private DslItemContainer<DslHandlerController> handlerItemContainer;
  private ModifiableObject reloadModel;
  private PrintStream printStream;
  private Undoable lastAction;// to check if clear modified marker

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    undoRedoSupport = UndoRedoSupport.get(this);
    UndoRedoSupport.setContext(undoRedoSupport);

    printStream = newLogPrintStream();
    reloadModel = new ModifiableObject();
    filterItemContainer = new DslItemContainer<>(DslFilterController.class, filterContainer);
    handlerItemContainer = new DslItemContainer<>(DslHandlerController.class, handlerContainer);

    splitPane.getDividers().get(0).positionProperty().bindBidirectional(SPLIT_POS_PROPERTY);
    tabPane.getSelectionModel().selectedIndexProperty().addListener((observable, o, n) -> {
      if (n.intValue() == GUI_TAB) {
        // If from source to GUI, reload the source text.
        if (reloadModel.isModified()) {
          reloadModel.saved(); // Has been reload.
          setModel(sourceArea.getText());
        }
      } else if (n.intValue() == SRC_TAB) {
        // If from GUI to source, rewrite source by GUI and it won't cause
        // modify.
        disableModified();
        reloadModel.disableModified();
        this.toText(false).ifPresent(text -> sourceArea.setText(text));
        reloadModel.enableModified();
        enableModified();
      }
    });
    initContextMenu();

    bindModified(sourceArea.textProperty());
    bindModified(filterItemContainer);
    bindModified(handlerItemContainer);
    reloadModel.bindModified(sourceArea.textProperty());

    UndoRedoSupport.removeContext();
  }

  private void initContextMenu() {
    ContextMenu menu = new ContextMenu();
    // CheckMenuItem lock = new CheckMenuItem("Lock scroll");
    // lock.selectedProperty().bindBidirectional(LOCK_LOG_PROPERTY);
    // logArea.scrollTopProperty().addListener((ob, o, n) -> {
    // if (LOCK_LOG_PROPERTY.get() && n.doubleValue() != logScrollPos.get()) {
    // logArea.setScrollTop(logScrollPos.get());
    // }
    // });
    // menu.getItems().add(lock);
    menu.getItems().add(Util.getSimpleMenuItem("Clear", null, e -> logArea.clear()));
    logArea.setContextMenu(menu);
  }

  @FXML
  private void addFilter() {
    try {
      filterItemContainer.add(filterItemContainer.create(), -1);
    } catch (IOException e) {
      log.error("Add filter fail", e);
    }
  }

  @FXML
  private void addHandler() {
    try {
      handlerItemContainer.add(handlerItemContainer.create(), -1);
    } catch (IOException e) {
      log.error("Add handler fail", e);
    }
  }

  boolean setModel(Watcher watcher, Queue<String> codeQueue) {
    undoRedoSupport.setAddable(false);
    UndoRedoSupport.setContext(undoRedoSupport);
    
    log.debug("seted: " + undoRedoSupport);
    this.watcher = watcher;
    this.codeQueue = codeQueue;
    constructHeader();
    constructFilter();
    constructHandler();
    
    UndoRedoSupport.removeContext();
    undoRedoSupport.setAddable(true);
    
    return true;
  }

  boolean setModel(String text) {
    Pair<FileWatcherDslContext, Queue<String>> p = null;
    try {
      p = Util.parseDsl(text);
    } catch (IOException e) {
      log.error("Parse code in source tab error.", e);
      return false;
    }
    FileWatcherDslContext context = p.getKey();
    if (context.getWatchers().size() == 1) {
      return setModel(context.getWatchers().get(0), p.getValue());
    } else {
      log.error("There is more than one watcher in one tab");
      return false;
    }
  }

  private void constructHeader() {
    log.debug("To construct header...");
    this.header = null;
    this.headerContainer.getChildren().clear();
    try {
      FXMLLoader loader = new FXMLLoader(Util.getFxmlResource("DslHeader.fxml"));
      Parent header = loader.load();
      DslHeaderController controller = loader.getController();

      controller.setData(watcher);

      headerContainer.getChildren().add(header);
      this.header = controller;
      bindModified(controller);
    } catch (IOException e) {
      log.error("Header construct fail", e);
    }
  }

  private void constructFilter() {
    log.debug("To construct filter...");
    filterItemContainer.clear();
    try {
      for (Filter filter : watcher.getFilters()) {
        log.debug("Construct filter" + filter);
        DslFilterController controller = filterItemContainer.create();
        controller.setData(filter, codeQueue);
        filterItemContainer.add(controller, -1);
      }
    } catch (IOException e) {
      log.error("Filter construct fail", e);
    }
  }

  private void constructHandler() {
    log.debug("To construct handler...");
    handlerItemContainer.clear();
    try {
      for (Handler handler : watcher.getHandlers()) {
        log.debug("Construct handler" + handler);
        DslHandlerController controller = handlerItemContainer.create();
        controller.setData(handler, codeQueue);
        handlerItemContainer.add(controller, -1);
      }
    } catch (IOException e) {
      log.error("Handler construct fail", e);
    }
  }

  /******************** Velocity ******************/
  public DslHeaderController getHeader() {
    return header;
  }

  public List<DslFilterController> getFilters() {
    return filterItemContainer.getItems();
  }

  public List<DslHandlerController> getHandlers() {
    return handlerItemContainer.getItems();
  }

  /*******************************************************************/
  public PrintStream getLogStream() {
    return printStream;
  }
  
  public UndoRedoSupport getUndoRedoSupport(){
    return undoRedoSupport;
  }

  public Optional<String> toText() {
    return toText(true);
  }

  @Override
  public void saved() {
    ObservableList<Undoable> undoList = undoRedoSupport.undoListProperty();
    if (undoList.isEmpty()) {
      lastAction = null;
    } else {
      lastAction = undoList.get(undoList.size() - 1);
    }
    super.saved();
  }
  
  public boolean isUndoable(){
    return undoRedoSupport.isUndoable();
  }
  
  public boolean isRedoable(){
    return undoRedoSupport.isRedoable();
  }

  @Override
  public Response undo() {
    Response result = undoRedoSupport.undo();
    checkModified();
    return result;
  }

  @Override
  public Response redo() {
    Response result = undoRedoSupport.redo();
    checkModified();
    return result;
  }
  
  private void checkModified(){
    ObservableList<Undoable> undoList = undoRedoSupport.undoListProperty();
    if (undoList.isEmpty()) {
      if (lastAction == null) {
        saved();
      } else {
        modified();
      }
    } else {
      Undoable nowLast = undoList.get(undoList.size() - 1);
      if (lastAction == nowLast) {
        saved();
      } else {
        modified();
      }
    }
  }

  /**
   * @param sync
   *          If true, it will sync source and GUI first and then do toText
   * @return
   */
  private Optional<String> toText(boolean sync) {
    if (sync) {
      if (tabPane.getSelectionModel().getSelectedIndex() == 1) {
        if (setModel(sourceArea.getText()) == false) {
          return Optional.empty();
        }
      }
    }
    return Optional.of(Util.toTextByVelocity("watcher.vm", "watcher", this));
  }

  private PrintStream newLogPrintStream() {
    return new PrintStream(new OutputStream() {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }

      @Override
      public void flush() throws IOException {
        super.flush();
        String text = baos.toString();
        Platform.runLater(() -> logArea.appendText(text));
        baos.reset();
      }
    });
  }
}
