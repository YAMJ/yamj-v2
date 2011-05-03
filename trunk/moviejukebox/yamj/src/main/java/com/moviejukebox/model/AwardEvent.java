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
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;
/*
 * @author ilgizar
 */
public class AwardEvent {
    private String event = Movie.UNKNOWN;
    private Collection<Award> awards = new ArrayList<Award>();

    public String getName() {
        return event;
    }

    public void setName(String event) {
        this.event = event;
    }

    public Collection<Award> getAwards() {
        return this.awards;
    }

    public void setAwards(Collection<Award> awards) {
        if (awards != null) {
            this.awards = awards;
        }
    }

    public void addAward(Award award) {
        if (award != null) {
            this.awards.add(award);
        }
    }
}
