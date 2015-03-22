/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.tools;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Get the Git Repository State information from the maven-git-commit-id plugin
 *
 * @author Stuart
 */
public class GitRepositoryState {

    private final String branch;                  // =${git.branch}
    private final String dirty;                   // =${git.dirty}
    private final String tags;                    // =${git.tags} // comma separated tag names
    private final String describe;                // =${git.commit.id.describe}
    private final String describeShort;           // =${git.commit.id.describe-short}
    private final String commitId;                // =${git.commit.id}
    private final String commitIdAbbrev;          // =${git.commit.id.abbrev}
    private final String buildUserName;           // =${git.build.user.name}
    private final String buildUserEmail;          // =${git.build.user.email}
    private final String buildTime;               // =${git.build.time}
    private final String commitUserName;          // =${git.commit.user.name}
    private final String commitUserEmail;         // =${git.commit.user.email}
    private final String commitMessageFull;       // =${git.commit.message.full}
    private final String commitMessageShort;      // =${git.commit.message.short}
    private final String commitTime;              // =${git.commit.time}

    /**
     * Git Repository State
     *
     * @throws IOException
     */
    public GitRepositoryState() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));

        this.branch = properties.get("git.branch").toString();
        this.dirty = properties.get("git.dirty").toString();
        this.tags = properties.get("git.tags").toString();
        this.describe = properties.get("git.commit.id.describe").toString();
        this.describeShort = properties.get("git.commit.id.describe-short").toString();
        this.commitId = properties.get("git.commit.id").toString();
        this.commitIdAbbrev = properties.get("git.commit.id.abbrev").toString();
        this.buildUserName = properties.get("git.build.user.name").toString();
        this.buildUserEmail = properties.get("git.build.user.email").toString();
        this.buildTime = properties.get("git.build.time").toString();
        this.commitUserName = properties.get("git.commit.user.name").toString();
        this.commitUserEmail = properties.get("git.commit.user.email").toString();
        this.commitMessageShort = properties.get("git.commit.message.short").toString();
        this.commitMessageFull = properties.get("git.commit.message.full").toString();
        this.commitTime = properties.get("git.commit.time").toString();
    }

    /**
     * The branch of the build
     *
     * @return
     */
    public String getBranch() {
        return branch;
    }

    /**
     * If the build is dirty (built from an un-committed branch)
     *
     * @return
     */
    public String getDirty() {
        return dirty;
    }

    /**
     * Get the build tags (if available)
     *
     * @return
     */
    public String getTags() {
        return tags;
    }

    /**
     * Summary of the commit in the branch history
     *
     * @return
     */
    public String getDescribe() {
        return describe;
    }

    /**
     * Get the short description of the commit
     *
     * @return
     */
    public String getDescribeShort() {
        return describeShort;
    }

    /**
     * Get the commit SHA
     *
     * @return
     */
    public String getCommitId() {
        return commitId;
    }

    /**
     * get the short commit SHA
     *
     * @return
     */
    public String getCommitIdAbbrev() {
        return commitIdAbbrev;
    }

    /**
     * Get the Commit User Name
     *
     * @return
     */
    public String getBuildUserName() {
        return buildUserName;
    }

    /**
     * Get the Commit User Email
     *
     * @return
     */
    public String getBuildUserEmail() {
        return buildUserEmail;
    }

    /**
     * Get the time the build was made
     *
     * @return
     */
    public String getBuildTime() {
        return buildTime;
    }

    /**
     * Get the Username of the commit
     *
     * @return
     */
    public String getCommitUserName() {
        return commitUserName;
    }

    /**
     * Get the Email of the committer
     *
     * @return
     */
    public String getCommitUserEmail() {
        return commitUserEmail;
    }

    /**
     * Get the full commit message
     *
     * @return
     */
    public String getCommitMessageFull() {
        return commitMessageFull;
    }

    /**
     * Get the short commit message
     *
     * @return
     */
    public String getCommitMessageShort() {
        return commitMessageShort;
    }

    /**
     * Get the time of the commit
     *
     * @return
     */
    public String getCommitTime() {
        return commitTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
