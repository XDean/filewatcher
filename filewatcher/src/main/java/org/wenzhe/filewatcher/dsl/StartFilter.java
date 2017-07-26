package org.wenzhe.filewatcher.dsl;

import java.util.Arrays;

import lombok.Getter;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class StartFilter extends FilterCondition {
  @Getter private final String[] values;
  private final boolean ignoreCase;
  
  public StartFilter(String[] values, boolean ignoreCase) {
    this.values = values;
    this.ignoreCase = ignoreCase;
  }

  @Override
  public boolean filter(String nameOrPath) {
    return Arrays.stream(values).anyMatch(it ->
      ignoreCase ? nameOrPath.toLowerCase().startsWith(it.toLowerCase())
      : nameOrPath.startsWith(it.toLowerCase()));
  }
}
