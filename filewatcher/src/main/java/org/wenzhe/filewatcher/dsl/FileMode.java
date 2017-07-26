package org.wenzhe.filewatcher.dsl;

/**
 * @author wen-zhe.liu@asml.com
 *
 */
public enum FileMode {
  readOnly, writeOnly, canReadAndWrite, cannotReadOrWrite;

  public static FileMode from(boolean readable, boolean writable) {
    if (readable && !writable) {
      return readOnly;
    } else if (!readable && writable) {
      return writeOnly;
    } else if (readable && writable) {
      return canReadAndWrite;
    } else {
      return cannotReadOrWrite;
    }
  }
}
