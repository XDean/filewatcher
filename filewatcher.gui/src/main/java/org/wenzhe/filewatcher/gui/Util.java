package org.wenzhe.filewatcher.gui;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.wenzhe.filewatcher.FileWatchEvent;
import org.wenzhe.filewatcher.FileWatcherExecutor;
import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;
import org.wenzhe.filewatcher.gui.config.Context;
import org.wenzhe.filewatcher.gui.fw.FileWatcherDslContextExtension;
import org.wenzhe.filewatcher.gui.fw.FileWatcherExecutorExtension;
import org.wenzhe.filewatcher.gui.velocity.VelocityMacroLibrary;

import rx.Observable;

@Slf4j
public class Util {

  /************************** File Watcher **********************************/

  private static final String CODE_START_REGEX = "(?m)("
      + "(^filter ((exclude)|(include)) (\\w* \\w* )?when ?\\{(.|\\R)*?->)|"
      + "(^on ((file)|(folder)|(file and folder)) ((created)|(modified)|(updated)|(deleted)) ?\\{(.|\\R)*?->))";
  private static final String CODE_END_REGEX = "(?m)^\\} *(\\\\)?$";
  private static final Pattern CODE_START_PATTERN = Pattern.compile(CODE_START_REGEX);
  private static final Pattern CODE_END_PATTERN = Pattern.compile(CODE_END_REGEX);

  public static Pair<FileWatcherDslContext, Queue<String>> parseDsl(File dslFile) throws IOException {
    return parseDsl(dslFile.toPath());
  }

  public static Pair<FileWatcherDslContext, Queue<String>> parseDsl(Path dslPath) throws IOException {
    return parseDsl(readFileAsString(dslPath));
  }

  public static Pair<FileWatcherDslContext, Queue<String>> parseDsl(String dslText) throws IOException {
    FileWatcherDslContext context = new FileWatcherDslContextExtension();
    // FileWatcherDslContext context = new FileWatcherDslContext();
    Binding binding = new Binding();
    binding.setProperty("context", context);
    binding.setProperty("out", context);
    CompilerConfiguration configuration = new CompilerConfiguration();
    String groovyCode = String.format("context.with {%s}", dslText);
    Script dslScript = new GroovyShell(binding, configuration).parse(groovyCode);
    try {
      dslScript.run();
    } catch (GroovyRuntimeException e) {
      throw new IOException(e);
    }

    Matcher startMatcher = CODE_START_PATTERN.matcher(dslText);

    Matcher endMatcher = CODE_END_PATTERN.matcher(dslText);

    Queue<String> queue = new ArrayDeque<>();
    while (true) {
      boolean startFind = startMatcher.find();
      boolean endFind = endMatcher.find();
      if (!(startFind || endFind)) {
        break;
      }
      if (startFind ^ endFind) {
        String msg = String.format("This dslText is not standard format, can't parse it.");
        log.error(msg);
        throw new IOException(msg);
      }
      String code = dslText.substring(startMatcher.end(), endMatcher.start());
      code = deleteBlankLine(untabAllLines(untabAllLines(code)));
      queue.add(code);

    }
    return new Pair<>(context, queue);
  }

  public static Observable<FileWatchEvent> runFileWatcherContext(FileWatcherDslContext context) {
    if (context instanceof FileWatcherDslContextExtension) {
      return new FileWatcherExecutorExtension((FileWatcherDslContextExtension) context).run();
    } else {
      return FileWatcherExecutor.getInstance().run(context);
    }
  }

  /************************* JavaFX ***********************************/

  public static Stage createStage() {
    Stage stage = new Stage();
    stage.getIcons().add(new Image(Util.class.getClassLoader().getResourceAsStream("icon/icon.png")));
    return stage;
  }

  public static Scene createScene(Parent root){
    Scene scene = new Scene(root);
    Context.SKIN.bind(scene);
    return scene;
  }
  
  public static <T> Dialog<T> createDialog() {
    Dialog<T> dialog = new Dialog<>();
    ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(
        new Image(Util.class.getClassLoader().getResourceAsStream("icon/icon.png")));
    Context.SKIN.bind(dialog);
    return dialog;
  }

