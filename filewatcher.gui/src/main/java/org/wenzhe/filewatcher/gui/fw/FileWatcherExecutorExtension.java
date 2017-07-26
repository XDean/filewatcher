package org.wenzhe.filewatcher.gui.fw;

import static java.util.stream.Collectors.partitioningBy;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.wenzhe.filewatcher.FileWatchEvent;
import org.wenzhe.filewatcher.FileWatcher;
import org.wenzhe.filewatcher.FileWatcherExecutor;
import org.wenzhe.filewatcher.dsl.Filter;
import org.wenzhe.filewatcher.dsl.FilterType;
import org.wenzhe.filewatcher.dsl.Handler;
import org.wenzhe.filewatcher.dsl.UpdateType;
import org.wenzhe.filewatcher.dsl.Watcher;

import rx.Observable;

@Slf4j
public class FileWatcherExecutorExtension extends FileWatcherExecutor {

  private final FileWatcherDslContextExtension context;

  public FileWatcherExecutorExtension(@NonNull FileWatcherDslContextExtension context) {
    this.context = context;
  }

  public Observable<FileWatchEvent> run() {
    return Observable.from(context.getWatchers())
        .filter(watcher -> watcher.isStart())
        .flatMap(this::run);
  }

  @Override
  public Observable<FileWatchEvent> run(Watcher watcher) {
    Map<Boolean, List<Filter>> groupedFilters = watcher.getFilters().stream()
        .collect(partitioningBy(filter -> filter.getFilterType() == FilterType.INCLUDE));
    List<Filter> includeFilters = groupedFilters.get(true);
    List<Filter> excludeFilters = groupedFilters.get(false);

    Observable<FileWatchEvent> fwe = new FileWatcher(
        watcher.getWatchedFile(), watcher.isRecursively())
        .asObservable()
        .doOnNext(e -> context.lock(watcher))
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
              log.error(e.getMessage(), e);
            }
          }
        }
      });
    }
    fwe.doOnNext(e -> context.unlock());
    return fwe;
  }
}
