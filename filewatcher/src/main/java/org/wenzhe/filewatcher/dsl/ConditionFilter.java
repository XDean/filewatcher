package org.wenzhe.filewatcher.dsl;

import org.wenzhe.filewatcher.FileWatchEvent;

import rx.functions.Func2;

/**
 * @author liuwenzhe2008@gmail.com
 *
 */
public class ConditionFilter extends FilterCondition {

  private final Func2<String, String, Boolean> condition;
  
  public ConditionFilter(Func2<String, String, Boolean> condition) {
    this.condition = condition;
  }
  
  @Override
  protected boolean doFilter(FileWatchEvent evt, Filter filter) {
    return condition.call(evt.getPath().toString(), 
        UpdateType.from(evt).toString().toLowerCase());
  }

  @Override
  protected boolean filter(String nameOrPath) {
    throw new UnsupportedOperationException();
  }

  
}
