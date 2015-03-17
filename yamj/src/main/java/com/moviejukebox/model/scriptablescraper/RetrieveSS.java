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
package com.moviejukebox.model.scriptablescraper;

import java.nio.charset.Charset;
import org.apache.commons.lang3.StringUtils;

/**
 * ScriptableScraper class
 *
 * @author ilgizar
 */
public final class RetrieveSS {

    private String url;
    private Charset encoding;
    private String cookies;
    private int retries = 3;
    private int timeout = 3000;

    public RetrieveSS(String url) {
        super();
        setURL(url);
    }

    public RetrieveSS(String url, String encoding, int retries, int timeout, String cookies) {
        super();
        setURL(url);
        setEncoding(encoding);
        setRetries(retries);
        setTimeout(timeout);
        setCookies(cookies);
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        if (StringUtils.isNotBlank(encoding)) {
            this.encoding = Charset.forName(encoding);
        } else {
            this.encoding = null;
        }
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        if (retries >= 0) {
            this.retries = retries;
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        if (timeout >= 0) {
            this.timeout = timeout;
        }
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }
}
