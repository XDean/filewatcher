package org.wenzhe.filewatcher.edd.watchservice;

import static org.wenzhe.filewatcher.dsl.FileMode.readOnly;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.createCommand;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.exclude;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.file;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.include;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.name;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.path;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.recursively;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.sync;

import java.io.File;

import org.wenzhe.filewatcher.FileWatcherExecutor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author wen-zhe.liu@asml.com
 * 
 *         Update to P4
 *
 */
public class FwJarInjector {

  private static final String localRoot = "C:/Users/weliu/code/pwo/pwo";

  private static final String javaDir = "C:/Program Files/Java/jdk1.8.0_45/bin";

  private static final String jarInjectorDir = "C:/Documents and Settings/weliu/code/jarinjector/target";

  private static final boolean remoteMode = false;

  public static void main(String[] args) {

    FileWatcherExecutor.getInstance().execute(ctx -> ctx

        .start(recursively).watch(localRoot)
        .filter(include).folder(path).equalsTo(
            localRoot + "/gui"
        )
        .filter(include).file(path).equalsTo(
            localRoot + "/version.dat"
        )
        .filter(exclude).folder(path).contains(
            "/gen-py/", "/target/", "/bin/", "/.settings/"
        )
        .filter(exclude).folder(path).equalsTo(
            localRoot + "/com.asml.pwo.mdp.gui.doc/"
        )
        .filter(exclude).file(name).extension(
            "pyo", "pyc", ".class", "jar"
        )
        .filter(exclude).file(readOnly)
        // .on(file).modified(f -> injectJar(f))
        .on(file).updated((f, t) -> {
          String msg = f + " " + t + "\r\n";
          System.out.print(msg);
          try {
            Files.append(msg, new File("C:/users/weliu/pwo_file_update.txt"), Charsets.UTF_8);
          } catch (Exception e) {
            e.printStackTrace();
          }
        })
        );
    while (true) {
      ;
    }
  }

  @SuppressWarnings("unused")
  private static void injectJar(String f) {
    String[] cmd;
    if (remoteMode) {
      cmd = new String[] { "java", "-jar", "jarinjector.jar", f,
          localRoot + "/gui/com.asml.pwo.mdp.gui.ui.thirdparty/lib"
          // ...
      };
    } else {
      cmd = new String[] { "java", "-jar", "jarinjector.jar", f,
          localRoot + "/gui/com.asml.pwo.mdp.gui.ui.thirdparty/lib" };
    }

    createCommand(cmd)
        .workingDirectory(jarInjectorDir)
        .findBinaryIn(javaDir)
        .run(sync);
  }
}
