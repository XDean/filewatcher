package org.wenzhe.filewatcher.edd.watchservice;

import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.createCommand;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.file;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.include;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.name;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.recursively;
import static org.wenzhe.filewatcher.dsl.FileWatcherDslContext.sync;

import java.io.File;

import lombok.SneakyThrows;

import org.apache.commons.lang3.SystemUtils;
import org.wenzhe.filewatcher.FileWatcherExecutor;

/**
 * @author wen-zhe.liu@asml.com
 *
 */
public class FileWatcher2 {

  private static final String rootDir = "C:\\Users\\weliu\\code\\filewatcher\\filewatcher.app";
  
  public static void main(String[] args) {

    FileWatcherExecutor.getInstance().execute(ctx -> ctx
        
        .start(recursively).watch(rootDir)
        .filter(include).file(name).extension("java", "xml")
        .on(file).modified(f -> FileWatcher2.onModified2(f))
        
    );
    while(true) {
      ;
    }
  }

  @SneakyThrows
  private static void onModified(String filePath) {
    ProcessBuilder pb = new ProcessBuilder("mvn.bat", "clean", "package")
    .inheritIO()
    .directory(new File(rootDir));
    
    char pathSeperator = SystemUtils.IS_OS_UNIX ? ':' : ';';
    
    pb.environment().keySet().stream()
    .filter(key -> key.equalsIgnoreCase("path"))
    .findFirst()
    .ifPresent(key -> pb.environment().computeIfPresent(key, 
        (k, v) -> "C:\\Localdata\\software\\apache-maven-3.1.0\\bin" + pathSeperator + v));
    
    Process p = pb.start();
    int exitCode = p.waitFor();
    System.out.println(exitCode);
  }
  
  @SneakyThrows
  private static void onModified2(String filePath) {
    @SuppressWarnings("unused")
    String sysPath = System.getenv("PATH");
    
    int exitCode = createCommand("mvn.bat", "clean", "package")
    .workingDirectory("C:\\Users\\weliu\\code\\filewatcher\\filewatcher.app")
    .findBinaryIn("C:", "Localdata", "software", "apache-maven-3.1.0", "bin")
    //.environment("PATH", "C:\\Localdata\\software\\apache-maven-3.1.0\\bin;" + sysPath)
    //.redirectOutput(".....")
    //.redirectError(".....")
    .run(sync);
    
    System.out.println(exitCode);
  }

}
