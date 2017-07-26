package org.wenzhe.filewatcher.gui.fw;

import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.dsl.Command;
import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;
import org.wenzhe.filewatcher.dsl.Watcher;

import rx.schedulers.Schedulers;

@Slf4j
public class FileWatcherDslContextExtension extends FileWatcherDslContext {

  private class CommandExtension extends Command {

    public CommandExtension(String[] cmd) {
      super(cmd);
    }

    @Override
    public int run(boolean async) {
      pipe(PipeType.output);
      pipe(PipeType.error);
      getProcessBuilder().redirectErrorStream(true);
      super.run(true);
      Process process = getProcess();
      Watcher watcher = watcherLocal.get();
      Schedulers.computation().createWorker().schedule(() -> {
        if (process != null) {
          lock(watcher);
          InputStream input = process.getInputStream();
          Scanner scanner = new Scanner(input, System.getProperty("sun.jnu.encoding"));
          while (scanner.hasNextLine()) {
            println(scanner.nextLine());
          }
          scanner.close();
          unlock();
        }
      });
      try {
        return async ? 0 : process.waitFor();
      } catch (InterruptedException e) {
        log.warn(e.getMessage(), e);
        return -1;
      }
    }
  }

  private final ThreadLocal<Watcher> watcherLocal;
  private final ThreadLocal<SimpleDateFormat> format;
  private final Map<Watcher, PrintStream> printMap;

  public FileWatcherDslContextExtension() {
    watcherLocal = new ThreadLocal<>();
    format = new ThreadLocal<SimpleDateFormat>() {
      @Override
      protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat("HH:mm:ss.SSS");
      }
    };
    printMap = new HashMap<>();
  }

  @Override
  public Command command(String... cmd) {
    return new CommandExtension(cmd);
  }

  void lock(Watcher watcher) {
    watcherLocal.set(watcher);
  }

  void unlock() {
    watcherLocal.set(null);
  }

  public void setPrintStream(Watcher w, PrintStream p) {
    printMap.put(w, p);
  }

  PrintStream getPrintStream() {
    Watcher watcher = watcherLocal.get();
    if (watcher != null) {
      return printMap.get(watcher);
    } else {
      return System.out;
    }
  }

  public void print(Object value) {
    printTime();
    getPrintStream().print(value);
    getPrintStream().flush();
  }

  public void printf(String format, Object value) {
    printTime();
    getPrintStream().printf(format, value);
    getPrintStream().flush();
  }

  public void printf(String format, Object[] values) {
    printTime();
    getPrintStream().printf(format, values);
    getPrintStream().flush();
  }

  public void println() {
    printTime();
    getPrintStream().println();
    getPrintStream().flush();
  }

  public void println(Object value) {
    printTime();
    getPrintStream().println(value);
    getPrintStream().flush();
  }

  private void printTime() {
    getPrintStream().printf("[%s]  ", format.get().format(new Date()));
  }
}
