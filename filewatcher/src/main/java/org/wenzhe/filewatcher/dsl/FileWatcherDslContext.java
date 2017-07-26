package org.wenzhe.filewatcher.dsl;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import com.google.common.collect.ImmutableList;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class FileWatcherDslContext {

  private static final Logger log = LoggerFactory.getLogger(FileWatcherDslContext.class);

  public static final boolean recursively = true;
  public static final boolean to = !recursively;

  public static final FileType file = FileType.FILE;
  public static final FileType folder = FileType.FOLDER;
  public static final FilterType include = FilterType.INCLUDE;
  public static final FilterType exclude = FilterType.EXCLUDE;
  public static final NamePath name = NamePath.NAME;
  public static final NamePath path = NamePath.PATH;

  public static final boolean sensitive = false;
  public static final boolean insensitive = !sensitive;

  public static final boolean sync = false;
  public static final boolean async = !sync;

  private final List<Watcher> watchers = new ArrayList<>();

  public Watcher start(boolean recursively) {
    if (log.isDebugEnabled()) {
      log.debug("start {}to", recursively ? "recursively " : "");
    }
    val w = new Watcher();
    w.setRecursively(recursively);
    w.setStart(true);
    watchers.add(w);
    return w;
  }

  public Watcher stop(boolean to) {
    log.debug("stop to");
    val w = new Watcher();
    w.setStart(false);
    watchers.add(w);
    return w;
  }

  public static Subscription async(Action0 action) {
    return Schedulers.io().createWorker().schedule(action);
  }

  public Command command(String... cmd) {
    return new Command(cmd);
  }

  public static Command createCommand(String... cmd) {
    return new Command(cmd);
  }

  public List<Watcher> getWatchers() {
    return ImmutableList.copyOf(watchers);
  }
}
