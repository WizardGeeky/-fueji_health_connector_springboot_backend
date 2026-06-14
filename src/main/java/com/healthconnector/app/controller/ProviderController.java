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
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "Provider management by Super Admin")
@SecurityRequirement(name = "bearerAuth")
public class ProviderController {

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Provider")
    public ResponseEntity<ApiResponse<UserResponse>> createProvider(
            @Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        request.setRole(UserRole.PROVIDER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Provider created successfully", userService.createUser(request, httpRequest)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Providers")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listProviders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByRole(UserRole.PROVIDER, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PAYER')")
    @Operation(summary = "Get Provider")
    public ResponseEntity<ApiResponse<UserResponse>> getProvider(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Provider")
    public ResponseEntity<ApiResponse<UserResponse>> updateProvider(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Provider updated", userService.updateUser(id, request, httpRequest)));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Block Provider")
    public ResponseEntity<ApiResponse<UserResponse>> blockProvider(@PathVariable("id") String id, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Provider blocked", userService.blockUser(id, req)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate Provider")
    public ResponseEntity<ApiResponse<UserResponse>> activateProvider(@PathVariable("id") String id, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Provider activated", userService.activateUser(id, req)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset Provider Password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable("id") String id, HttpServletRequest req) {
        String newPassword = userService.resetPassword(id, req);
        return ResponseEntity.ok(ApiResponse.success("Password reset. Email sent to provider.", newPassword));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete Provider")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable("id") String id, HttpServletRequest req) {
        userService.softDeleteUser(id, req);
        return ResponseEntity.ok(ApiResponse.success("Provider deleted"));
    }
}
