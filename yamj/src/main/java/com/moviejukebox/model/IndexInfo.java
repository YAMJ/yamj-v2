package com.moviejukebox.model;

import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;

public class IndexInfo {

//    private static Logger logger = Logger.getLogger(IndexInfo.class);
    private static boolean skipHtmlGeneration = PropertiesUtil.getBooleanProperty("mjb.skipHtmlGeneration", "true");
    private static final String EXT_XML = ".xml";
    private static final String EXT_HTML = ".html";
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
    }

    public void checkSkip(int page, String rootPath) {
        StringBuilder filetest = new StringBuilder(rootPath);
        filetest.append(File.separator).append(baseName).append(page).append(EXT_XML);

        canSkip = canSkip && FileTools.fileCache.fileExists(filetest.toString());
        FileTools.addJukeboxFile(filetest.toString());

        // Don't check if we aren't using HTML
        if (!skipHtmlGeneration) {
            // not nice, but no need to do this again in HTMLWriter
            filetest = new StringBuilder(rootPath);
            filetest.append(File.separator).append(baseName).append(page).append(EXT_HTML);
            canSkip = canSkip && FileTools.fileCache.fileExists(filetest.toString());
            FileTools.addJukeboxFile(filetest.toString());
        }
    }

    @Override
    public String toString() {
        StringBuilder indexinfo = new StringBuilder("[IndexInfo ");

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
