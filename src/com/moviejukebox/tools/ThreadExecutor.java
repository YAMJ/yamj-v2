/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadExecutor<T> implements ThreadFactory{

    private Collection<Future<T>> values = new ArrayList<Future<T>>();
    private ExecutorService pool;
    private int threads_run, threads_io;
    private boolean ignoreErrors = true;
    private static Logger logger = Logger.getLogger("moviejukebox");
    private Semaphore runningThreads, ioThreads;

    private static Map<String, String> hostgrp = new HashMap<String, String>();
    private static Map<String, Semaphore> grouplimits = new HashMap<String, Semaphore>();
    /**
     * Handle IO slots allocation to avoid throttling / ban on source sites Find the proper semaphore for each host:
     *  - Map each unique host to a group
     * (hostgrp) - Max each group (rule) to a semaphore
     * 
     * @author Gabriel Corneanu
     */
    static {
        // First we have to read/create the rules
        // Default, can be overridden
        grouplimits.put(".*", new Semaphore(1));
        String limitsProperty = PropertiesUtil.getProperty("mjb.MaxDownloadSlots", ".*=1");
        logger.finer("WebBrowser: Using download limits: " + limitsProperty);

        Pattern semaphorePattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher semaphoreMatcher = semaphorePattern.matcher(limitsProperty);
        while (semaphoreMatcher.find()) {
            String group = semaphoreMatcher.group(1);
            try {
                Pattern.compile(group);
                logger.finer("WebBrowser: " + group + "=" + semaphoreMatcher.group(2));
                grouplimits.put(group, new Semaphore(Integer.parseInt(semaphoreMatcher.group(2))));
            } catch (Exception error) {
                logger.finer("WebBrowser: Limit rule \"" + group + "\" is not valid regexp, ignored");
            }
        }
    }

    /**
     * Helper class
     * Encapsulates a fixed thread pool ExecutorService
     * Saves futures, used just to catch inner exceptions
     * Usage patter:
     * - create with thread count and io slots
     * - submit tasks (Callable)
     * - call waitFor; this logs
     * 
     * - in addition processing threads should call pairs EnterIO, LeaveIO to switch from running to io state
     * 
     * @author Gabriel Corneanu
     */

    public ThreadExecutor(int threads_run, int threads_io) {
        this.threads_run = threads_run;
        this.threads_io = threads_io <= 0 ? threads_run : threads_io;
        restart();
    }

    static private class ScheduledThread extends Thread {
        private Semaphore s_run, s_io, s_iotarget;
        private int io_cnt = 0;
        private String iohost="none";
        private ScheduledThread(Runnable r, Semaphore s_run, Semaphore s_io) {
            super(r);
            this.s_run = s_run;
            this.s_io = s_io;
        }
        @Override
        public void run() {
            try{
                s_run.acquireUninterruptibly();
                super.run();
            }finally{
                s_run.release();
            }
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        return new ScheduledThread(r, runningThreads, ioThreads);
        
    }

    static public void EnterIO(URL url){
        String host = url.getHost().toLowerCase();
        if (!(Thread.currentThread() instanceof ScheduledThread)) {
            logger.fine("Unmanaged thread call to EnterIO("+host+"); ignored.");
            return;
        }
        ScheduledThread st = (ScheduledThread)Thread.currentThread();
        st.io_cnt ++;
        if(st.io_cnt > 1){
            logger.finer("Nested call to EnterIO("+host+"); previous("+st.iohost+"); ignored");
            return;
        }
        String semaphoreGroup;
        synchronized(hostgrp){
            semaphoreGroup = hostgrp.get(host);
            // first time not found, search for matching group
            if (semaphoreGroup == null) {
                semaphoreGroup = ".*";
                for (String searchGroup : grouplimits.keySet()) {
                    if (host.matches(searchGroup))
                        if (searchGroup.length() > semaphoreGroup.length())
                            semaphoreGroup = searchGroup;
                }
                logger.finer(String.format("IO download host: %s; rule: %s", host, semaphoreGroup));
                hostgrp.put(host, semaphoreGroup);
            }
        }

        // there should be NO way to fail
        logger.finest("Enter IO mode; host="+host);
        Semaphore s = grouplimits.get(semaphoreGroup);
        st.s_iotarget = s;
        st.iohost = host;
        st.s_run.release(); // exit running state; another thread might be released;
        st.s_iotarget.acquireUninterruptibly(); // aquire URL target semaphore
        st.s_io.acquireUninterruptibly(); // enter io state
        //ready to go...
    }

    static public void EnterIO(String url){
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            try {
                u = new URL("http://"+url);
            } catch (MalformedURLException e1) {
                logger.fine("Invalid call to EnterIO.");
                logger.fine(getStackTrace(e1));
                return;
            }
        }
        EnterIO(u);
    }

    static public void LeaveIO(){
        if (!(Thread.currentThread() instanceof ScheduledThread)) {
            logger.fine("Unscheduled thread call to LeaveIO; ignored.");
            //logger.fine(getStackTrace(new Throwable("Unscheduled thread call to LeaveIO; ignored.")));
            return;
        }
        ScheduledThread st = (ScheduledThread)Thread.currentThread();
        if (st.io_cnt <= 0) {
            logger.fine("Warning: unbalanced LeaveIO call; ignored.");
            return;
        }
        st.io_cnt --;
        if (st.io_cnt > 0) {
            logger.finer("Nested LeaveIO call; ignored.");
            return;
        }
        st.s_iotarget.release();
        st.s_io.release();
        st.s_iotarget = null;
        st.s_run.acquireUninterruptibly(); //back to running state
        logger.finest("Left IO mode; host="+st.iohost);
    }

    public void restart(){
        runningThreads = new Semaphore(threads_run);
        ioThreads      = new Semaphore(threads_io);
        pool = Executors.newFixedThreadPool(threads_run + threads_io, this);
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

    public ArrayList<T> waitForValues() throws Throwable{
        pool.shutdown();
        ArrayList<T> v = new ArrayList<T>();
        for(Future<T> f: values){
            try{
                v.add(f.get());
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
        return v;
    }

    public void waitFor() throws Throwable{
        waitForValues();
    }
}
