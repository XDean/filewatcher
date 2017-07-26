package org.wenzhe.filewatcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class FileWatcher {
  
  /**
   * fix Java's watch service issue that modify file event will send 2 to 3 times
   */
  private static class DuplicateEventChecker {
    
    private static final int TIME_OUT = 150; // ms
    
    private long lastModifiedTime = 0;
    
    public boolean isDuplicate() {
      long now = System.currentTimeMillis();
      boolean isDuplicated = lastModifiedTime != 0 && (now - lastModifiedTime < TIME_OUT);
      lastModifiedTime = now;
      return isDuplicated;
    }
  }
  
  private final Path path;
  private final boolean recursively;
  
  public FileWatcher(Path path, boolean recursively) {
    this.path = path;
    this.recursively = recursively;
  }

  /**
   * include the root path itself
   */
  private static List<Path> listDirsRecursively(Path path) throws IOException {
    List<Path> dirsToWatch = new ArrayList<>(); 
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        dirsToWatch.add(dir);
        return FileVisitResult.CONTINUE;
      }
    });
    return dirsToWatch;
  }

  @SneakyThrows
  public Observable<FileWatchEvent> asObservable() {
    if (!Files.exists(path)) {
      throw new FileWatcherException("not exist path " + path);
    }
    
    boolean isWatchDir = Files.isDirectory(path);
    Path pathToWatch = isWatchDir ? path : path.getParent();
 
    Observable<Path> obsvPath = isWatchDir && recursively ? 
        Observable.from(listDirsRecursively(pathToWatch)) : Observable.just(pathToWatch);
    
    FileWatchService watchService = new FileWatchService();

    return obsvPath.filter(path -> path != null)
    .reduce(watchService, (watcher, path) -> watcher.register(path))
    .flatMap(watcher -> 
      Observable.<FileWatchEvent>create(subscriber -> watchFile(watcher, subscriber))
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      
      .filter(event -> isWatchDir || path.equals(event.getPath()))
//      .filter(event -> event.exists() || event.isDeleted())//XXX may be false and false
      .doOnNext(event -> updateWatchService(watchService, event))
      .doAfterTerminate(() -> watchService.close())
    );
    
  }

  private void updateWatchService(final FileWatchService watchService, FileWatchEvent event) {
    if (event.isCreated() && event.isDirectory()) {
      watchService.register(event.getPath());
    } else if (event.isDeleted() && !event.exists()) {
      watchService.cancel(event.getPath());
    }
  }

  private void watchFile(FileWatchService watcher, Subscriber<? super FileWatchEvent> subscriber) {
    DuplicateEventChecker checker = new DuplicateEventChecker();
    try {
      while (!subscriber.isUnsubscribed()) {
        WatchKey key = watcher.take();
        for (WatchEvent<?> event : key.pollEvents()) {  
          Path watchablePath = (Path) key.watchable();
          Path path = watchablePath.resolve((Path) event.context());
          if (!checker.isDuplicate()) {
            subscriber.onNext(new FileWatchEvent(path, event.kind()));
          }
        }
        if (key.isValid() && !key.reset()) {  
          break;  
        }
      }
      subscriber.onCompleted();
    } catch (Throwable e) {
      subscriber.onError(e);
    }
  }
}
