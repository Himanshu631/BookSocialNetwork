package com.gamtech.booknetwork.book;

import com.gamtech.booknetwork.book.file.FileUtils;
import com.gamtech.booknetwork.history.BookTransactionHistory;
import org.springframework.stereotype.Service;

@Service
public class BookMapper {
    public Book toBook(BookRequest bookRequest) {
        return Book.builder()
                .id(bookRequest.id())
                .authorName(bookRequest.authorName())
                .isbn(bookRequest.isbn())
                .title(bookRequest.title())
                .archived(false)
                .shareable(bookRequest.shareable())
                .build();
    }

    public BookResponse toBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .rate(book.getRate())
                .authorName(book.getAuthorName())
                .isbn(book.getIsbn())
                .archived(book.isArchived())
                .shareable(book.isShareable())
                .synopsis(book.getSynopsis())
                .owner(book.getOwner().getFullName())
                .bookCover(FileUtils.readFileFromLocation(book.getBookCover()))
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory history) {
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .rate(history.getBook().getRate())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .shareable(history.getBook().isShareable())
                .returned(history.isReturned())
                .returnApproved(history.isReturnedApproved())
                .build();
    }
}
