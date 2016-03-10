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
package com.moviejukebox;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTests.class);
    private static final String BASE_TEST_DIR = "src/test/java/xml_test_files/";

    /**
     * Do the initial configuration for the test cases
     *
     */
    protected static final void doConfiguration() {
        TestLogger.configure();
    }

    /**
     * Load a test file
     *
     * @param filename
     * @param subDirectory
     * @return
     */
    protected File getTestFile(String filename, String subDirectory) {
        File file = FileUtils.getFile(BASE_TEST_DIR, subDirectory, filename);
        LOG.info("File: {} Length: {} Exists: {}", file.getAbsolutePath(), file.length(), file.exists());
        return file;
    }

    /**
     * Load a test file
     *
     * @param filename
     * @return
     */
    protected File getTestFile(String filename) {
        return getTestFile(filename, "");
    }

}
