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

import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebBrowserTest {

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
    }

    @Test
    public void testWithoutProxy() throws Exception {
        PropertiesUtil.setProperty("mjb.ProxyHost", "");
        PropertiesUtil.setProperty("mjb.ProxyPort", "");

        String url = "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora";
        WebBrowser browser = new WebBrowser();
        String result = browser.request(url);
        assertNotNull("No response", result);
        assertTrue("No text in response", StringUtils.isNotBlank(result));
    }

    @Test
    public void testWithProxy() throws Exception {
        PropertiesUtil.setProperty("mjb.ProxyHost", "proxy.example.com");
        PropertiesUtil.setProperty("mjb.ProxyPort", "3128");

        String url = "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora";
        WebBrowser browser = new WebBrowser();
        String result = browser.request(url);
        assertNotNull("No response", result);
        assertTrue("No text in response", StringUtils.isNotBlank(result));
    }
}
