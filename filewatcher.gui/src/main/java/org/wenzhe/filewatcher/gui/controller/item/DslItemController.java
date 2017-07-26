package org.wenzhe.filewatcher.gui.controller.item;

import java.util.function.Consumer;

import javafx.fxml.Initializable;
import javafx.scene.Parent;

public interface DslItemController<S extends DslItemController<S>> extends Initializable {

  Parent getParent();

  void setParent(Parent parent);

  void setUpListener(Consumer<S> c);

  void setDeleteListener(Consumer<S> c);
  
  String getControllerName();
}
