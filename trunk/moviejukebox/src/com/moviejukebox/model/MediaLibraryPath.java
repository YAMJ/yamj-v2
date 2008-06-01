package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;

public class MediaLibraryPath {
	String path;
	String nmtRootPath;
	Collection<String> excludes;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getNmtRootPath() {
		return nmtRootPath;
	}

	public void setNmtRootPath(String nmtRootPath) {
		this.nmtRootPath = nmtRootPath;
	}

	public Collection<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(Collection<String> excludes) {
		if (excludes == null) {
			this.excludes = new ArrayList<String>();
		} else {
			this.excludes = excludes;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("[MediaLibraryPath");
		sb.append("[path=").append(path).append("]");
		sb.append("[nmtRootPath=").append(nmtRootPath).append("]");
		for (String excluded : excludes) {
			sb.append("[excludes=").append(excluded).append("]");
		}
		sb.append("]");
		return sb.toString();
	}
}
