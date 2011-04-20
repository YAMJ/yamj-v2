/*
 *      Copyright (c) 2004-2011 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */
package com.moviejukebox.scanner;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.moviejukebox.tools.ArchiveScanner;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.exception.RarException.RarExceptionType;
import de.innosystec.unrar.rarfile.FileHeader;
import de.innosystec.unrar.rarfile.MainHeader;

public class RARArchiveScanner implements ArchiveScanner {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private static final Pattern partPattern=Pattern.compile("(.*?\\.part)(\\d+)(\\.rar)$", Pattern.CASE_INSENSITIVE);

    boolean useRARLastModified=false;

    public void setUseRARLastModified(boolean useRARLastModified) {
        this.useRARLastModified = useRARLastModified;
    }

    @Override
    public Collection<? extends File> getArchiveFiles(File parent, List<String> mutableNames) {
        List<File> archiveFiles = new ArrayList<File>();

        Set<String> remainingNames=new HashSet<String>(mutableNames);

        // handle all .rar, .000 and .001 files and decide if they are head of multipart archive or standalone archives
        mutableNameLoop:
        for(String name: mutableNames) {
            if(!remainingNames.contains(name)) {
                // process only yet-unprocessed filenames
                continue;
            }
            if(name!=null && name.length() >= 4) {
                if(".rar".equalsIgnoreCase(new String(name.substring(name.length() - 4))) || name.endsWith(".000") || name.endsWith(".001")) {
                    Archive archive=null;
                    try {
                        logger.debug("Processing "+name+"...");
                        archive=new Archive(new File(parent,name));
                        MainHeader mh=archive.getMainHeader();
                        if(mh.isEncrypted()) {
                            logger.warn("Encrypted archive, skipping...");
                            continue;
                        }

                        if(mh.isMultiVolume() && !mh.isFirstVolume()) {
                            // for older RARs, they never have firstVolume=true.
                            // Assume there are no splitBefores in the first volume. (and vice versa)
                            for(FileHeader fh: archive.getFileHeaders()) {
                                if(fh.isSplitBefore()) {
                                    continue mutableNameLoop;
                                }
                            }
                        }

                        // parse the prefix, suffix, number, and number padding width of the rar
                        String namePrefix="";
                        String nameSuffix="";
                        int nextVolume=-1;
                        String volumeFormat=null;
                        if(name.endsWith(".000")) {
                            namePrefix=new String(name.substring(0, name.length()-3));
                            nextVolume=1;
                            volumeFormat="%s%03d";
                        } else if(name.endsWith(".001")) {
                            namePrefix=new String(name.substring(0, name.length()-3));
                            nextVolume=2;
                            volumeFormat="%s%03d";
                        } else {
                            Matcher matcher=partPattern.matcher(name);
                            if(matcher.matches()) {
                                namePrefix=matcher.group(1);
                                nameSuffix=matcher.group(3);
                                try {
                                    nextVolume=Integer.parseInt(matcher.group(2))+1;
                                } catch(NumberFormatException e) {
                                    logger.warn("RAR part regex matches but is not an integer");
                                    continue;
                                }
                                volumeFormat="%s%0"+matcher.group(2).length()+"d%s";
                            } else {
                                namePrefix=new String(name.substring(0, name.length()-2));
                                nextVolume=0;
                                volumeFormat="%s%02d";
                            }
                        }
                        long rarLastModified=new File(parent,name).lastModified();
                        VirtualFile virtualRARDirectory = VirtualFile.createRootVirtualFile(parent,name,0,rarLastModified);
                        archiveFiles.add(virtualRARDirectory);

                        // repeat until there are no more volumes remaining for this RAR
                        while(true) {
                            boolean isSplitAfter=false;
                            for(FileHeader fh: archive.getFileHeaders()) {
                                String fileName=getFileName(fh);
                                if(logger.isDebugEnabled()) {
                                    if(!fh.isSplitBefore())
                                        logger.debug("\t\t"+fileName);
                                    else
                                        logger.debug("...\t\t"+fileName);
                                }
                                isSplitAfter=fh.isSplitAfter();
                                // ignore latter parts of a files, already handled as the first part
                                if(fh.isSplitBefore()) {
                                    continue;
                                }
                                if(fh.isEncrypted()) {
                                    logger.warn("Encrypted file, skipping...");
                                    continue;
                                }
                                if(useRARLastModified) {
                                    VirtualFile.createVirtualFile(virtualRARDirectory,fileName,fh.getFullUnpackSize(),rarLastModified,fh.isDirectory());
                                } else {
                                    VirtualFile.createVirtualFile(virtualRARDirectory,fileName,fh.getFullUnpackSize(),fh.getMTime().getTime(),fh.isDirectory());
                                }
                            }
                            remainingNames.remove(name);
                            // stop processing after there is no more continuations to process
                            // FIXME: ??? in the unlikely case of a file ending at the rar part boundary,
                            // should we check we don't have properly named next file? Or is isSplitAfter true even in those cases?
                            if(!isSplitAfter) {
                                break;
                            }
                            archive.close();
                            mh=null;
                            name=String.format(volumeFormat,namePrefix,nextVolume++,nameSuffix);
                            logger.debug("Processing "+name+"...");
                            archive=new Archive(new File(parent,name));
                        }
                    } catch (RarException e) {
                        // .000 and .001 can be other files than RARs
                        if (!((name.endsWith(".000") || name.endsWith(".001")) && e.getType() == RarExceptionType.notRarArchive)) {
                            logger.warn("Could not process RAR \""+new File(parent,name).getPath()+"\", reason: "+e.getType());
                        }
                        // feature: if the failed file is .rar (and the next file would be .r00), the .r## files will be left in the directory.
                    } catch (IOException e) {
                        logger.warn("Could not process RAR \""+new File(parent,name).getPath()+"\", reason: "+e.getMessage());
                    } finally {
                        if(archive!=null) {
                            try {
                                archive.close();
                            } catch (IOException e) {
                                logger.info("Could not close archive: "+e.getMessage());
                                // ignore
                            }
                        }
                    }
                }
            }
        }

        // leave only the unhandled filenames; we need to use the same List instance
        mutableNames.clear();
        mutableNames.addAll(remainingNames);

        return archiveFiles;
    }

