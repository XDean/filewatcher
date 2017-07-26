package org.wenzhe.filewatcher.edd.watchservice;

import static org.wenzhe.filewatcher.dsl.Command.PipeType.output;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.createCommand;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.exclude;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.file;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.name;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.path;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.recursively;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.sync;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import lombok.SneakyThrows;

import org.wenzhe.filewatcher.FileWatcherExecutor;
import org.wenzhe.filewatcher.dsl.Command;

/**
 * @author wen-zhe.liu@asml.com
 *
 */
public class FileWatcher3 {

  private static final String p4bin = "C:\\Program Files\\Perforce";

  private static final String remoteRoot = "U:/pwo/dev/LSF/pwo/server/python/gui_interface";

  private static final String localRoot = "C:/Users/weliu/code/pwo/pwo/server/python/gui_interface";

  public static void main(String[] args) {

    FileWatcherExecutor.getInstance().execute(ctx -> {
      ctx
          .start(recursively).watch(remoteRoot)
          .filter(exclude).folder(path).equalsTo("gen-py")
          .filter(exclude).file(name).extension("pyo", "pyc")
          .on(file).modified(f -> copy(f))
          .on(file).deleted(f -> delete(f));

      ctx
          .start(recursively).watch(localRoot)
          .filter(exclude).folder(path).equalsTo("gen-py")
          .filter(exclude).file(name).extension("pyo", "pyc")
          .filter(exclude).when((f, updatedType) -> isFileReadOnly(f))
          .on(file).created(f -> addToP4(f))
          .on(file).modified(f -> modifedToP4(f))
          .on(file).deleted(f -> deleteFromP4(f));
    });
    while (true) {
      ;
    }
  }

  private static boolean isFileReadOnly(String f) {
    File file = new File(f);
    return file.exists() && !file.canWrite();
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
        // .environment("P4PASSWD", "xxxxxx")
        .environment("P4PORT", "p4proxy-brion.asml.com:1666")
        .pipe(output);

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

  @SneakyThrows
  private static void delete(String src) {
    Path srcPath = Paths.get(src);
    Path relative = Paths.get(remoteRoot).relativize(srcPath);
    Path target = Paths.get(localRoot).resolve(relative);

    if (Files.exists(target) && !Files.isWritable(target)) {
      target.toFile().setWritable(true);
    }
    Files.deleteIfExists(target);
    System.out.printf("deleted %s\n", target);
  }

  @SneakyThrows
  private static void copy(String src) {
    Path srcPath = Paths.get(src);
    Path relative = Paths.get(remoteRoot).relativize(srcPath);
    Path target = Paths.get(localRoot).resolve(relative);

    if (Files.exists(target) && !Files.isWritable(target)) {
      target.toFile().setWritable(true);
    }
    Files.write(target, Files.readAllBytes(srcPath), StandardOpenOption.CREATE);
    System.out.printf("copied %s to %s\n", srcPath, target);
  }

}
