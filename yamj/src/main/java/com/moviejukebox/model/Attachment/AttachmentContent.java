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
