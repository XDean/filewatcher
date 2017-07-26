package org.wenzhe.filewatcher.gui.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.FileWatchEvent;
import org.wenzhe.filewatcher.dsl.FileType;
import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;
import org.wenzhe.filewatcher.dsl.FilterType;
import org.wenzhe.filewatcher.dsl.NamePath;
import org.wenzhe.filewatcher.dsl.Watcher;
import org.wenzhe.filewatcher.gui.Util;
import org.wenzhe.filewatcher.gui.config.ConfigKey;
import org.wenzhe.filewatcher.gui.config.Context;
import org.wenzhe.filewatcher.gui.fw.FileWatcherDslContextExtension;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import xdean.jex.config.Config;
import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.RecentFileMenuSupport;
import xdean.jfx.ex.support.skin.SkinStyle;

import com.google.common.io.Files;

@Slf4j
public class MainFrameController extends ModifiableObject implements Initializable {

  private static StringProperty message = new ReadOnlyStringWrapper();

  static void message(String msg) {
    message.set(msg);
  }

  @FXML
  Menu skinMenu;

  @FXML
  TabPane contentTabPane;

  @FXML
  TextField msgField;

  @FXML
  MenuItem undoItem, redoItem, runItem, stopItem, closeItem, saveItem, saveAsItem, revertItem;

  @FXML
  Button undoButton, redoButton, runButton, stopButton, newButton, openButton, saveButton;

  @FXML
  Menu openRecentMenu;;

  @FXML
  Tab addTab;

  private Stage stage;
  private ObservableList<WorkPaneController> workPanes;
  private ObjectProperty<WorkPaneController> currentWorkPane;

  private Queue<String> emptyQueue;
  private Watcher emptyWatcher;
  private Pair<FileWatcherDslContext, Queue<String>> newFileData;

  private ReadOnlyObjectWrapper<File> currentFile = new ReadOnlyObjectWrapper<File>();
  private RecentFileMenuSupport recentSupport;
  private final File newFile = new File("");

  private ReadOnlyBooleanWrapper isRunning;
  private Subscription runSubscriber;
  private Subscription watchSelfSubscriber;

