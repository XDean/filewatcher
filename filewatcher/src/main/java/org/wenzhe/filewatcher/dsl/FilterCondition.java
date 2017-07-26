package org.wenzhe.filewatcher.dsl;

import java.nio.file.Files;
import java.nio.file.Path;

import org.wenzhe.filewatcher.FileWatchEvent;


/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public abstract class FilterCondition {
  
  public final boolean filter(FileWatchEvent evt, Filter filter) {
    if (filter.getFileTypes().contains(FileType.FOLDER)) {
      Path path = evt.getPath();
      if (Files.isDirectory(path) || filter.getFileTypes().contains(FileType.FILE)) {
        if (doFilter(evt, filter)) {
          return true;
        }
      }
      while ((path = path.getParent()) != null) {
        if (doFilter(new FileWatchEvent(path, evt.getKind()), filter)) {
          return true;
        }
      }
      return false;
    } else {
      return doFilter(evt, filter);
    }
  }
  
  protected boolean doFilter(FileWatchEvent evt, Filter filter) {
    return filter(evt.getPath(), filter.getNameOrPath());
  }

  protected boolean filter(Path path, NamePath namePath) {
    return filter(namePath == NamePath.NAME ? path.getFileName().toString() : 
      path.toString().replace("\\", "/"));
  }
  
  protected abstract boolean filter(String nameOrPath);

}
