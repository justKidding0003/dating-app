package com.kshiteesh.datingapp.identityservice.identity.repository;

import com.kshiteesh.datingapp.identityservice.identity.entity.Identity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, UUID> {

	Optional<Identity> findByPhoneNumberHash(String phoneNumberHash);

	boolean existsByPhoneNumberHash(String phoneNumberHash);
}
