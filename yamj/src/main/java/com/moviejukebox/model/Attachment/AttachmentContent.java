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
package com.moviejukebox.model.Attachment;

/**
 * A volatile container for content information of an attachment.
 *
 * @author modmax
 */
public class AttachmentContent {

    private final ContentType contentType;
    private final int part;

    public AttachmentContent(ContentType contentType) {
        this(contentType, -1);
    }

    public AttachmentContent(ContentType contentType, int part) {
        this.contentType = contentType;
        this.part = part;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public int getPart() {
        return part;
    }
}
