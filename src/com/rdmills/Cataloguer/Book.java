package com.rdmills.Cataloguer;

import org.json.JSONArray;

/**
 * Created by Randy on 16/04/2015.
 */
public class Book {
    private int catalogueId;
    private String title;
    private String subtitle;
    private JSONArray authors;
    private String thumbnail;
    private String isbn;
    private String publisher;
    private String publishedDate;
    private int pageCount;

    public Book(String title, String subtitle, JSONArray authors,
                String thumbnail, String isbn, String publisher,
                String publishedDate, int pageCount) {
        this.title = title;
        this.subtitle = subtitle;
        this.authors = authors;
        this.thumbnail = thumbnail;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.pageCount = pageCount;
    }

    public int getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(int catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public JSONArray getAuthors() {
        return authors;
    }

    public void setAuthors(JSONArray authors) {
        this.authors = authors;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}
