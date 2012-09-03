/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.tools.downloader;

/**
 * This is the interface for the RBCWrapper class
 *
 * It will get the progress as a percentage, if known, otherwise it will return
 * -1.0 to indicate indeterminate progress.
 *
 * Taken from http://stackoverflow.com/a/11068356/443283
 *
 * @author stuart.boston
 */
public interface RBCWrapperDelegate {

    public void rbcProgressCallback(RBCWrapper rbc, double progress);
}
