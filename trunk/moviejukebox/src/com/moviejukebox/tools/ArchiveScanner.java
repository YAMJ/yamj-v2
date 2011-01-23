package com.moviejukebox.tools;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface ArchiveScanner {

    // creates Files from mutableNames clearing them from the list.
    public Collection<? extends File> getArchiveFiles(File parent, List<String> mutableNames);

}
