package org.wenzhe.filewatcher.dsl;

import java.util.regex.Pattern;

import lombok.Getter;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class MatchFilter extends FilterCondition {
  @Getter private final Pattern pattern;
  
  public MatchFilter(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean filter(String nameOrPath) {
    return pattern.matcher(nameOrPath).matches();
  }
}
