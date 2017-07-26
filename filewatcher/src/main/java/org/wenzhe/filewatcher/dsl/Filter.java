package org.wenzhe.filewatcher.dsl;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wenzhe.filewatcher.FileWatchEvent;

import rx.functions.Func2;

import com.google.common.collect.ImmutableList;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class Filter {

  private static final Logger log = LoggerFactory.getLogger(Filter.class);
  
  private final Watcher watcher;
  private final FilterType filterType;
  
  @Nullable private FilterCondition filterCondition;
  private List<FileType> fileTypes = Arrays.asList(FileType.FILE, FileType.FOLDER);
  private NamePath nameOrPath = NamePath.NAME;
  private boolean ignoreCase = false;
  
  public Filter(Watcher watcher, FilterType filterType) {
    this.watcher = watcher;
    this.filterType = filterType;
  }

  public Filter by(FileType fileType) {
    log.debug("by {}", fileType);
    fileTypes = Arrays.asList(fileType);
    return this;
  }
  
  public Filter by(NamePath nameOrPath) {
    log.debug("by {}", nameOrPath);
    this.nameOrPath = nameOrPath;
    return this;
  }
  
  public Filter folder(NamePath nameOrPath) {
    return by(FileType.FOLDER).by(nameOrPath);
  }
  
  public Filter file(NamePath nameOrPath) {
    return by(FileType.FILE).by(nameOrPath);
  }
  
  public Watcher file(FileMode mode) {
    log.debug("file {}", mode);
    by(FileType.FILE);
    filterCondition = new FileModeFilter(mode);
    return watcher;
  }
  
  public Filter cases(boolean ignoreCase) {
    log.debug("cases {}", ignoreCase ? "insensitive" : "sensitive");
    this.ignoreCase = ignoreCase;
    return this;
  }
  
  public Watcher extension(String... extensions) {
    if (log.isDebugEnabled()) {
      log.debug("extension {}", Arrays.toString(extensions));
    }
    filterCondition = new ExtensionFilter(extensions);
    return watcher;
  }
  
  public Watcher when(Func2<String, String, Boolean> condition) {
    log.debug("when condition");
    filterCondition = new ConditionFilter(condition);
    return watcher;
  }
  
  public Watcher equalsTo(String... values) {
    if (nameOrPath == NamePath.PATH) {
      values = Arrays.stream(values)
          .map(v -> v.replace("\\", "/"))
          .toArray(String[]::new);
    }
    if (log.isDebugEnabled()) {
      log.debug("equals to {}", Arrays.toString(values));
    }
    filterCondition = new EqualFilter(values, ignoreCase);
    return watcher;
  }
  
  public Watcher startsWith(String... values) {
    if (log.isDebugEnabled()) {
      log.debug("starts with {}", Arrays.toString(values));
    }
    filterCondition = new StartFilter(values, ignoreCase);
    return watcher;
  }
  
  public Watcher endsWith(String... values) {
    if (log.isDebugEnabled()) {
      log.debug("ends with {}", Arrays.toString(values));
    }
    filterCondition = new EndFilter(values, ignoreCase);
    return watcher;
  }
  
  public Watcher contains(String... values) {
    if (log.isDebugEnabled()) {
      log.debug("contains {}", Arrays.toString(values));
    }
    filterCondition = new ContainFilter(values, ignoreCase);
    return watcher;
  }
  
  public Watcher matches(String pattern) {
    log.debug("matches {}", pattern);
    filterCondition = new MatchFilter(Pattern.compile(pattern, 
        ignoreCase ? Pattern.CASE_INSENSITIVE : 0));
    return watcher;
  }

  public boolean filter(FileWatchEvent evt) {
    if (filterCondition == null) {
      return true;
    }
    return filterCondition.filter(evt, this);
  }

  public Watcher getWatcher() {
    return watcher;
  }

  public FilterType getFilterType() {
    return filterType;
  }

  public FilterCondition getFilterCondition() {
    return filterCondition;
  }

  public List<FileType> getFileTypes() {
    return ImmutableList.copyOf(fileTypes);
  }

  public NamePath getNameOrPath() {
    return nameOrPath;
  }

  public boolean isIgnoreCase() {
    return ignoreCase;
  }
}
