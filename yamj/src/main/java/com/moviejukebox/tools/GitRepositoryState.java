/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.tools;

import static com.moviejukebox.model.Movie.UNKNOWN;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the Git Repository State information from the maven-git-commit-id plugin
 *
 * @author Stuart
 */
public class GitRepositoryState {

    private static final Logger LOG = LoggerFactory.getLogger(GitRepositoryState.class);

    private String branch = UNKNOWN;                  // =${git.branch}
    private Boolean dirty = null;                     // =${git.dirty}
    private List<String> tags = null;                 // =${git.tags} // comma separated tag names
    private String describe = UNKNOWN;                // =${git.commit.id.describe}
    private String describeShort = UNKNOWN;           // =${git.commit.id.describe-short}
    private String commitId = UNKNOWN;                // =${git.commit.id}
    private String commitIdAbbrev = UNKNOWN;          // =${git.commit.id.abbrev}
    private String buildUserName = UNKNOWN;           // =${git.build.user.name}
    private String buildUserEmail = UNKNOWN;          // =${git.build.user.email}
    private String buildTime = UNKNOWN;               // =${git.build.time}
    private String commitUserName = UNKNOWN;          // =${git.commit.user.name}
    private String commitUserEmail = UNKNOWN;         // =${git.commit.user.email}
    private String commitMessageFull = UNKNOWN;       // =${git.commit.message.full}
    private String commitMessageShort = UNKNOWN;      // =${git.commit.message.short}
    private String commitTime = UNKNOWN;              // =${git.commit.time}

    /**
     * Git Repository State
     *
     */
    public GitRepositoryState() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));

            this.branch = properties.get("git.branch").toString();
            this.dirty = asBoolean(properties.get("git.dirty").toString());
            this.tags = asList(properties.get("git.tags").toString());
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
        } catch (IOException ex) {
            LOG.warn("Failed to get build properties from git.properties file", ex);
        }
    }

    private Boolean asBoolean(final String valueToConvert) {
        if (StringUtils.isNotBlank(valueToConvert)) {
            return Boolean.parseBoolean(StringUtils.trimToEmpty(valueToConvert));
        }
        return null;
    }

    private List<String> asList(final String valueToConvert) {
        if (StringUtils.isNotBlank(valueToConvert)) {
            return new ArrayList<>(Arrays.asList(valueToConvert.split(",")));
        } else {
            return new ArrayList<>();
        }
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
    public Boolean getDirty() {
        return dirty;
    }

    /**
     * Get the build tags (if available)
     *
     * @return
     */
    public List<String> getTags() {
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
