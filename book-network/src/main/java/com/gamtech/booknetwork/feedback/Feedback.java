package com.gamtech.booknetwork.feedback;

import com.gamtech.booknetwork.book.Book;
import com.gamtech.booknetwork.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
public class Feedback extends BaseEntity {

    private String comment;
    private Double note;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
}
