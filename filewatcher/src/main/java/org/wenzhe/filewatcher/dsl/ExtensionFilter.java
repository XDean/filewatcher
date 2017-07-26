package org.wenzhe.filewatcher.dsl;

import java.util.Arrays;

import lombok.Getter;
import lombok.val;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class ExtensionFilter extends FilterCondition {
  
  @Getter private final String[] extensions;

  public ExtensionFilter(String[] extensions) {
    this.extensions = Arrays.stream(extensions)
        .map(ext -> addDotIfAbsent(ext))
        .toArray(String[]::new);
  }

  private String addDotIfAbsent(String ext) {
    return ext.startsWith(".") ? ext : ("." + ext);
  }

  @Override
  public boolean filter(String name) {
    val extName = getExtension(name);
    return Arrays.stream(extensions).anyMatch(extName::equalsIgnoreCase);
  }
  
  private String getExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    return index == -1 ? "" : fileName.substring(index);
  }
}
