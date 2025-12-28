package com.elib.library;

public class Book {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private int year;
    private boolean available;
    private Long issueDate; // timestamp in millis
    private Long returnDate; // timestamp in millis
    private Double fine; // calculated fine
    private String borrowerId;
    private String lastBorrowerId;
    private Long dueDate; // timestamp in millis
    private String borrowerEmail;
    private String description;
    private String pdfUrl;

    public Book() {
        // Default constructor required for Firestore
        this.available = true;
        this.fine = 0.0;
    }

    public Book(String title, String author, String isbn, int year, boolean available) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.year = year;
        this.available = available;
        this.fine = 0.0;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Long getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Long issueDate) {
        this.issueDate = issueDate;
    }

    public Long getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Long returnDate) {
        this.returnDate = returnDate;
    }

    public Double getFine() {
        return fine;
    }

    public void setFine(Double fine) {
        this.fine = fine;
    }
    public String getBorrowerId() {
        return borrowerId;
    }
    public void setBorrowerId(String borrowerId) {
        this.borrowerId = borrowerId;
    }
    public String getLastBorrowerId() {
        return lastBorrowerId;
    }
    public void setLastBorrowerId(String lastBorrowerId) {
        this.lastBorrowerId = lastBorrowerId;
    }
    public Long getDueDate() {
        return dueDate;
    }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
    }
    public String getBorrowerEmail() {
        return borrowerEmail;
    }
    public void setBorrowerEmail(String borrowerEmail) {
        this.borrowerEmail = borrowerEmail;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
