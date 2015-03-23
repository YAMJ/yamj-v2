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
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
/*
 * @author ilgizar
 */

public class AwardEvent {

    private String event = Movie.UNKNOWN;
    private Collection<Award> awards = new ArrayList<>();

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
