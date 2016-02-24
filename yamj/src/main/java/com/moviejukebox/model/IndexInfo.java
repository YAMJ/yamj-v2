/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.model;

import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class IndexInfo {

    private static final boolean SKIP_HTML_GENERATION = PropertiesUtil.getBooleanProperty("mjb.skipHtmlGeneration", Boolean.TRUE);
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
        if (!SKIP_HTML_GENERATION) {
            // not nice, but no need to do this again in HTMLWriter
            filetest = new StringBuilder(rootPath);
            filetest.append(File.separator).append(baseName).append(page).append(EXT_HTML);
            canSkip = canSkip && FileTools.fileCache.fileExists(filetest.toString());
            FileTools.addJukeboxFile(filetest.toString());
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
