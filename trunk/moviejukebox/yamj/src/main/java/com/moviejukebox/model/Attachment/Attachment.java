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

import java.io.File;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * File attachment;
 *
 * @author modmax
 */
@XmlType
public class Attachment {

    private AttachmentType type;
    private int attachmentId;
    private ContentType contentType;
    private String mimeType;
    private File sourceFile;
    
    @XmlElement
    public AttachmentType getType() {
        return type;
    }
    
    public void setType(AttachmentType type) {
        this.type = type;
    }
    
    @XmlElement
    public int getAttachmentId() {
        return attachmentId;
    }
    
    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }
    
    @XmlElement
    public ContentType getContentType() {
        return contentType;
    }
    
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }
    
    @XmlElement
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @XmlTransient
    public File getSourceFile() {
        return sourceFile;
    }
    
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Id=");
        sb.append(this.getAttachmentId());
        sb.append(", Content=");
        sb.append(this.getContentType());
        sb.append(", MimeType=");
        sb.append(this.getMimeType());
        return sb.toString();
    }
}
