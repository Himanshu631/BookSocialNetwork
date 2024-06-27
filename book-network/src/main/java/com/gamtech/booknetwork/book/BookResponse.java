package com.gamtech.booknetwork.book;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookResponse {

    private Integer id;
    private String title;
    private String authorName;
    private String isbn;
    private byte[] bookCover;
    private boolean archived;
    private boolean shareable;
    private double rate;
    private String synopsis;
    private String owner;

}
