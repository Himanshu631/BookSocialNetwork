package com.gamtech.booknetwork.book;

import com.gamtech.booknetwork.book.exception.OpearationNotPermittedException;
import com.gamtech.booknetwork.book.file.FileStorageService;
import com.gamtech.booknetwork.common.PageResponse;
import com.gamtech.booknetwork.history.BookTransactionHistory;
import com.gamtech.booknetwork.history.BookTransactionHistoryRepository;
import com.gamtech.booknetwork.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest bookRequest, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(bookRequest);

        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
    return bookRepository.findById(bookId)
            .map(bookMapper::toBookResponse)
            .orElseThrow(() -> new EntityNotFoundException("No book found with book id "+ bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getUserId());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getUserId()),pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllBorrowedBooks(pageable,user.getUserId());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allReturnedBooksBooks = bookTransactionHistoryRepository.findAllReturnedBooks(pageable,user.getUserId());
        List<BorrowedBookResponse> bookResponse = allReturnedBooksBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allReturnedBooksBooks.getNumber(),
                allReturnedBooksBooks.getSize(),
                allReturnedBooksBooks.getTotalElements(),
                allReturnedBooksBooks.getTotalPages(),
                allReturnedBooksBooks.isFirst(),
                allReturnedBooksBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getUserId(),user.getUserId())){
            throw new OpearationNotPermittedException("Cannot update book as you are not the owner");
        }
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchiveStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getUserId(),user.getUserId())){
            throw new OpearationNotPermittedException("Cannot archive book as you are not the owner");
        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        if (book.isArchived() || !book.isShareable()){
            throw new OpearationNotPermittedException("The requested book cannot be borrowed");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getUserId(),user.getUserId())){
            throw new OpearationNotPermittedException("Owner cannot borrow his own book");
        }

        final boolean isAlreadyBorrowed = bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getUserId());

        if (isAlreadyBorrowed) {
            throw new OpearationNotPermittedException("Requested book is already borrowed");
        }

        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();

        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        if (book.isArchived() || !book.isShareable()){
            throw new OpearationNotPermittedException("The requested book cannot be borrowed");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getUserId(),user.getUserId())){
            throw new OpearationNotPermittedException("Owner cannot return his own book");
        }

        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository.findByBookIdAndUserId(bookId,user.getUserId()).orElseThrow(()-> new OpearationNotPermittedException("User has not borrowed the book"));

        bookTransactionHistory.setReturned(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        if (book.isArchived() || !book.isShareable()){
            throw new OpearationNotPermittedException("The requested book cannot be borrowed");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getUserId(),user.getUserId())){
            throw new OpearationNotPermittedException("You cannot approve return of this book, as you are not the owner");
        }

        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository.findByBookIdAndOwnerId(bookId,user.getUserId()).orElseThrow(()->
                new OpearationNotPermittedException("The book is not returned yet"));

        bookTransactionHistory.setReturnApproved(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+bookId));
        User user = ((User) connectedUser.getPrincipal());

        var bookCover = fileStorageService.saveFile(file, user.getUserId());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }
}
