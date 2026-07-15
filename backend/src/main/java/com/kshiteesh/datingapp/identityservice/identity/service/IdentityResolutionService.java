package com.kshiteesh.datingapp.identityservice.identity.service;

import com.kshiteesh.datingapp.identityservice.identity.entity.Identity;
import com.kshiteesh.datingapp.identityservice.identity.repository.IdentityRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityResolutionService {

	private final IdentityRepository identityRepository;

	public IdentityResolutionService(IdentityRepository identityRepository) {
		this.identityRepository = identityRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Identity> findByPhoneLookupHash(String phoneLookupHash) {
		return identityRepository.findByPhoneNumberHash(requireLookupHash(phoneLookupHash));
	}

	@Transactional(readOnly = true)
	public boolean existsByPhoneLookupHash(String phoneLookupHash) {
		return identityRepository.existsByPhoneNumberHash(requireLookupHash(phoneLookupHash));
	}

	@Transactional
	public Identity getOrCreateAfterSuccessfulAuthentication(String phoneLookupHash) {
		String lookupHash = requireLookupHash(phoneLookupHash);
		return identityRepository.findByPhoneNumberHash(lookupHash)
				.orElseGet(() -> createOrLoadExistingIdentity(lookupHash));
	}

	private Identity createOrLoadExistingIdentity(String phoneLookupHash) {
		try {
			return identityRepository.saveAndFlush(Identity.active(phoneLookupHash));
		} catch (DataIntegrityViolationException ex) {
			return identityRepository.findByPhoneNumberHash(phoneLookupHash)
					.orElseThrow(() -> ex);
		}
	}

	private static String requireLookupHash(String phoneLookupHash) {
		if (phoneLookupHash == null || phoneLookupHash.isBlank()) {
			throw new IllegalArgumentException("phoneLookupHash must not be blank");
		}
		return phoneLookupHash;
	}
}
