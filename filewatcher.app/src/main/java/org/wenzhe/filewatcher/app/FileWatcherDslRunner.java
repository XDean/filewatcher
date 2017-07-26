package org.wenzhe.filewatcher.app;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;
import lombok.val;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.wenzhe.filewatcher.dsl.FileWatcherDslContext;

import rx.Observable;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class FileWatcherDslRunner {

  @SneakyThrows
  private static FileWatcherDslContext parse(Path dslPath) {
    val context = new FileWatcherDslContext();
    val binding = new Binding();
    binding.setProperty("context", context);
    val configuration = new CompilerConfiguration();
    val dslText = new String(Files.readAllBytes(dslPath), "UTF8");
    val groovyCode = String.format("import java.nio.file.* \n"
        + "import java.io.* \n"
        + "import org.wenzhe.filewatcher.dsl.* \n"
        + "import static org.wenzhe.filewatcher.dsl.Command.PipeType.* \n"
        + "import static org.wenzhe.filewatcher.dsl.FileMode.* \n"
        + "context.with {%s}"
              , dslText);
    val dslScript = new GroovyShell(binding, configuration).parse(groovyCode);
    dslScript.run();
    return context;
  }
  
  @SneakyThrows
  private static Observable<Path> getDslFiles(Path folder, int maxDepth) {
    try (val stream = Files.walk(folder, maxDepth)) {
      return Observable.from(
          stream.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".fw"))
          .toArray(Path[]::new));
    }
  }
  
  public static Observable<FileWatcherDslContext> getDslContexts(Path dsl) {
    return Observable.just(dsl)
    .flatMap(dslPath -> {
      if (Files.isDirectory(dslPath)) {
        return getDslFiles(dslPath, 1);
      } else {
        return Observable.just(dslPath);
      }
    })
    .map(FileWatcherDslRunner::parse)
    ;
  }
}
