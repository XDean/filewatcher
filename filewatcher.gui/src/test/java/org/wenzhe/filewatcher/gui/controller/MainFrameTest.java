package org.wenzhe.filewatcher.gui.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.hasText;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.api.FxService;
import org.testfx.api.FxToolkit;
import org.testfx.service.support.WaitUntilSupport;
import org.wenzhe.filewatcher.gui.Main;

public class MainFrameTest {

  Application app;
  Stage stage;
  FxRobot robot;
  WaitUntilSupport wait;

  @Before
  public void setup() throws Exception {
    stage = FxToolkit.registerPrimaryStage();
    app = FxToolkit.setupApplication(Main.class);
    robot = new FxRobot();
    wait = FxService.serviceContext().getWaitUntilSupport();
  }

  @Test
  public void testNew() {
    robot.clickOn("#newButton");
    FxAssert.<TabPane> verifyThat("#contentTabPane", t -> t.getTabs().size() == 2);
  }

  @Test
  public void testRun() throws IOException {
    String dir = System.getProperty("user.dir");
    if (!dir.endsWith("\\")) {
      dir += "\\";
    }

    robot.clickOn("#newButton")
        .clickOn("#actionBox").clickOn("start")
        .clickOn("#modeBox").clickOn("to")
        .clickOn("#pathField").push(KeyCode.CONTROL, KeyCode.A).type(KeyCode.DELETE).write(dir)
        .clickOn("#fileTypeBox").clickOn("file")
        .clickOn("#updateTypeBox").clickOn("updated")
        .clickOn("#codeArea").write("println \"$updatedFile+$updatedType\"")
        .clickOn("#runButton")
        .clickOn(ButtonType.NO.getText());
    verifyThat("#runButton", isDisabled());
    verifyThat("#stopButton", isEnabled());
    wait.<TextField> waitUntil(robot.lookup("#msgField").queryFirst(), t -> t.getText().contains("run"), 3000);

    String fileName = "temp" + System.nanoTime() + ".tmp";
    Path path = Paths.get(dir, fileName);
    Files.createFile(path);
    robot.sleep(1000);
    Files.delete(path);

    robot.clickOn("#stopButton");
    verifyThat("#logArea", allOf(
        hasText(containsString(String.format("%s+%s", dir + fileName, "created"))),
        hasText(containsString(String.format("%s+%s", dir + fileName, "deleted")))));
    verifyThat("#runButton", isEnabled());
    verifyThat("#stopButton", isDisabled());
  }

  @Test
  public void testCommand() {
    robot.clickOn("#newButton")
        .clickOn("#addCommandButton")
        .clickOn("#asyncButton")
        .clickOn("#commandArea")
        .write("cmd /c dir")
        .clickOn("#doneButton");
    verifyThat("#codeArea", hasText("command \"cmd\", \"/c\", \"dir\" run async\n"));
  }
}
