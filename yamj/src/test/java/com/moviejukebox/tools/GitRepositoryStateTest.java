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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.moviejukebox.model.Movie;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Stuart
 */
public class GitRepositoryStateTest {

    private static GitRepositoryState instance;

    @BeforeClass
    public static void setUpClass() {
        instance = new GitRepositoryState();
        System.out.println(ToStringBuilder.reflectionToString(instance, ToStringStyle.MULTI_LINE_STYLE));
    }

    /**
     * Test of getBranch method, of class GitRepositoryState.
     */
    @Test
    public void testGetBranch() {
        System.out.println("getBranch");
        String result = instance.getBranch();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDirty method, of class GitRepositoryState.
     */
    @Test
    public void testGetDirty() {
        System.out.println("getDirty");
        Boolean result = instance.getDirty();
        assertNotNull(result);
    }

    /**
     * Test of getTags method, of class GitRepositoryState.
     */
    @Test
    public void testGetTags() {
        System.out.println("getTags");
        List<String> result = instance.getTags();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDescribe method, of class GitRepositoryState.
     */
    @Test
    public void testGetDescribe() {
        System.out.println("getDescribe");
        String result = instance.getDescribe();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getDescribeShort method, of class GitRepositoryState.
     */
    @Test
    public void testGetDescribeShort() {
        System.out.println("getDescribeShort");
        String result = instance.getDescribeShort();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitId method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitId() {
        System.out.println("getCommitId");
        String result = instance.getCommitId();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitIdAbbrev method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitIdAbbrev() {
        System.out.println("getCommitIdAbbrev");
        String result = instance.getCommitIdAbbrev();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildUserName method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildUserName() {
        System.out.println("getBuildUserName");
        String result = instance.getBuildUserName();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildUserEmail method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildUserEmail() {
        System.out.println("getBuildUserEmail");
        String result = instance.getBuildUserEmail();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getBuildTime method, of class GitRepositoryState.
     */
    @Test
    public void testGetBuildTime() {
        System.out.println("getBuildTime");
        String result = instance.getBuildTime();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitUserName method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitUserName() {
        System.out.println("getCommitUserName");
        String result = instance.getCommitUserName();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitUserEmail method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitUserEmail() {
        System.out.println("getCommitUserEmail");
        String result = instance.getCommitUserEmail();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitMessageFull method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitMessageFull() {
        System.out.println("getCommitMessageFull");
        String result = instance.getCommitMessageFull();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitMessageShort method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitMessageShort() {
        System.out.println("getCommitMessageShort");
        String result = instance.getCommitMessageShort();
        assertNotEquals(Movie.UNKNOWN, result);
    }

    /**
     * Test of getCommitTime method, of class GitRepositoryState.
     */
    @Test
    public void testGetCommitTime() {
        System.out.println("getCommitTime");
        String result = instance.getCommitTime();
        assertNotEquals(Movie.UNKNOWN, result);
    }

}