    private String getFileName(FileHeader fh) {
        String rawName=null;
        if(fh.isUnicode()) {
            rawName=fh.getFileNameW();
        } else {
            rawName=fh.getFileNameString();
        }
        String[] pathElements=rawName.split("\\\\");
        StringBuilder sb=new StringBuilder(rawName.length());
        for(String pathElement: pathElements) {
            sb.append(pathElement);
            sb.append(File.separatorChar);
        }
        // remove the trailing File.separator
        sb.setLength(sb.length()-1);
        return sb.toString();
    }

    // I'm sure there are plenty of bugs and corner cases, but this
    // 1) works for now and 2) is less effort than implementing a full VFS throughout MJB
    @SuppressWarnings("serial")
    public static class VirtualFile extends File {

        private static final String separatorRegex=Pattern.quote(File.separator);

        private File parent;
        private long length;
        private long lastModified;
        private boolean directory;
        private List<File> children=new ArrayList<File>();

        // create a root of VFS named child in a directory called parent. Child may not contain path separator
        public static VirtualFile createRootVirtualFile(File parent, String child, long length, long lastModified) {
            return new VirtualFile(parent, child, length, lastModified, true);
        }

        // child may contain path separator
        public static VirtualFile createVirtualFile(VirtualFile parent, String canonicalChild, long length, long lastModified) {
            return createVirtualFile(parent, canonicalChild, length, lastModified, false);
        }

        // child may contain path separator
        public static VirtualFile createVirtualFile(VirtualFile parent, String canonicalChild, long length, long lastModified, boolean directory) {
            String[] names = canonicalChild.split(separatorRegex);
            VirtualFile childFile = null;
            for(int i = 0; i < names.length; i++) {
                childFile=parent.getNamedChild(names[i]);
                if(childFile == null) {
                    if(i + 1 >= names.length) {
                        // last element
                        childFile = new VirtualFile(parent, names[i], length, lastModified, directory);
                    } else {
                        // in-between element, make it a directory
                        childFile = new VirtualFile(parent, names[i], 0, lastModified, true);
                    }
                } else if(i + 1 >= names.length && directory != childFile.isDirectory()) {
                    // the file exists already; sanity check
                    throw new IllegalArgumentException("Tried to create a virtual file/directory with the same name as an existing directory/file");
                }
                parent = childFile;
            }
            return childFile;
        }

        private VirtualFile(File parent, String child, long length, long lastModified, boolean directory) {
            super(parent, child);
            this.parent = parent;
            if(directory) {
                this.length = 0;
            } else {
                this.length = length;
            }
            this.lastModified = lastModified;
            this.directory = directory;

            // register as a child if the parent is an instance of VirtualFile
            // if the parent is not a VirtualFile (ie. regular java.io.File) the code constructing this object
            // is responsible of making this Object visible in the list*() methods or other ways of
            // accessing the virtual part of the filesystem. If only j.i.File was a proper pluggable VFS...
            if(parent instanceof VirtualFile) {
                ((VirtualFile)parent).addChild(this);
            }
        }

        private void addChild(VirtualFile child) {
            children.add(child);
        }

