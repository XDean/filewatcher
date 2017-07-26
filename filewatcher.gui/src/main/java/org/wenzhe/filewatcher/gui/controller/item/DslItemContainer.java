package org.wenzhe.filewatcher.gui.controller.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.gui.Util;

import xdean.jfx.ex.extra.ModifiableObject;
import xdean.jfx.ex.support.undoRedo.UndoRedoSupport;
import xdean.jfx.ex.support.undoRedo.UndoUtil;
import xdean.jfx.ex.support.undoRedo.Undoable;

@Slf4j
public class DslItemContainer<C extends ModifiableObject & DslItemController<C>> extends ModifiableObject {
  List<C> controllers;
  Pane container;
  UndoRedoSupport undoRedoSupport;
  Class<C> controllerClass;

  public DslItemContainer(Class<C> controllerClass, Pane container) {
    this.undoRedoSupport = UndoRedoSupport.getContext();
    this.controllerClass = controllerClass;
    this.container = container;
    this.controllers = new ArrayList<>();
  }

  public void clear() {
    controllers.clear();
    container.getChildren().clear();
  }

  public C create() throws IOException {
    UndoRedoSupport.setContext(undoRedoSupport);
    Pair<C, Parent> pair = Util.renderFxml(controllerClass);
    C controller = pair.getKey();

    controller.setParent(pair.getValue());
    controller.setUpListener(this::up);
    controller.setDeleteListener(this::delete);
    bindModified(controller);
    UndoRedoSupport.removeContext();

    return controller;
  }

  public void add(C controller, int pos) {
    if (pos < 0) {
      pos = container.getChildren().size() + pos + 1;
    }
    int position = pos;
    Parent parent = controller.getParent();
    controllers.add(pos, controller);
    container.getChildren().add(pos, parent);

    undoRedoSupport.add(Undoable.create(
        UndoUtil.<DslItemContainer<C>, C> weakConsumer(this, (contain, contro) -> contain.add(contro, position)),
        UndoUtil.<DslItemContainer<C>, C> weakPredicate(this, DslItemContainer::delete),
        controller));
    modified();
  }

  public boolean delete(C controller) {
    if (false == Util.showConfirmDialog(getWindow(), "Delete Confirm",
        String.format("The %s will be deleted. Continue?", controller.getControllerName()))) {
      return false;
    }
    if (controllers.indexOf(controller) == -1) {
      log.error("The item has been deleted. It can't be deleted again. Check code.");
      return false;
    }
    int position = container.getChildren().indexOf(controller.getParent());
    controllers.remove(controllers.indexOf(controller));
    container.getChildren().remove(position);

    undoRedoSupport.add(Undoable.create(
        UndoUtil.<DslItemContainer<C>, C> weakPredicate(this, DslItemContainer::delete),
        UndoUtil.<DslItemContainer<C>, C> weakConsumer(this, (contain, control) -> contain.add(control, position)),
        controller));
    modified();

    return true;
  }

  public void up(C controller) {
    int index = controllers.indexOf(controller);
    if (index == 0) {
      return;
    }
    Parent pane = controller.getParent();
    int paneIndex = container.getChildren().indexOf(pane);
    controllers.remove(index);
    controllers.add(index - 1, controller);
    container.getChildren().remove(pane);
    container.getChildren().add(paneIndex - 1, pane);

    C underController = controllers.get(index);
    undoRedoSupport.add(Undoable.create(UndoUtil.weakConsumer(this, DslItemContainer::up),
        controller, underController));
    modified();
  }

  public List<C> getItems() {
    return controllers;
  }

  private Window getWindow() {
    return container.getScene().getWindow();
  }
}
