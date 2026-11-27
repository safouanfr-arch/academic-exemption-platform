package org.isfce.pid.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final KeycloakJwtConverter jwtConverter;

	public SecurityConfig(KeycloakJwtConverter jwtConverter) {
		this.jwtConverter = jwtConverter;
	}

	@Bean
	@Profile("dev")
	WebSecurityCustomizer ignoreH2ConsoleInDev() {
		return web -> web.ignoring().requestMatchers("/h2/**");
	}

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
			@Value("${pid.security.audience}") String audience
	) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		decoder.setJwtValidator(apiTokenValidator(issuer, audience));
		return decoder;
	}

	static OAuth2TokenValidator<Jwt> apiTokenValidator(String issuer, String audience) {
		if (audience.isBlank()) {
			throw new IllegalArgumentException("A non-empty API audience is required");
		}
		var audienceValidator = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD, values -> values != null && values.contains(audience));
		return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
	}

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder decoder,
			@Value("${pid.security.allowed-origins:}") List<String> allowedOrigins) throws Exception {
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.csrf(csrf -> csrf.disable());
		http.cors(cors -> cors.configurationSource(apiCorsConfigurationSource(allowedOrigins)));

		http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
				.decoder(decoder)
				.jwtAuthenticationConverter(jwtConverter)));

		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/ue/sections", "/api/ue/liste", "/api/ue/detail/*")
				.hasAnyRole("ETUDIANT", "ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/ue/*/delete").hasAnyRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/ue/add").hasAnyRole("ADMIN")
				.anyRequest().authenticated());

		return http.build();
	}

	static CorsConfigurationSource apiCorsConfigurationSource(List<String> allowedOrigins) {
		if (allowedOrigins.contains("*")) {
			throw new IllegalArgumentException("Configure explicit API origins");
		}
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
