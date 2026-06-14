package com.healthconnector.app.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.healthconnector.app.constants.UserStatus;
import com.healthconnector.app.exception.ForbiddenException;
import com.healthconnector.app.model.User;
import com.healthconnector.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security UserDetailsService implementation. Loads user by email (for
 * authentication) or by ID (for JWT filter).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	/**
	 * Called by Spring Security during form-based / DAO authentication.
	 */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
		return buildUserDetails(user);
	}

	/**
	 * Called by {@link JwtAuthenticationFilter} to resolve user from JWT subject
	 * (userId).
	 */
	public UserDetails loadUserById(String userId) {
		User user = userRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
		return buildUserDetails(user);
	}

	private UserDetails buildUserDetails(User user) {
		if (user.isAccountLocked()) {
			throw new ForbiddenException("Account is locked due to too many failed login attempts");
		}
		if (user.getStatus() == UserStatus.BLOCKED) {
			throw new ForbiddenException("Account has been blocked. Please contact administrator");
		}
		return org.springframework.security.core.userdetails.User.builder().username(user.getId())
				.password(user.getPassword())
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
				.accountLocked(user.isAccountLocked())
				.disabled(user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING).build();
	}
}
