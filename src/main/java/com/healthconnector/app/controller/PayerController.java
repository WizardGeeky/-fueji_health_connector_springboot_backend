package com.healthconnector.app.controller;

import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.dto.request.CreateUserRequest;
import com.healthconnector.app.dto.request.UpdateUserRequest;
import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.dto.response.UserResponse;
import com.healthconnector.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payers")
@Tag(name = "Payers", description = "Payer management by Super Admin")
@SecurityRequirement(name = "bearerAuth")
public class PayerController {

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Payer")
    public ResponseEntity<ApiResponse<UserResponse>> createPayer(
            @Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        request.setRole(UserRole.PAYER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payer created successfully", userService.createUser(request, httpRequest)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER')")
    @Operation(summary = "List Payers")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listPayers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByRole(UserRole.PAYER, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PROVIDER')")
    @Operation(summary = "Get Payer")
    public ResponseEntity<ApiResponse<UserResponse>> getPayer(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Payer")
    public ResponseEntity<ApiResponse<UserResponse>> updatePayer(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payer updated", userService.updateUser(id, request, httpRequest)));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Block Payer")
    public ResponseEntity<ApiResponse<UserResponse>> blockPayer(@PathVariable("id") String id, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payer blocked", userService.blockUser(id, req)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate Payer")
    public ResponseEntity<ApiResponse<UserResponse>> activatePayer(@PathVariable("id") String id, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payer activated", userService.activateUser(id, req)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset Payer Password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable("id") String id, HttpServletRequest req) {
        String newPassword = userService.resetPassword(id, req);
        return ResponseEntity.ok(ApiResponse.success("Password reset. Email sent to payer.", newPassword));
    }
}
