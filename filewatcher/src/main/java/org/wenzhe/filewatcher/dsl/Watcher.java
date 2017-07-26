package org.wenzhe.filewatcher.dsl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.val;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class Watcher {
  
  private static final Logger log = LoggerFactory.getLogger(Watcher.class);
  
  private boolean start;
  private boolean recursively;
  private Path watchedFile;
  
  private final List<Handler> handlers = new ArrayList<>();
  private final List<Filter> filters = new ArrayList<>();
  
  public Watcher watch(String path, String... subPath) {
    log.debug("watch {}", path);
    watchedFile = Paths.get(path, subPath).toAbsolutePath();
    return this;
  }
  
  public Handler on(FileType fileType) {
    log.debug("on {}", fileType);
    val handler = new Handler(this, fileType);
    handlers.add(handler);
    return handler;
  }
  
  public Filter filter(FilterType filterType) {
    log.debug("filter {}", filterType);
    val ft = new Filter(this, filterType);
    filters.add(ft);
    return ft;
  }

  public boolean isStart() {
    return start;
  }

  public void setStart(boolean start) {
    this.start = start;
  }

  public boolean isRecursively() {
    return recursively;
  }

  public void setRecursively(boolean recursively) {
    this.recursively = recursively;
  }

  public Path getWatchedFile() {
    return watchedFile;
  }

  public void setWatchedFile(Path watchedFile) {
    this.watchedFile = watchedFile;
  }

  public List<Handler> getHandlers() {
    return ImmutableList.copyOf(handlers);
  }

  public List<Filter> getFilters() {
    return ImmutableList.copyOf(filters);
  }
}
