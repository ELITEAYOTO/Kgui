/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

import java.util.ArrayList;
import java.util.List;

public class KitItemBookData {
    private List<String> pages;
    private String author;
    private String generation;
    private String title;

    public KitItemBookData(List<String> pages, String author, String generation, String title) {
        this.pages = pages;
        this.author = author;
        this.generation = generation;
        this.title = title;
    }

    public List<String> getPages() {
        return this.pages;
    }

    public void setPages(List<String> pages) {
        this.pages = pages;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGeneration() {
        return this.generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public KitItemBookData clone() {
        return new KitItemBookData(new ArrayList<String>(this.pages), this.author, this.generation, this.title);
    }
}

