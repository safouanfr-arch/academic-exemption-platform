package org.isfce.pid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
	private String jwkSetUri;

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
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		// API REST classique : pas de session serveur, tout repose sur le JWT.
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.csrf(csrf -> csrf.disable());
		http.cors(cors -> cors.configurationSource(openCorsConfigurationSource()));

		// Spring verifie le token JWT avec les cles publiques exposees par Keycloak.
		http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
				.jwkSetUri(jwkSetUri)
				.jwtAuthenticationConverter(jwtConverter)));

		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/ue/sections", "/api/ue/liste", "/api/ue/detail/*")
				.hasAnyRole("ETUDIANT", "ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/ue/*/delete").hasAnyRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/ue/add").hasAnyRole("ADMIN")
				.anyRequest().authenticated());

		return http.build();
	}

	@Bean
	CorsConfigurationSource openCorsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// Configuration volontairement large pour le projet.
		configuration.addAllowedOriginPattern("*");
		configuration.addAllowedMethod("*");
		configuration.addAllowedHeader("*");
		configuration.addExposedHeader("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
