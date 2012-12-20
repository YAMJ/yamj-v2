/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.plugin;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class ImdbInfoTest {

    
    public ImdbInfoTest() {
        BasicConfigurator.configure();
    }
    
    @Test
    public void testImdbPersonId() {
        ImdbInfo imdbInfo = new ImdbInfo();
        String id = imdbInfo.getImdbPersonId("Renée Zellweger");
        assertEquals("nm0000250", id);
    }
}
