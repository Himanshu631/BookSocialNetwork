package com.gamtech.booknetwork.feedback;

import com.gamtech.booknetwork.book.Book;
import com.gamtech.booknetwork.book.BookRepository;
import com.gamtech.booknetwork.book.exception.OpearationNotPermittedException;
import com.gamtech.booknetwork.common.PageResponse;
import com.gamtech.booknetwork.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public Integer save(FeedbackRequest feedbackRequest, Authentication connectedUser) {
        Book book = bookRepository.findById(feedbackRequest.bookId()).orElseThrow(()-> new EntityNotFoundException("No Book found with Id "+feedbackRequest.bookId()));
        if (book.isArchived() || !book.isShareable()){
            throw new OpearationNotPermittedException("The requested book cannot be borrowed");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OpearationNotPermittedException("You cannot approve return of this book, as you are not the owner");
        }

        Feedback feedback = feedbackMapper.toFeedBack(feedbackRequest);
        return feedbackRepository.save(feedback).getId();
    }

    public PageResponse<FeedbackResponse> findAllFeedBackByBookId(int page, int size, Integer bookId, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        User user = ((User) connectedUser.getPrincipal());
        Page<Feedback> feedbacks = feedbackRepository.findAllByBookId(bookId,pageable);

        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedBackResponse(f,user.getId()))
                .toList();
        return new PageResponse<>(
                feedbackResponses, feedbacks.getNumber(), feedbacks.getSize(), feedbacks.getTotalElements(), feedbacks.getTotalPages(), feedbacks.isFirst(), feedbacks.isLast()
        );
    }
}
