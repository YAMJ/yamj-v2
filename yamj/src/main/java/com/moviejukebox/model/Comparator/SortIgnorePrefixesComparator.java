/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.model.Comparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;

import com.moviejukebox.tools.PropertiesUtil;

public class SortIgnorePrefixesComparator implements Comparator<Object> {

    private final ArrayList<String> sortIgnorePrefixes = new ArrayList<String>();
    private boolean inited = false;

    @Override
    public int compare(Object o1, Object o2) {
        if (!inited) {
            initSortIgnorePrefixes();
        }

        return getStrippedTitle((String) o1).compareToIgnoreCase(getStrippedTitle((String) o2));
    }

    private String getStrippedTitle(String title) {
        String lowerTitle = title.toLowerCase();

        for (String prefix : sortIgnorePrefixes) {
            if (lowerTitle.startsWith(prefix.toLowerCase())) {
                title = new String(title.substring(prefix.length()));
                break;
            }
        }

        return title;
    }

    public void initSortIgnorePrefixes() {
        String temp = PropertiesUtil.getProperty("sorting.strip.prefixes", null);
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = new String(token.substring(1, token.length() - 1));
                }
                sortIgnorePrefixes.add(token.toLowerCase());
            }
        }
        inited = true;
    }

}
