package io.bluetape4k.hibernate.reactive.examples.model;

import java.time.LocalDate;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Book.class)
public abstract class Book_ {

    public static volatile SingularAttribute<Book, Long> id;
    public static volatile SingularAttribute<Book, String> isbn;
    public static volatile SingularAttribute<Book, String> title;
    public static volatile SingularAttribute<Book, LocalDate> published;
    public static volatile SingularAttribute<Book, Author> author;

    public static final String ID = "id";
    public static final String ISBN = "isbn";
    public static final String TITLE = "title";
    public static final String PUBLISHED = "published";
    public static final String AUTHOR = "author";
}
