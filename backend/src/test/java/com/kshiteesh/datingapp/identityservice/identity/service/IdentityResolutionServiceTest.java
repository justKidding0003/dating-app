package com.kshiteesh.datingapp.identityservice.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kshiteesh.datingapp.identityservice.identity.entity.Identity;
import com.kshiteesh.datingapp.identityservice.identity.repository.IdentityRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class IdentityResolutionServiceTest {

	private static final String PHONE_LOOKUP_HASH =
			"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final IdentityRepository identityRepository = org.mockito.Mockito.mock(IdentityRepository.class);
	private final IdentityResolutionService identityResolutionService =
			new IdentityResolutionService(identityRepository);

	@Test
	void getOrCreateAfterSuccessfulAuthenticationReturnsExistingIdentity() {
		Identity existingIdentity = Identity.active(PHONE_LOOKUP_HASH);
		when(identityRepository.findByPhoneNumberHash(PHONE_LOOKUP_HASH)).thenReturn(Optional.of(existingIdentity));

		Identity resolvedIdentity = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);

		assertThat(resolvedIdentity).isSameAs(existingIdentity);
		verify(identityRepository, never()).saveAndFlush(any(Identity.class));
	}

	@Test
	void getOrCreateAfterSuccessfulAuthenticationCreatesIdentityForUnknownLookupHash() {
		when(identityRepository.findByPhoneNumberHash(PHONE_LOOKUP_HASH)).thenReturn(Optional.empty());
		when(identityRepository.saveAndFlush(any(Identity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Identity resolvedIdentity = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);

		assertThat(resolvedIdentity.getIdentityId()).isInstanceOf(UUID.class);
		assertThat(resolvedIdentity.getPhoneNumberHash()).isEqualTo(PHONE_LOOKUP_HASH);
	}

	@Test
	void getOrCreateAfterSuccessfulAuthenticationDoesNotCreateDuplicateOnRepeatedCalls() {
		Identity existingIdentity = Identity.active(PHONE_LOOKUP_HASH);
		when(identityRepository.findByPhoneNumberHash(PHONE_LOOKUP_HASH))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(existingIdentity));
		when(identityRepository.saveAndFlush(any(Identity.class))).thenReturn(existingIdentity);

		Identity firstResolution = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);
		Identity secondResolution = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);

		assertThat(firstResolution).isSameAs(existingIdentity);
		assertThat(secondResolution).isSameAs(existingIdentity);
		verify(identityRepository).saveAndFlush(any(Identity.class));
	}

	@Test
	void getOrCreateAfterSuccessfulAuthenticationLoadsIdentityAfterUniquenessRace() {
		Identity createdByConcurrentTransaction = Identity.active(PHONE_LOOKUP_HASH);
		when(identityRepository.findByPhoneNumberHash(PHONE_LOOKUP_HASH))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(createdByConcurrentTransaction));
		when(identityRepository.saveAndFlush(any(Identity.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate phone lookup hash"));

		Identity resolvedIdentity = identityResolutionService.getOrCreateAfterSuccessfulAuthentication(PHONE_LOOKUP_HASH);

		assertThat(resolvedIdentity).isSameAs(createdByConcurrentTransaction);
	}

	@Test
	void identityModelDoesNotIntroduceProfileFields() {
		assertThat(Arrays.stream(Identity.class.getDeclaredFields()).map(field -> field.getName()))
				.containsExactlyInAnyOrder("identityId", "phoneNumberHash", "status", "createdAt", "updatedAt");
	}
}
