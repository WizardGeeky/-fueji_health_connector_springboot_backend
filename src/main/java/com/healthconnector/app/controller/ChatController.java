package com.healthconnector.app.controller;

import com.healthconnector.app.dto.request.SendMessageRequest;
import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.dto.response.ChatMessageResponse;
import com.healthconnector.app.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Bidirectional messaging between Provider and Payer on authorizations")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

	@Autowired
    private ChatService chatService;

    @PostMapping("/authorizations/{authorizationId}/messages")
    @PreAuthorize("hasAnyRole('PROVIDER','PAYER')")
    @Operation(summary = "Send Message", description = "Send an encrypted message on an authorization thread")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable("authorizationId") String authorizationId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        ChatMessageResponse response = chatService.sendMessage(authorizationId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent", response));
    }

    @GetMapping("/authorizations/{authorizationId}/messages")
    @PreAuthorize("hasAnyRole('PROVIDER','PAYER','SUPER_ADMIN')")
    @Operation(summary = "Get Messages", description = "Get paginated message history for an authorization. Marks received messages as read.")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            @PathVariable("authorizationId") String authorizationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(authorizationId, pageable)));
    }
}
