/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebBrowserTest {

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testWithoutProxy() throws Exception {
        PropertiesUtil.setProperty("mjb.ProxyHost", "");
        PropertiesUtil.setProperty("mjb.ProxyPort", "");

        String url = "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora";
        WebBrowser browser = new WebBrowser();
        String result = browser.request(url);
        System.err.println(result);
    }

    @Test
    public void testWithProxy() throws Exception {
        PropertiesUtil.setProperty("mjb.ProxyHost", "proxy.example.com");
        PropertiesUtil.setProperty("mjb.ProxyPort", "3128");

        String url = "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora";
        WebBrowser browser = new WebBrowser();
        String result = browser.request(url);
        System.err.println(result);
    }
}
