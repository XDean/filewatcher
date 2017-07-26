package org.wenzhe.filewatcher.dsl;

import java.util.Arrays;

import lombok.Getter;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class ContainFilter extends FilterCondition {
  @Getter private final String[] values;
  private final boolean ignoreCase;
  
  public ContainFilter(String[] values, boolean ignoreCase) {
    this.values = values;
    this.ignoreCase = ignoreCase;
  }

  @Override
  protected boolean filter(String nameOrPath) {
    return Arrays.stream(values).anyMatch(it -> ignoreCase ? 
        nameOrPath.toLowerCase().contains(it.toLowerCase()) : 
        nameOrPath.contains(it));
  }
}
