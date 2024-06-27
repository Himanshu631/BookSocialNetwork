package com.gamtech.booknetwork.feedback;

import com.gamtech.booknetwork.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
@Tag(name = "FeedBack")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Integer> saveFeedback(@Valid @RequestBody FeedbackRequest feedbackRequest, Authentication connectedUser){
        return ResponseEntity.ok(feedbackService.save(feedbackRequest,connectedUser));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<PageResponse<FeedbackResponse>> findAllFeedBackByBookId(@PathVariable Integer bookId,
                                                                                  @RequestParam(name = "page", defaultValue = "0", required = false) int page,
                                                                                  @RequestParam(name = "size", defaultValue = "10", required = false) int size, Authentication connectedUser){
        return ResponseEntity.ok(feedbackService.findAllFeedBackByBookId(page,size,bookId,connectedUser));
    }
}