        private VirtualFile getNamedChild(String child) {
            if(child == null) {
                return null;
            }

            // linear search is fast enough for most real-life archives.
            for(File f: children) {
                if(f instanceof VirtualFile && child.equals(f.getName())) {
                    return (VirtualFile)f;
                }
            }

            return null;
        }

        @Override
        public boolean canExecute() {
            return directory;
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return false;
        }

        @Override
        public int compareTo(File pathname) {
            return super.compareTo(pathname);
        }

        @Override
        public boolean createNewFile() throws IOException {
            return false;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public void deleteOnExit() {
            ;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public boolean exists() {
            return true;
        }

        // possible bug: this leaks non-virtual files. But it's not used anywhere at the moment
        @Override
        public File getAbsoluteFile() {
            logger.error("possible bug, calling RARArchiveScanner.VirtualFile.getAbsoluteFile()");
            return super.getAbsoluteFile();
        }

        @Override
        public String getAbsolutePath() {
            return super.getAbsolutePath();
        }

        // possible bug: this leaks non-virtual files. But it's not used anywhere at the moment
        @Override
        public File getCanonicalFile() throws IOException {
            logger.error("possible bug, calling RARArchiveScanner.VirtualFile.getCanonicalFile()");
            return super.getCanonicalFile();
        }

        @Override
        public String getCanonicalPath() throws IOException {
            return super.getCanonicalPath();
        }

        @Override
        public long getFreeSpace() {
            return super.getFreeSpace();
        }

        @Override
        public String getName() {
            return super.getName();
        }

        @Override
        public String getParent() {
            return parent == null ? null : parent.getPath();
        }

        @Override
        public File getParentFile() {
            return parent;
        }

        @Override
        public String getPath() {
            return super.getPath();
        }

        @Override
        public long getTotalSpace() {
            return super.getTotalSpace();
        }

        @Override
        public long getUsableSpace() {
            return super.getUsableSpace();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean isAbsolute() {
            return super.isAbsolute();
        }

        @Override
        public boolean isDirectory() {
            return directory;
        }

        @Override
        public boolean isFile() {
            return !directory;
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public long lastModified() {
            return lastModified;
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public String[] list() {
            if(!directory) {
                return null;
            }
            String[] sa = new String[children.size()];
            int i = 0;
            for(File f: children) {
                sa[i++] = f.getName();
            }
            return sa;
        }

        @Override
        public String[] list(FilenameFilter filter) {
            if(!directory) {
                return null;
            }
            if(filter == null) {
                return list();
            }
            List<String> accepted = new ArrayList<String>();
            for(File f: children) {
                if(filter.accept(this, f.getName()))
                    accepted.add(f.getName());
            }
            return accepted.toArray(new String[accepted.size()]);
        }

        @Override
        public File[] listFiles() {
            if(!directory) {
                return null;
            }
            return children.toArray(new File[children.size()]);
        }

        @Override
        public File[] listFiles(FileFilter filter) {
            if(!directory) {
                return null;
            }
            if(filter == null) {
                return children.toArray(new File[children.size()]);
            }
            List<File> accepted = new ArrayList<File>();
            for(File f: children) {
                if(filter.accept(f))
                    accepted.add(f);
            }
            return accepted.toArray(new File[accepted.size()]);
        }

        @Override
        public File[] listFiles(FilenameFilter filter) {
            if(!directory) {
                return null;
            }
            if(filter == null) {
                return children.toArray(new File[children.size()]);
            }
            List<File> accepted = new ArrayList<File>();
            for(File f: children) {
                if(filter.accept(this, f.getName()))
                    accepted.add(f);
            }
            return accepted.toArray(new File[accepted.size()]);
        }

        @Override
        public boolean mkdir() {
            return false;
        }

        @Override
        public boolean mkdirs() {
            return false;
        }

        @Override
        public boolean renameTo(File dest) {
            return false;
        }

        @Override
        public boolean setExecutable(boolean executable, boolean ownerOnly) {
            return false;
        }

        @Override
        public boolean setExecutable(boolean executable) {
            return false;
        }

        @Override
        public boolean setLastModified(long lastModified) {
            this.lastModified=lastModified;
            return true;
        }

        @Override
        public boolean setReadOnly() {
            return true;
        }

        @Override
        public boolean setReadable(boolean readable, boolean ownerOnly) {
            return false;
        }

        @Override
        public boolean setReadable(boolean readable) {
            return false;
        }

        @Override
        public boolean setWritable(boolean writable, boolean ownerOnly) {
            return false;
        }

        @Override
        public boolean setWritable(boolean writable) {
            return false;
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        public URI toURI() {
            return super.toURI();
        }

        @Override
        @SuppressWarnings("deprecation")
        public URL toURL() throws MalformedURLException {
            return super.toURL();
        }

    }

}
