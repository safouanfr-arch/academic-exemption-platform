package org.isfce.pid.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class ApiTokenValidationTest {
	private static final String ISSUER = "https://identity.example.test/realms/DISP";
	private static final String API_AUDIENCE = "dispense-api";
	private static RSAPrivateKey signingKey;
	private static NimbusJwtDecoder decoder;

	@BeforeAll
	static void useLocalTestKeys() throws Exception {
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		var keys = generator.generateKeyPair();
		signingKey = (RSAPrivateKey) keys.getPrivate();
		decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
		decoder.setJwtValidator(SecurityConfig.apiTokenValidator(ISSUER, API_AUDIENCE));
	}

	@Test
	void acceptsSignedTokenForTheConfiguredApi() throws JOSEException {
		var jwt = decoder.decode(token(ISSUER, List.of(API_AUDIENCE, "account"), Instant.now().plusSeconds(120)));
		assertThat(jwt.getSubject()).isEqualTo("synthetic-student");
	}

	@Test
	void rejectsAnUnexpectedIssuerEvenWithTheTestSigningKey() throws JOSEException {
		String jwt = token("https://other-issuer.example.test", List.of(API_AUDIENCE), Instant.now().plusSeconds(120));
		assertThatThrownBy(() -> decoder.decode(jwt)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void rejectsMissingAndUnrelatedAudiences() throws JOSEException {
		String missing = token(ISSUER, List.of(), Instant.now().plusSeconds(120));
		String unrelated = token(ISSUER, List.of("another-api"), Instant.now().plusSeconds(120));
		assertThatThrownBy(() -> decoder.decode(missing)).isInstanceOf(JwtValidationException.class);
		assertThatThrownBy(() -> decoder.decode(unrelated)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void stillRejectsExpiredTokens() throws JOSEException {
		String jwt = token(ISSUER, List.of(API_AUDIENCE), Instant.now().minusSeconds(180));
		assertThatThrownBy(() -> decoder.decode(jwt)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void permitsOnlyConfiguredOriginsOnApiRoutes() {
		var cors = SecurityConfig.apiCorsConfigurationSource(List.of("http://localhost:4200"));
		var config = cors.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/dossier/liste"));
		assertThat(config).isNotNull();
		assertThat(config.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
		assertThat(config.checkOrigin("https://other.example.test")).isNull();
		assertThat(config.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
		assertThat(cors.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/h2/"))).isNull();
	}

	private String token(String issuer, List<String> audience, Instant expiresAt) throws JOSEException {
		var claims = new JWTClaimsSet.Builder().subject("synthetic-student").issuer(issuer)
				.expirationTime(Date.from(expiresAt));
		if (!audience.isEmpty()) {
			claims.audience(audience);
		}
		var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
		jwt.sign(new RSASSASigner(signingKey));
		return jwt.serialize();
	}
}
