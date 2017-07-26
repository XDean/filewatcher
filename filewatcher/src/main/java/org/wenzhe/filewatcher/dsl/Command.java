package org.wenzhe.filewatcher.dsl;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Shell command
 * 
 * @author wen-zhe.liu@asml.com
 *
 */
public class Command {
  
  private static final Logger log = LoggerFactory.getLogger(Command.class);
  
  private final ProcessBuilder processBuilder;
  
  private Process process;
  
  public static enum PipeType {
    output, error;
  }

  public Command(String[] cmd) {
    processBuilder = new ProcessBuilder(cmd).inheritIO();
  }

  /**
   * @param sync
   * @return exit code, normally 0 means correct, only meaningful in sync mode, always 0 for async mode
   */
  public int run(boolean async) {
    try {
      log.debug(processBuilder.command().toString());
      process = processBuilder.start();
      return async ? 0 : process.waitFor();
    } catch (IOException | InterruptedException e) {
      log.warn(e.getMessage(), e);
      return -1;
    }
  }

  public Command workingDirectory(String workDir, String... subDir) {
    processBuilder.directory(Paths.get(workDir, subDir).toFile());
    return this;
  }

  public Command findBinaryIn(String... binDirs) {
    String pathSeperator = SystemUtils.IS_OS_UNIX ? ":" : ";";
    Map<String, String> environment = processBuilder.environment();
    environment.keySet().stream()
    .filter(key -> key.equalsIgnoreCase("path"))
    .findFirst()
    .ifPresent(key -> environment.computeIfPresent(key, 
        (k, v) -> String.join(pathSeperator, binDirs) + pathSeperator + v));
    return this;
  }

  public Command environment(String key, String value) {
    processBuilder.environment().put(key, value);
    return this;
  }

  public Command redirectOutput(String outPath, String... subPath) {
    processBuilder.redirectOutput(Paths.get(outPath, subPath).toFile());
    return this;
  }
  
  public Command redirectError(String errPath, String... subPath) {
    processBuilder.redirectError(Paths.get(errPath, subPath).toFile());
    return this;
  }
  
  public Command pipe(PipeType type) {
    switch (type) {
    case error:
      processBuilder.redirectError(Redirect.PIPE);
      break;
    case output:
      processBuilder.redirectOutput(Redirect.PIPE);
      break;
    }
    return this;
  }

  /**
   * created after run command, usually use in asyn run
   */
  @Nullable
  public Process getProcess() {
    return process;
  }

  public ProcessBuilder getProcessBuilder() {
    return processBuilder;
  }
}
