package com.healthconnector.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Send a chat message on an authorization")
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    @Schema(description = "Message content (will be AES encrypted)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
