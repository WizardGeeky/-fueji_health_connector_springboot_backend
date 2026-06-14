package com.healthconnector.app.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User domain entity representing all three roles: SUPER_ADMIN, PROVIDER,
 * PAYER. Passwords are BCrypt-hashed. Soft delete is implemented via the
 * {@code deleted} flag.
 */
@Document(collection = AppConstants.COL_USERS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	private String id;

	@Field("first_name")
	private String firstName;

	@Field("last_name")
	private String lastName;

	@Indexed(unique = true, sparse = true)
	@Field("email")
	private String email;

	@Field("mobile")
	private String mobile;

	/** BCrypt-hashed password. Never plain-text. */
	@Field("password")
	private String password;

	@Field("role")
	private UserRole role;

	@Field("organization_id")
	private String organizationId;

	@Field("organization_name")
	private String organizationName;

	/** National Provider Identifier (10-digit). */
	@Field("npi")
	private String npi;

	@Field("address")
	private String address;

	@Field("status")
	@Builder.Default
	private UserStatus status = UserStatus.PENDING;

	@Field("failed_attempts")
	@Builder.Default
	private int failedAttempts = 0;

	@Field("account_locked")
	@Builder.Default
	private boolean accountLocked = false;

	/** True if user has changed the temporary password assigned at creation. */
	@Field("password_changed")
	@Builder.Default
	private boolean passwordChanged = false;

	@Field("last_login")
	private Instant lastLogin;

	@Field("password_changed_at")
	private Instant passwordChangedAt;

	@CreatedDate
	@Field("created_at")
	private Instant createdAt;

	@LastModifiedDate
	@Field("updated_at")
	private Instant updatedAt;

	@CreatedBy
	@Field("created_by")
	private String createdBy;

	@LastModifiedBy
	@Field("updated_by")
	private String updatedBy;

	/** Last admin-set temp password stored in plain text for super admin visibility. Cleared when user changes own password. */
	@Field("last_admin_password")
	private String lastAdminPassword;

	@Field("password_reset_token")
	private String passwordResetToken;

	@Field("password_reset_token_expiry")
	private Instant passwordResetTokenExpiry;

	@Field("deleted")
	@Builder.Default
	private boolean deleted = false;

	@Field("deleted_at")
	private Instant deletedAt;

	@Version
	private Long version;

	// ==========================================
	// Explicit Getters and Setters (Bypassing Lombok for IDEs)
	// ==========================================

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }

	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getMobile() { return mobile; }
	public void setMobile(String mobile) { this.mobile = mobile; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public UserRole getRole() { return role; }
	public void setRole(UserRole role) { this.role = role; }

	public String getOrganizationId() { return organizationId; }
	public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

	public String getOrganizationName() { return organizationName; }
	public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

	public String getNpi() { return npi; }
	public void setNpi(String npi) { this.npi = npi; }

	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }

	public UserStatus getStatus() { return status; }
	public void setStatus(UserStatus status) { this.status = status; }

	public int getFailedAttempts() { return failedAttempts; }
	public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

	public Instant getLastLogin() { return lastLogin; }
	public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }

	public Instant getPasswordChangedAt() { return passwordChangedAt; }
	public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	public String getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

	public String getLastAdminPassword() { return lastAdminPassword; }
	public void setLastAdminPassword(String lastAdminPassword) { this.lastAdminPassword = lastAdminPassword; }

	public String getPasswordResetToken() { return passwordResetToken; }
	public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

	public Instant getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
	public void setPasswordResetTokenExpiry(Instant passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }

	public Instant getDeletedAt() { return deletedAt; }
	public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public boolean isAccountLocked() { return this.accountLocked; }
	public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

	public boolean isPasswordChanged() { return this.passwordChanged; }
	public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }

	public boolean isDeleted() { return this.deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
