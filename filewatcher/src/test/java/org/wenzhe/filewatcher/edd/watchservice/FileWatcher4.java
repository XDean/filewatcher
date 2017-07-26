package org.wenzhe.filewatcher.edd.watchservice;

import static org.wenzhe.filewatcher.dsl.Command.PipeType.error;
import static org.wenzhe.filewatcher.dsl.FileMode.readOnly;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.createCommand;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.exclude;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.file;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.include;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.name;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.path;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.recursively;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.sync;
import lombok.SneakyThrows;

import org.wenzhe.filewatcher.FileWatcherExecutor;
import org.wenzhe.filewatcher.dsl.Command;

/**
 * @author wen-zhe.liu@asml.com
 * 
 * Update to P4
 *
 */
public class FileWatcher4 {

  private static final String p4bin = "C:\\Program Files\\Perforce";

  private static final String localRoot = "C:/Users/weliu/code/pwo/pwo";
  
  public static void main(String[] args) {

    FileWatcherExecutor.getInstance().execute(ctx -> ctx

        .start(recursively).watch(localRoot)
        .filter(include).folder(path).equalsTo(
            localRoot + "/gui", 
            localRoot + "/server/python/gui_interface",
            localRoot + "/xsd"
        )
        .filter(include).file(path).equalsTo(
            localRoot + "/Makefile", 
            localRoot + "/version.dat"
        )
        .filter(exclude).folder(path).contains(
            "/gen-py/", "/target/", "/bin/", "/.settings/"
        )
        .filter(exclude).folder(path).equalsTo(
            localRoot + "/com.asml.pwo.mdp.gui.doc/",
            localRoot + "/com.asml.pwo.mdp.gui.dao.xml/src/main/resources/xsd"
        )
        .filter(exclude).file(name).extension(
            "pyo", "pyc", ".class", "jar"
        )
        .filter(exclude).file(readOnly)
        .on(file).created(f -> addToP4(f))
        .on(file).modified(f -> modifedToP4(f))
        .on(file).deleted(f -> deleteFromP4(f))
    );
    while(true) {
      ;
    }
  }

  private static void deleteFromP4(String updatedFile) {
    runP4Cmd("p4.exe", "revert", updatedFile);
  }
  
  private static int doModifedToP4(String updatedFile) {
    return runP4Cmd("p4.exe", "edit", "-c", "default", updatedFile);
  }

  private static void modifedToP4(String updatedFile) {
    if (doModifedToP4(updatedFile) != 0) {
      addToP4(updatedFile);
    }
  }

  private static void addToP4(String updatedFile) {
    runP4Cmd("p4.exe", "add", "-d", "-f", "-c", "default", updatedFile);
  }
  
  @SneakyThrows
  private static int runP4Cmd(String... cmd) {
    Command p4cmd = createCommand(cmd)
        .findBinaryIn(p4bin)
        .environment("P4CLIENT", "pwo_main")
        .environment("P4USER", "weliu")
        //.environment("P4PASSWD", "xxxxxx")
        .environment("P4PORT", "p4proxy-brion.asml.com:1666")
        .pipe(error)
        ;
        
    int exitCode = p4cmd.run(sync);
    if (exitCode == 0) {
      byte[] errBytes = new byte[1024];
      int byteCount = p4cmd.getProcess().getErrorStream().read(errBytes);
      if (byteCount != -1) {
        System.err.println(new String(errBytes, 0, byteCount));
        return -1;
      }
    }
    return 0;
  }

}
