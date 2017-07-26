package org.wenzhe.filewatcher;

import static java.util.stream.Collectors.partitioningBy;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wenzhe.filewatcher.dsl.FileType;
import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;
import org.wenzhe.filewatcher.dsl.Filter;
import org.wenzhe.filewatcher.dsl.FilterType;
import org.wenzhe.filewatcher.dsl.Handler;
import org.wenzhe.filewatcher.dsl.UpdateType;
import org.wenzhe.filewatcher.dsl.Watcher;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class FileWatcherExecutor {

  private static final Logger log = LoggerFactory.getLogger(FileWatcherExecutor.class);

  private static final FileWatcherExecutor INSTANCE = new FileWatcherExecutor();

  public static FileWatcherExecutor getInstance() {
    return INSTANCE;
  }
  
  protected FileWatcherExecutor() {
  }

  public Subscription execute(Action1<FileWatcherDslContext> dslContextInitializer) {
    return run(dslContextInitializer).subscribe();
  }

  public Observable<FileWatchEvent> run(Action1<FileWatcherDslContext> dslContextInitializer) {
    FileWatcherDslContext ctx = new FileWatcherDslContext();
    dslContextInitializer.call(ctx);
    return run(ctx);
  }

  public Observable<FileWatchEvent> run(FileWatcherDslContext ctx) {
    return Observable.from(ctx.getWatchers())
        .filter(watcher -> watcher.isStart())
        .flatMap(this::run);
  }

  public Observable<FileWatchEvent> run(Watcher watcher) {
    Map<Boolean, List<Filter>> groupedFilters = watcher.getFilters().stream()
        .collect(partitioningBy(filter -> filter.getFilterType() == FilterType.INCLUDE));
    List<Filter> includeFilters = groupedFilters.get(true);
    List<Filter> excludeFilters = groupedFilters.get(false);

    Observable<FileWatchEvent> fwe = new FileWatcher(
        watcher.getWatchedFile(), watcher.isRecursively())
        .asObservable()
        .filter(evt -> includeFilters.isEmpty()
            || includeFilters.stream().anyMatch(filter -> filter.filter(evt)))
        .filter(evt -> !excludeFilters.stream().anyMatch(filter -> filter.filter(evt)));

    for (Handler handler : watcher.getHandlers()) {
      fwe = fwe.doOnNext(evt -> {
        if (isFileTypeMatch(evt, handler.getFileTypes())
            && handler.getUpdateType().match(evt)) {
          try {
            handler.getCode().call(evt.getPath().toString(),
                UpdateType.from(evt).toString().toLowerCase());
          } catch (Throwable e) {
            if (log.isTraceEnabled()) {
              log.trace(e.getMessage(), e);
            } else {
              log.error(e.getMessage());
            }
          }
        }
      });
    }
    return fwe;
  }

  // private static boolean matchFilter(FileWatchEvent evt, Filter filter) {
  // return isFileTypeMatch(evt, filter.getFileTypes()) && filter.filter(evt);
  // }

  protected boolean isFileTypeMatch(FileWatchEvent evt, List<FileType> fileTypes) {
    return (evt.isFile() && fileTypes.contains(FileType.FILE))
        || (evt.isDirectory() && fileTypes.contains(FileType.FOLDER))
        || evt.isDeleted();
  }
}
