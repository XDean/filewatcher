package org.wenzhe.filewatcher.gui.velocity;

import org.wenzhe.filewatcher.gui.Util;

public enum VelocityMacroLibrary {
	INSTANCE;
	public String deleteBlankLine(String str) {
		return Util.deleteBlankLine(str);
	}

	public String tabAllLines(String str) {
		return Util.tabAllLines(str);
	}

	public String toEscapeString(String str) {
		return Util.toEscapeString(str);
	}

	public String handleCode(String str) {
		return tabAllLines(deleteBlankLine(str));
	}
	
	public String wrapCommand(String str){
	  return Util.wrapCommand(str);
	}
}