  /************************************ Initialize ************************************/
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initField();
    initBind();
    loadSkinMenu();
    message("Ready");
  }

  private void initField() {
    recentSupport = new RecentFileMenuSupport(openRecentMenu) {
      @Override
      public List<String> load() {
        return Arrays.asList(Config.getProperty(ConfigKey.RECENT_LOCATIONS, "").split(","));
      }

      @Override
      public void save(List<String> s) {
        Config.setProperty(ConfigKey.RECENT_LOCATIONS, String.join(", ", s));
      }
    };
    // TODO consider file not exist
    recentSupport.setOnAction(file -> {
      if (askToSaveAndShouldContinue()) {
        currentFile.set(file);
      }
    });
    workPanes = FXCollections.observableArrayList();
    isRunning = new ReadOnlyBooleanWrapper(this, "running", false);
    currentWorkPane = new SimpleObjectProperty<>();

    emptyQueue = new ArrayDeque<String>() {
      @Override
      public String poll() {
        return "";
      }

      @Override
      public String peek() {
        return "";
      }
    };
    FileWatcherDslContext context = new FileWatcherDslContext();
    emptyWatcher = context.start(true).watch("").on(FileType.FILE).created(null);
    newFileData = new Pair<FileWatcherDslContext, Queue<String>>(context, emptyQueue);
    contentTabPane.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
      if (n == addTab) {
        contentTabPane.getSelectionModel().select(o);
      }
    });
  }

  private void initBind() {
    currentFile.addListener((ob, o, n) -> {
      _onFileChange(o, n);
      onFileChange(o, n);
    });
    closeItem.disableProperty().bind(currentFile.isNull());
    saveAsItem.disableProperty().bind(currentFile.isNull());
    saveItem.disableProperty().bind(saveAsItem.disableProperty()
        .or(modifiedProperty().not()));
    saveButton.disableProperty().bind(saveItem.disableProperty());
    revertItem.disableProperty().bind(currentFile.isNull()
        .or(modifiedProperty().not())
        .or(Bindings.createBooleanBinding(() -> isNewFile(currentFile.get()), currentFile)));

    msgField.textProperty().bind(message);

    runItem.disableProperty().bind(currentFile.isNull().or(isRunning));
    runButton.disableProperty().bind(runItem.disableProperty());
    stopItem.disableProperty().bind(currentFile.isNull().or(isRunning.not()));
    stopButton.disableProperty().bind(stopItem.disableProperty());

    currentWorkPane.bind(Bindings.createObjectBinding(() -> getCurrentWorkPane(),
        contentTabPane.getSelectionModel().selectedItemProperty()));

    undoItem.disableProperty().bind(new BooleanBinding() {
      private WorkPaneController last;
      {
        last = currentWorkPane.get();
        bind(currentWorkPane);
        bind(last);
      }

      @Override
      protected boolean computeValue() {
        WorkPaneController now = currentWorkPane.get();
        if (now != last) {
          unbind(last);
          bind(now);
          last = now;
        }
        if (now == null) {
          return true;
        }
        return now.getUndoRedoSupport().isUndoable() == false;
      }

      private void bind(WorkPaneController controller) {
        if (controller != null) {
          bind(controller.getUndoRedoSupport().undoListProperty());
        }
      }

      private void unbind(WorkPaneController controller) {
        if (controller != null) {
          unbind(controller.getUndoRedoSupport().undoListProperty());
        }
      }
    });
    undoButton.disableProperty().bind(undoItem.disableProperty());
    redoItem.disableProperty().bind(new BooleanBinding() {
      private WorkPaneController last;
      {
        last = currentWorkPane.get();
        bind(currentWorkPane);
        bind(last);
      }

      @Override
      protected boolean computeValue() {
        WorkPaneController now = currentWorkPane.get();
        if (now != last) {
          unbind(last);
          bind(now);
          last = now;
        }
        if (now == null) {
          return true;
        }
        return now.getUndoRedoSupport().isRedoable() == false;
      }

      private void bind(WorkPaneController controller) {
        if (controller != null) {
          bind(controller.getUndoRedoSupport().redoListProperty());
        }
      }

      private void unbind(WorkPaneController controller) {
        if (controller != null) {
          unbind(controller.getUndoRedoSupport().redoListProperty());
        }
      }
    });
    redoButton.disableProperty().bind(redoItem.disableProperty());
  }

  /************************************ FXML Handler **********************************/
  public void onFileChange(File o, File n) {
    if (n == null) {
      // null means closed
      setTitle(null);
      workPanes.clear();
      contentTabPane.getTabs().clear();
      contentTabPane.getTabs().add(addTab);
    } else {
      if (isNewFile(n)) {
        setTitle("new");
      } else {
        setTitle(n.getAbsolutePath());
      }
      openFile(n);
      saved();
    }
  }

  @FXML
  public void clearRecent() {
    recentSupport.clear();
  }

  @FXML
  public void close() {
    if (askToSaveAndShouldContinue()) {
      currentFile.set(null);
    }
  }

  @FXML
  public boolean save() {
    if (isNewFile(currentFile.get())) {
      return saveAs();
    } else {
      return saveToFile(currentFile.get());
    }
  }

  @FXML
  public boolean saveAs() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save");
    fileChooser.getExtensionFilters().add(new ExtensionFilter("File Watcher", "*.fw"));
    fileChooser.setInitialDirectory(recentSupport.getLastFile().getParentFile());
    File selectedFile = fileChooser.showSaveDialog(stage);
    if (selectedFile == null) {
      return false;
    }
    boolean saveToFile = saveToFile(selectedFile);
    if (saveToFile) {
      currentFile.set(selectedFile);
    }
    return saveToFile;
  }

  @FXML
  public void revert() {
    File file = currentFile.get();
    close();
    if (askToSaveAndShouldContinue()) {
      currentFile.set(file);
    }
  }

  @FXML
  public void exit() {
    if (askToSaveAndShouldContinue()) {
      stage.close();
    }
  }

  @FXML
  public void addTab() {
    if (currentFile.get() == null) {
      newFile();
    } else {
      try {
        WorkPaneController controller = constructWorkPane();
        controller.setModel(emptyWatcher, emptyQueue);
        controller.saved();
        contentTabPane.getTabs().remove(addTab);
        contentTabPane.getTabs().add(contentTabPane.getTabs().size(), addTab);
        contentTabPane.getSelectionModel().select(contentTabPane.getTabs().size() - 2);
      } catch (Exception e) {
        log.error("Add tab error.", e);
      }
    }
  }

  @FXML
  public void newFile() {
    if (askToSaveAndShouldContinue()) {
      currentFile.set(newFile);
    }
    saved();
    message("New");
  }

  @FXML
  public void open() {
    if (askToSaveAndShouldContinue() == false) {
      return;
    }
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open");
    fileChooser.getExtensionFilters().add(new ExtensionFilter("File Watcher", "*.fw"));
    // TODO consider folder not exist
    fileChooser.setInitialDirectory(recentSupport.getLastFile().getParentFile());
    File selectedFile = fileChooser.showOpenDialog(stage);
    if (selectedFile != null && askToSaveAndShouldContinue()) {
      currentFile.set(selectedFile);
    }
    message("Open " + currentFile.get());
  }

  @FXML
  public void undo() {
    currentWorkPane.get().undo();
  }

  @FXML
  public void redo() {
    currentWorkPane.get().redo();
  }

  @FXML
  public void help() {
    Util.showMessageDialog(stage, "Help", "Send email to dean.xu@asml.com for help.");
  }

  @FXML
  public void about() {
    // TODO About
  }

  @FXML
  public void runFw() {
    if (askToSaveAndShouldContinue() == false) {
      return;
    }
    runFilewatcher().subscribe(e -> {
    }, ex -> {
      ex.printStackTrace();
      log.error("Illegal text to parse to FileWatcherContext", ex);
      isRunning.set(false);
      message("Run fail");
    }, () -> {
      if (isRunning.get()) {
        message("FileWatcher now running");
        if (currentFile.get() != null && isNewFile(currentFile.get()) == false) {
          watchSelfSubscriber = watchSelf().subscribe();
        }
      } else {
        message("Text complie error, run fail");
      }
    });
    ;
  }

  @FXML
  public void stopFw() {
    if (watchSelfSubscriber != null) {
      watchSelfSubscriber.unsubscribe();
      watchSelfSubscriber = null;
    }
    if (runSubscriber != null) {
      runSubscriber.unsubscribe();
      runSubscriber = null;
    }
    isRunning.set(false);
    message("FileWatcher stoped");
  }

  /*********************************** Public Method **********************************/
  public void setStage(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(e -> {
      e.consume();
      exit();
    });
  }

  /*********************************** Private Method *********************************/

  /******************* Runtime *****************/
  private Observable<FileWatcherDslContext> runFilewatcher() {
    return Observable.just(this)
        .map(MainFrameController::toText)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .subscribeOn(Schedulers.immediate())
        .observeOn(Schedulers.io())
        .doOnNext(e -> isRunning.set(true))
        .map(text -> Util.uncheck(() -> Util.parseDsl(text)))
        .map(Pair::getKey)
        .doOnNext(context -> {
          if (context instanceof FileWatcherDslContextExtension) {
            FileWatcherDslContextExtension extension = (FileWatcherDslContextExtension) context;
            for (int i = 0; i < workPanes.size(); i++) {
              extension.setPrintStream(extension.getWatchers().get(i), workPanes.get(i).getLogStream());
            }
          }
          runSubscriber = Util.runFileWatcherContext(context).subscribe();
        })
        .observeOn(Schedulers.computation());
  }

  private Observable<FileWatchEvent> watchSelf() {
    File file = currentFile.get();
    FileWatcherDslContext context = new FileWatcherDslContext();
    context.start(false).watch(file.getParent())
        .filter(FilterType.INCLUDE).file(NamePath.NAME).equalsTo(file.getName())
        .on(FileType.FILE).modified(path -> {
          runSubscriber.unsubscribe();
          runFilewatcher().subscribe(e -> {
          }, ex -> message("Updated file fail"), () -> message("FileWatcher updated"));
        });
    return Util.runFileWatcherContext(context);
  }

  /******************** File *******************/

  private void openFile(File file) {
    if (isNewFile(file)) {
      updateWorkPane(newFileData);
    } else if (file.exists() == false) {
      Util.showMessageDialog(stage, "Error", "File " + file + " not exists.");
    } else {
      updateWorkPane(file);
    }
  }

  private Optional<String> toText() {
    List<Optional<String>> map = workPanes.stream().map(WorkPaneController::toText).collect(Collectors.toList());
    if (map.stream().filter(o -> !o.isPresent()).findAny().isPresent()) {
      return Optional.empty();
    }
    return map.stream().map(Optional::get).reduce((s1, s2) -> String.format("%s\n\n%s", s1, s2));
  }

  /********************* UI *******************/
  private void updateWorkPane(File dslFile) {
    Pair<FileWatcherDslContext, Queue<String>> pair = null;
    try {
      pair = Util.parseDsl(dslFile);
    } catch (IOException e) {
      log.error("Parse dsl file error.", e);
      return;
    }
    updateWorkPane(pair);
  }

  private void updateWorkPane(Pair<FileWatcherDslContext, Queue<String>> pair) {
    workPanes.clear();
    contentTabPane.getTabs().clear();

    contentTabPane.getTabs().add(addTab);
    FileWatcherDslContext dslContext = pair.getKey();
    dslContext.getWatchers().forEach(
        watcher -> {
          try {
            WorkPaneController controller = constructWorkPane();
            controller.setModel(watcher, pair.getValue());
          } catch (IOException e) {
            e.printStackTrace();
            log.error("Construct workPane error.", e);
          }
        });
    contentTabPane.getTabs().remove(addTab);
    contentTabPane.getTabs().add(addTab);
  }

  private WorkPaneController constructWorkPane() throws IOException {
    Pair<WorkPaneController, Parent> pair = Util.renderFxml(WorkPaneController.class);
    Parent workPane = pair.getValue();
    WorkPaneController controller = pair.getKey();
    String title = "FileWatcher" + (contentTabPane.getTabs().size());
    Tab tab = Util.getSimpleTab(title, null, workPane);
    tab.setUserData(controller);
    contentTabPane.getTabs().add(tab);
    workPanes.add(controller);
    bindModified(controller);
    controller.modifiedProperty().addListener(
        (observable, o, n) -> tab.setText(n ? ("*" + title) : title));
    tab.setOnClosed(e -> workPanes.remove(controller));
    tab.setOnCloseRequest(e -> {
      if (Util.showConfirmDialog(stage, "Delete Confirm",
          "The tab will be deleted and its data will be lost. Continue?") == false) {
        e.consume();
      } else {
        modified();
      }
    });
    return controller;
  }

  private void setTitle(String title) {
    if (stage == null) {
      return;
    }
    if (title == null || title.trim().length() == 0) {
      stage.setTitle("File Watcher GUI");
    } else {
      stage.setTitle("File Watcher GUI - " + title);
    }
  }

  private WorkPaneController getCurrentWorkPane() {
    try {
      WorkPaneController userData = (WorkPaneController) contentTabPane
          .getSelectionModel().getSelectedItem().getUserData();
      return userData;
    } catch (NullPointerException e) {
      return null;
    }
  }

  /******************** Skin *******************/

  private void loadSkinMenu() {
    ToggleGroup group = new ToggleGroup();
    for (SkinStyle style : Context.SKIN.getSkinList()) {
      RadioMenuItem item = new RadioMenuItem(style.getName());
      item.setToggleGroup(group);
      item.setOnAction(e -> Context.SKIN.changeSkin(style));
      if (Context.SKIN.currentSkin() == style) {
        item.setSelected(true);
      }
      skinMenu.getItems().add(item);
    }
  }

  private void _onFileChange(File o, File n) {
    if (n != null && n != newFile) {
      recentSupport.setLastFile(n);
    }
    saved();
  }

  protected boolean saveToFile(File file) {
    try {
      byte[] data = toText().orElse("Save error").getBytes();
      Files.write(data, file);
      saved();
      return true;
    } catch (UnsupportedOperationException e) {
      throw e;
    } catch (IOException e) {
      log.error("Save fail.");
      return false;
    }
  }

  protected boolean askToSaveAndShouldContinue() {
    if (isModified()) {
      ButtonType result = Util.showConfirmCancelDialog(stage, "Save", "This file has been modified. Save changes?");
      if (result.equals(ButtonType.YES)) {
        return save();
      }
      return result.equals(ButtonType.NO);
    }
    return true;
  }

  protected boolean isNewFile(File file) {
    return file == newFile;
  }
}
