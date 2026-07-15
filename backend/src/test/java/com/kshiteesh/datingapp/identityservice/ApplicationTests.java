package com.kshiteesh.datingapp.identityservice;

import com.kshiteesh.datingapp.identityservice.identity.repository.IdentityRepository;
import com.kshiteesh.datingapp.identityservice.otp.repository.OtpChallengeRepository;
import com.kshiteesh.datingapp.identityservice.token.access.AccessTokenService;
import com.kshiteesh.datingapp.identityservice.token.refresh.RefreshSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
		"identity.otp.delivery-provider=local"
})
class ApplicationTests {

	@MockitoBean
	private IdentityRepository identityRepository;

	@MockitoBean
	private OtpChallengeRepository otpChallengeRepository;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@MockitoBean
	private AccessTokenService accessTokenService;

	@Test
	void contextLoads() {
	}

}
