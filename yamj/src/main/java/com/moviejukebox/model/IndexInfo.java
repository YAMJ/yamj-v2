package com.moviejukebox.model;

import java.io.File;

import com.moviejukebox.tools.FileTools;

public class IndexInfo {
    public String categoryName;
    public String key;
    public String baseName;
    public int videosPerPage, videosPerLine, pages;
    public boolean canSkip = true; // skip flags, global (all pages)

    public IndexInfo(String category, String key, int pages, int videosPerPage, int videosPerLine, boolean canSkip) {
        this.categoryName = category;
        this.key = key;
        this.pages = pages;
        this.videosPerPage = videosPerPage;
        this.videosPerLine = videosPerLine;
        this.canSkip = canSkip; // default values
        // "categ_key_"; to be combined with pageid and extension
        baseName = FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, key));
        pages = 0;
    }

    public void checkSkip(int page, String rootPath) {
        String filetest = rootPath + File.separator + baseName + page + ".xml";
        canSkip = canSkip && FileTools.fileCache.fileExists(filetest);
        FileTools.addJukeboxFile(filetest);
        // not nice, but no need to do this again in HTMLWriter
        filetest = rootPath + File.separator + baseName + page + ".html";
        canSkip = canSkip && FileTools.fileCache.fileExists(filetest);
        FileTools.addJukeboxFile(filetest);
    }

    public String toString() {
        StringBuffer indexinfo = new StringBuffer("[IndexInfo ");
        
        indexinfo.append("[categoryName=").append(categoryName).append("]");
        indexinfo.append("[key=").append(key).append("]");
        indexinfo.append("[baseName=").append(baseName).append("]");
        indexinfo.append("[videosPerPage=").append(videosPerPage).append("]");
        indexinfo.append("[videosPerLine=").append(videosPerLine).append("]");
        indexinfo.append("[pages=").append(pages).append("]");
        indexinfo.append("[canSkip=").append(canSkip).append("]");
        indexinfo.append("]");
        
        return indexinfo.toString();
    }
}