  public static Tab getSimpleTab(String text, Node graphic, Node content) {
    Tab tab = new Tab();
    if (text != null) {
      tab.setText(text);
    }
    if (graphic != null) {
      tab.setGraphic(graphic);
    }
    if (content != null) {
      tab.setContent(content);
    }
    return tab;
  }

  public static MenuItem getSimpleMenuItem(String text, Node graphic, Consumer<ActionEvent> eventHandler) {
    MenuItem item = new MenuItem();
    if (text != null) {
      item.setText(text);
    }
    if (graphic != null) {
      item.setGraphic(graphic);
    }
    if (eventHandler != null) {
      item.setOnAction(eventHandler::accept);
    }
    return item;
  }

  public static void showMessageDialog(Window window, String title, String message) {
    Dialog<ButtonType> dialog = createDialog();
    if (window != null) {
      dialog.initOwner(window);
    }
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
    dialog.setTitle(title);
    dialog.setContentText(message);
    dialog.showAndWait();
  }

  public static boolean showConfirmDialog(Window window, String title, String message) {
    Dialog<ButtonType> dialog = createDialog();
    if (window != null) {
      dialog.initOwner(window);
    }
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setTitle(title);
    dialog.setContentText(message);
    return dialog.showAndWait().orElse(ButtonType.CANCEL).equals(ButtonType.OK);
  }

  /**
   * 
   * @param window
   * @param title
   * @param message
   * @return {@code ButtonType.YES, ButtonType.NO, ButtonType.CANCEL}
   */
  public static ButtonType showConfirmCancelDialog(Window window, String title, String message) {
    Dialog<ButtonType> dialog = createDialog();
    if (window != null) {
      dialog.initOwner(window);
    }
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
    dialog.setTitle(title);
    dialog.setContentText(message);
    return dialog.showAndWait().orElse(ButtonType.CANCEL);
  }

