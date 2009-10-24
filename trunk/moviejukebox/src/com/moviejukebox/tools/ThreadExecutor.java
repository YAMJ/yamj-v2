/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

package com.moviejukebox.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ThreadExecutor<T> {

    private Collection<Future<T>> values = new ArrayList<Future<T>>();
    private ExecutorService pool;
    private boolean ignoreErrors = true;
    private static Logger logger = Logger.getLogger("moviejukebox");

    /**
     * Helper class
     * Encapsulates a fixed thread pool ExecutorService
     * Saves futures, used just to catch inner exceptions
     * Usage patter:
     * - create with thread count
     * - submit tasks (Callable)
     * - call waitFor; this logs
     * 
     * @author Gabriel Corneanu
     */

    public ThreadExecutor(int threadcount) {
        pool = Executors.newFixedThreadPool(threadcount);
    }

    public void submit(Callable<T> c){
        values.add(pool.submit(c));
    }

    public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    public void waitFor() throws Throwable{
        pool.shutdown();
        for(Future<T> f: values){
            try{
                f.get();
            } catch (ExecutionException e) {
                if(ignoreErrors)
                    logger.fine(getStackTrace(e.getCause()));
                else
                    throw e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        pool.shutdownNow();
    }
}
