package io.bluetape4k.hibernate.reactive.examples.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Author.class)
public abstract class Author_ {

    public static volatile SingularAttribute<Author, Long> id;
    public static volatile SingularAttribute<Author, String> name;
    public static volatile ListAttribute<Author, Book> books;

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String BOOKS = "books";
}