  public static void printAllWithId(Node n, int space) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < space; i++) {
      sb.append("-");
    }
    sb.append(String.format("id:%s  %s", n.getId(), n));
    log.debug(sb.toString());
    if (n instanceof Parent) {
      ((Parent) n).getChildrenUnmodifiable().forEach(node -> {
        printAllWithId(node, space + 1);
      });
    }
  }

  public static URL getFxmlResource(String fileName) throws IOException {
    if (!fileName.endsWith(".fxml")) {
      fileName += ".fxml";
    }
    return Util.class.getClassLoader().getResource("fxml/" + fileName);
  }

  public static <C extends Initializable, P> Pair<C, P> renderFxml(Class<C> controllerClass) throws IOException {
    String clzName = controllerClass.getSimpleName();
    String suffix = "Controller";
    if (!clzName.endsWith(suffix)) {
      throw new IOException("Class must named like \"xxxController\".");
    } else {
      return renderFxml(clzName.substring(0, clzName.length() - suffix.length()));
    }
  }

  public static <C extends Initializable, P> Pair<C, P> renderFxml(String fileName) throws IOException {
    FXMLLoader loader = new FXMLLoader(Util.getFxmlResource(fileName));
    P filterPane = loader.load();
    C controller = loader.getController();
    return new Pair<>(controller, filterPane);
  }

  public static void textAreaFitHeight(TextArea textArea) {
    Text text = new Text();
    textArea.textProperty().addListener((observable, o, n) -> {
      text.setWrappingWidth(textArea.getWidth());
      text.setText(n);
      textArea.setPrefHeight(text.getLayoutBounds().getHeight() + 9);
    });
  }
  
  /************************** Velocity ********************************/
  private static final VelocityEngine VELOCITY_ENGINE;
  static {
    VELOCITY_ENGINE = new VelocityEngine();
    try {
      Properties p = new Properties();
      p.load(Util.class.getClassLoader().getResourceAsStream("velocity.properties"));
      VELOCITY_ENGINE.init(p);
    } catch (IOException e) {
      log.error("\"velocity.properties\" not found! Init fail!");
    }
  }

  public static String toTextByVelocity(String vmFileName, String key, Object value) {
    Map<String, Object> map = new HashMap<>();
    map.put(key, value);
    return toTextByVelocity(vmFileName, map);
  }

  public static String toTextByVelocity(String vmFileName, Map<String, Object> context) {
    if (!vmFileName.endsWith(".vm")) {
      vmFileName += ".vm";
    }
    Template t = VELOCITY_ENGINE.getTemplate(vmFileName);
    VelocityContext vc = new VelocityContext(context);
    vc.put("lib", VelocityMacroLibrary.INSTANCE);
    StringWriter sw = new StringWriter();
    t.merge(vc, sw);
    return sw.toString();
  }

  /***************************** RxJava *****************************/
  public static interface TaskWithException<T> {
    T call() throws Exception;
  }

  public static <T> T uncheck(TaskWithException<T> task) {
    try {
      return task.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /************************** Common ********************************/
  public static String readFileAsString(Path path) throws IOException {
    return new String(Files.readAllBytes(path), "UTF8");
  }

  public static boolean isEmtpy(String str) {
    return str == null || Util.deleteBlankLine(str).equals("");
  }

  public static String toEscapeString(String str) {
    return str.replaceAll("\\\\", "\\\\\\\\");
  }

  private static final Pattern BLANK_BEGIN_PATTERN = Pattern.compile("^(\\s*)");
  private static final Pattern BLANK_END_PATTERN = Pattern.compile("(\\s*)$");

  public static String deleteBlankLine(String str) {
    Matcher m = BLANK_BEGIN_PATTERN.matcher(str);
    if (m.find()) {
      str = str.substring(m.end());
    }
    m = BLANK_END_PATTERN.matcher(str);
    if (m.find()) {
      str = str.substring(0, m.start());
    }
    return str;
  }

  public static String tabAllLines(String str) {
    return str.replaceAll("(?m)^", "\t");
  }

  public static String untabAllLines(String str) {
    return str.replaceAll("(?m)^\t", "");
  }

  public static String onlyUpperFirst(String st) {
    return st.substring(0, 1).toUpperCase() + st.substring(1).toLowerCase();
  }

  public static String wrapCommand(String cmd) {
    List<Integer> quoteIndexes = getIndexes(cmd, "\"", true, 0, cmd.length());
    List<Integer> cutPos = new ArrayList<>();
    quoteIndexes.add(0, 0);
    quoteIndexes.add(cmd.length());
    for (int i = 0; i < quoteIndexes.size() - 1; i += 2) {
      cutPos.addAll(getIndexes(cmd, " ", false,
          quoteIndexes.get(i), quoteIndexes.get(i + 1)));
    }
    List<String> cutStrings = cutStringByPoint(cmd, cutPos);
    StringBuilder sb = new StringBuilder();
    cutStrings.forEach(str -> {
      if (str.startsWith("\"") && str.endsWith("\"")) {
        str = String.format("\\\"%s\\\"", str.subSequence(1, str.length() - 1));
      }
      sb.append(String.format("\"%s\", ", str));
    });
    sb.deleteCharAt(sb.length() - 1);
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  public static List<Integer> getIndexes(String str, String c, boolean ignoreEscape,
      int beginIndex, int endIndex) {
    if (c.equals("\\") && ignoreEscape) {
      throw new RuntimeException("Method getAllIndex can't handle '\\'.");
    }
    if (endIndex > str.length()) {
      throw new StringIndexOutOfBoundsException(endIndex);
    }
    if (beginIndex >= endIndex) {
      return new ArrayList<>();
    }
    beginIndex = Math.max(0, beginIndex);

    List<Integer> list = new ArrayList<>();
    int fromIndex = beginIndex;
    while (fromIndex >= beginIndex && fromIndex < endIndex) {
      int index = str.indexOf(c, fromIndex);
      if (index == -1 || index >= endIndex) {
        break;
      }
      if (ignoreEscape && list.size() % 2 != 0) {
        int pre = 0;
        while (str.charAt(index - (++pre)) == '\\') {
          ;
        }
        if (pre % 2 == 0) {
          break;
        }
      }
      list.add(index);
      fromIndex = index + 1;
    }
    return list;
  }

  public static List<String> cutStringByPoint(String str, List<Integer> cutPos) {
    List<String> list = new ArrayList<>();

    cutPos.add(0, -1);
    cutPos.add(str.length());

    for (int i = 0; i < cutPos.size() - 1; i += 1) {
      String substring = str.substring(cutPos.get(i) + 1, cutPos.get(i + 1));
      if (substring == null || substring.equals("")) {
        continue;
      }
      list.add(substring);
    }
    return list;
  }
}
