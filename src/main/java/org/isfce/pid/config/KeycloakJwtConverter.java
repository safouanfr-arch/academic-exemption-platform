package org.isfce.pid.config;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, JwtAuthenticationToken> {

	private final JwtGrantedAuthoritiesConverter convertisseurRolesSpring =
			new JwtGrantedAuthoritiesConverter();

	@Override
	public JwtAuthenticationToken convert(Jwt jwt) {
		Set<GrantedAuthority> rolesSpring = extraireTousLesRoles(jwt);
		String nomPrincipal = determinerNomPrincipal(jwt);
		return new JwtAuthenticationToken(jwt, rolesSpring, nomPrincipal);
	}

	/**
	 * On fusionne :
	 * - les roles standards de Spring
	 * - les roles Keycloak presents dans realm_access.roles
	 */
	private Set<GrantedAuthority> extraireTousLesRoles(Jwt jwt) {
		return Stream.concat(convertisseurRolesSpring.convert(jwt).stream(), extraireRolesRealm(jwt).stream())
				.collect(Collectors.toSet());
	}

	/**
	 * On veut que auth.getName() soit un vrai nom utilisateur lisible.
	 * Si Keycloak donne preferred_username, on l'utilise.
	 * Sinon on retombe sur le sub technique.
	 */
	private String determinerNomPrincipal(Jwt jwt) {
		String usernamePrefere = jwt.getClaimAsString("preferred_username");
		return Optional.ofNullable(usernamePrefere).orElse(jwt.getClaimAsString(JwtClaimNames.SUB));
	}

	/**
	 * Keycloak place ses roles dans realm_access.roles.
	 * On les transforme au format attendu par Spring : ROLE_ETUDIANT, ROLE_ADMIN, etc.
	 */
	private Collection<GrantedAuthority> extraireRolesRealm(Jwt jwt) {
		if (jwt.getClaim("realm_access") == null) {
			return Set.of();
		}

		Map<String, Object> realmAccess = jwt.getClaim("realm_access");

		@SuppressWarnings("unchecked")
		Collection<String> roles = (Collection<String>) realmAccess.get("roles");

		if (roles == null) {
			return Set.of();
		}

		return roles.stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.collect(Collectors.toSet());
	}
}
