package com.moviejukebox.model;

public interface Identifiable {

    public abstract String getId(String key);

    public abstract void setId(String key, String id);

}