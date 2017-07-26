package org.wenzhe.filewatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class FileWatchEvent {
  private final Path path;
  private final Kind<?> kind;
  
  public FileWatchEvent(Path path, Kind<?> kind) {
    this.path = path;
    this.kind = kind;
  }

  public boolean isModified() {
    return kind == StandardWatchEventKinds.ENTRY_MODIFY;
  }
  
  public boolean isCreated() {
    return kind == StandardWatchEventKinds.ENTRY_CREATE;
  }
  
  public boolean isDeleted() {
    return kind == StandardWatchEventKinds.ENTRY_DELETE;
  }
  
  public boolean exists() {
    return Files.exists(path);
  }
  
  public boolean isDirectory() {
    return Files.isDirectory(path);
  }
  
  public boolean isFile() {
    return exists() && !isDirectory();
  }

  public Path getPath() {
    return path;
  }

  public Kind<?> getKind() {
    return kind;
  }
}