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

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.moviejukebox.model.Movie;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stuart
 */
public class GitRepositoryStateTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(GitRepositoryStateTest.class);
    private static GitRepositoryState instance;

    @BeforeClass
    public static void setUpClass() {
        doConfiguration();
        instance = new GitRepositoryState();
        LOG.info(ToStringBuilder.reflectionToString(instance, ToStringStyle.MULTI_LINE_STYLE));
    }

    /**
     * Test of getBranch method, of class GitRepositoryState.
     */
    @Test
    public void testGetBranch() {
        LOG.info("getBranch");
        String result = instance.getBranch();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDirty method, of class GitRepositoryState.
     */
    @Test
    public void testGetDirty() {
        LOG.info("getDirty");
        Boolean result = instance.getDirty();
        assertNotNull(result);
    }

    /**
     * Test of getTags method, of class GitRepositoryState.
     */
    @Test
    public void testGetTags() {
        LOG.info("getTags");
        List<String> result = instance.getTags();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDescribe method, of class GitRepositoryState.
     */
    @Test
    public void testGetDescribe() {
        LOG.info("getDescribe");
        String result = instance.getDescribe();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDescribeShort method, of class GitRepositoryState.
     */
    @Test
    public void testGetDescribeShort() {
        LOG.info("getDescribeShort");
        String result = instance.getDescribeShort();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitId method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitId() {
        LOG.info("getCommitId");
        String result = instance.getCommitId();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitIdAbbrev method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitIdAbbrev() {
        LOG.info("getCommitIdAbbrev");
        String result = instance.getCommitIdAbbrev();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildUserName method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildUserName() {
        LOG.info("getBuildUserName");
        String result = instance.getBuildUserName();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildUserEmail method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildUserEmail() {
        LOG.info("getBuildUserEmail");
        String result = instance.getBuildUserEmail();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildTime method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildTime() {
        LOG.info("getBuildTime");
        String result = instance.getBuildTime();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitUserName method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitUserName() {
        LOG.info("getCommitUserName");
        String result = instance.getCommitUserName();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitUserEmail method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitUserEmail() {
        LOG.info("getCommitUserEmail");
        String result = instance.getCommitUserEmail();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitMessageFull method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitMessageFull() {
        LOG.info("getCommitMessageFull");
        String result = instance.getCommitMessageFull();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitMessageShort method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitMessageShort() {
        LOG.info("getCommitMessageShort");
        String result = instance.getCommitMessageShort();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitTime method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitTime() {
        LOG.info("getCommitTime");
        String result = instance.getCommitTime();
        assertNotEquals(Movie.UNKNOWN, result);
    }

}
