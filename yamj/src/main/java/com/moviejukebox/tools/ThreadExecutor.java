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
package com.moviejukebox.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadExecutor<T> implements ThreadFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutor.class);
    private final Collection<Future<T>> values = new ArrayList<>(100);
    private ThreadPoolExecutor pool = null;
    private BlockingQueue<Runnable> queue = null;
    private final int threadsRun, threadsIo, threadsTotal;
    private final boolean ignoreErrors = true;
    private Semaphore runningThreads, ioThreads;
    private static final Map<String, String> HOST_GROUP = new HashMap<>();
    private static final Map<String, Semaphore> GROUP_LIMITS = new HashMap<>();

    /**
     * Handle IO slots allocation to avoid throttling / ban on source sites
     * <p>
     * Find the proper semaphore for each host:<br/>
     * - Map each unique host to a group (hostgrp)<br/>
     * - Max each group (rule) to a semaphore
     *
     * @author Gabriel Corneanu
     */
    static {
        // First we have to read/create the rules
        // Default, can be overridden
        GROUP_LIMITS.put(".*", new Semaphore(1));
        String limitsProperty = PropertiesUtil.getProperty("mjb.MaxDownloadSlots", ".*=1");
        LOG.debug("Using download limits: {}", limitsProperty);

        Pattern semaphorePattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher semaphoreMatcher = semaphorePattern.matcher(limitsProperty);
        while (semaphoreMatcher.find()) {
            String group = semaphoreMatcher.group(1);
            try {
                Pattern.compile(group);
                LOG.debug("{}={}", group, semaphoreMatcher.group(2));
                GROUP_LIMITS.put(group, new Semaphore(Integer.parseInt(semaphoreMatcher.group(2))));
            } catch (NumberFormatException error) {
                LOG.debug("Rule '{}' is not valid regexp, ignored", group);
            }
        }
    }

    /**
     * Helper class Encapsulates a fixed thread pool ExecutorService Saves
     * futures, used just to catch inner exceptions Usage patter: - create with
     * thread count and io slots - submit tasks (Callable) - call waitFor; this
     * logs
     *
     * - in addition processing threads should call pairs EnterIO, LeaveIO to
     * switch from running to io state
     *
     * @author Gabriel Corneanu
     * @param threadsRun
     * @param threadsIo
     */
    public ThreadExecutor(int threadsRun, int threadsIo) {
        this.threadsRun = threadsRun;
        this.threadsIo = threadsIo <= 0 ? threadsRun : threadsIo;
        threadsTotal = this.threadsRun + this.threadsIo;
        restart();
    }

    /*
     * Thread descendant class used for our execution scheduling
     */
    private static final class ScheduledThread extends Thread {

        private final Semaphore sRun, sIo;
        private Semaphore sIotarget;
        private final Stack<String> hosts = new Stack<>();

        private ScheduledThread(Runnable r, Semaphore sRun, Semaphore sIo) {
            super(r);
            this.sRun = sRun;
            this.sIo = sIo;
        }

        @Override
        public void run() {
            sRun.acquireUninterruptibly();
            try {
                super.run();
            } finally {
                sRun.release();
            }
        }

        private void enterIO(URL url) {
            String host = url.getHost().toLowerCase();

            if (!hosts.empty()) {
                //going to the same host is ok
                if (!host.equals(hosts.peek())) {
                    LOG.debug("ThreadExecutor: Nested EnterIO({}); previous({}); ignored", host, hosts.peek());
                }
                hosts.push(host);
                return;
            }
            String semaphoreGroup;
            synchronized (HOST_GROUP) {
                semaphoreGroup = HOST_GROUP.get(host);
                // first time not found, search for matching group
                if (semaphoreGroup == null) {
                    semaphoreGroup = ".*";
                    for (String searchGroup : GROUP_LIMITS.keySet()) {
                        if (host.matches(searchGroup)) {
                            if (searchGroup.length() > semaphoreGroup.length()) {
                                semaphoreGroup = searchGroup;
                            }
                        }
                    }
                    LOG.debug("IO download host: {}; rule: {}", host, semaphoreGroup);
                    HOST_GROUP.put(host, semaphoreGroup);
                }
            }

            // there should be NO way to fail
            //String dbgstr = "host="+host+"; thread="+getName();
            //logger.finest("ThreadExecutor: Try EnterIO: "+dbgstr);
            Semaphore s = GROUP_LIMITS.get(semaphoreGroup);
            sIotarget = s;
            sRun.release(); // exit running state; another thread might be released;
            sIotarget.acquireUninterruptibly(); // aquire URL target semaphore
            hosts.push(host);
            sIo.acquireUninterruptibly(); // enter io state
            //logger.finest("ThreadExecutor: EnterIO done: "+dbgstr);
            //ready to go...
        }

        private void leaveIO() {
            if (hosts.empty()) {
                LOG.info(SystemTools.getStackTrace(new Throwable("ThreadExecutor: Unbalanced LeaveIO call.")));
                return;
            }
            String host = hosts.pop();
            if (!hosts.empty()) {
                if (!host.equals(hosts.peek())) {
                    LOG.debug("Nested LeaveIO({}); previous({}); ignored", host, hosts.peek());
                }
                return;
            }

            //String dbgstr = "host="+host+"; thread="+getName();
            sIotarget.release();
            sIo.release();
            sIotarget = null;
            //logger.finest("ThreadExecutor: Try LeaveIO: "+dbgstr);
            sRun.acquireUninterruptibly(); //back to running state
            //logger.finest("ThreadExecutor: LeaveIO done: "+dbgstr);
        }
    }

    public static void enterIO(URL url) {
        if (!(Thread.currentThread() instanceof ScheduledThread)) {
            // logger.info(getStackTrace(new Throwable("ThreadExecutor: Unmanaged thread call to EnterIO; ignored.")));
            // If this isn't a managed thread, then just exit.
            return;
        }
        ((ScheduledThread) Thread.currentThread()).enterIO(url);
    }

    public static void enterIO(String url) {
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            try {
                u = new URL("http://" + url);
            } catch (MalformedURLException e1) {
                LOG.info("ThreadExecutor: Invalid call to EnterIO.");
                LOG.info(SystemTools.getStackTrace(e1));
                return;
            }
        }
        enterIO(u);
    }

    public static void leaveIO() {
        if (!(Thread.currentThread() instanceof ScheduledThread)) {
            //logger.info(getStackTrace(new Throwable("ThreadExecutor: Unmanaged thread call to LeaveIO; ignored.")));
            // If this isn't a managed thread, then just exit.
            return;
        }
        ((ScheduledThread) Thread.currentThread()).leaveIO();
    }

    @Override
    public Thread newThread(Runnable r) {
        return new ScheduledThread(r, runningThreads, ioThreads);
    }

    public final void restart() {
        values.clear();
        runningThreads = new Semaphore(threadsRun);
        ioThreads = new Semaphore(threadsIo);

        //refined: use a fixed queue with some extra space; in relation with submit
        //the size is just an approximation; it has no real connection to thread count
        //make it reasonable sized to avoid waiting in submit
        queue = new ArrayBlockingQueue<>(100);
//        queue = new LinkedBlockingQueue<Runnable>();
        //allow more threads, they are managed by semaphores
        pool = new ThreadPoolExecutor(threadsRun, 2 * threadsTotal,
                100, TimeUnit.MILLISECONDS,
                queue,
                this);
    }

    public void submit(Callable<T> c) throws InterruptedException {
        //never queue too many objects; wait for some space to limit resource allocations
        //in case of fixed size queues, tasks could even be rejected
        //therefore wait here a very short time
        while (queue.remainingCapacity() <= 0) {
            Thread.sleep(5);
        }
        values.add(pool.submit(c));
    }

    public void submit(Runnable r) throws InterruptedException {
        T result = null;
        submit(Executors.callable(r, result));
    }

    public List<T> waitForValues() throws Throwable {
        pool.shutdown();
        List<T> v = new ArrayList<>(values.size());
        for (Future<T> f : values) {
            try {
                v.add(f.get());
            } catch (ExecutionException ex) {
                if (ignoreErrors) {
                    LOG.info(SystemTools.getStackTrace(ex));
                } else {
                    throw ex.getCause();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        pool.shutdownNow();
        return v;
    }

    public void waitFor() throws Throwable {
        waitForValues();
        int dif = threadsIo - ioThreads.availablePermits();
        if (dif != 0) {
            LOG.error("ThreadExecutor: Unfinished downloading threads detected: {}", dif);
        }
    }
}
