package org.wenzhe.filewatcher.dsl;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author wen-zhe.liu@asml.com
 *
 */
public class FileModeFilter extends FilterCondition {

  private final FileMode mode;

  public FileModeFilter(FileMode mode) {
    this.mode = mode;
  }
  
  @Override
  protected boolean filter(Path path, NamePath namePath) {
    if (!Files.exists(path)) {
      return true;
    }
    boolean readable = Files.isReadable(path);
    boolean writable = Files.isWritable(path);
    return mode == FileMode.from(readable, writable);
  }

  @Override
  protected boolean filter(String nameOrPath) {
    throw new UnsupportedOperationException();
  }

}
