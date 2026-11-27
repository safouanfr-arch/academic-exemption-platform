package org.isfce.pid.user;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.Optional;

import org.isfce.pid.model.User;
import org.isfce.pid.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "testU")
class TestUserController {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userServiceMock;

	@BeforeEach
	void setUp() {
		User et1 = new User("et1", "et1@example.test", "Nom Et1", "Prenom Et1");
		User et2 = new User("et2", "et2@example.test", "Nom Et2", "Prenom Et2");
		User val = new User("demo-admin", "demo-admin@example.test", "Demo", "Admin");

		when(userServiceMock.findUserByUsername("et1")).thenReturn(Optional.of(et1));
		when(userServiceMock.findUserByUsername("et2")).thenReturn(Optional.empty());
		when(userServiceMock.findUserByUsername("val")).thenReturn(Optional.of(val));
		when(userServiceMock.saveUser(et2)).thenReturn(et2);
	}

	@Test
	void testGetUserInfoEt1() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("sub", "et1")
				.claim("scope", "openid email profile")
				.claim("preferred_username", "et1")
				.claim("given_name", "Prenom Et1")
				.claim("family_name", "Nom Et1")
				.claim("email", "et1@example.test")
				.build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ETUDIANT");
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

		mockMvc.perform(get("/api/user/profile/et1").with(authentication(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("username").value("et1"))
				.andExpect(jsonPath("email").value("et1@example.test"));

		verify(userServiceMock).findUserByUsername("et1");
		verify(userServiceMock, times(0)).saveUser(null);
	}

	@Test
	void testGetUserInfoNew() throws Exception {
		User et2 = new User("et2", "et2@example.test", "Nom Et2", "Prenom Et2");
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("sub", "et2")
				.claim("scope", "openid email profile")
				.claim("preferred_username", "et2")
				.claim("given_name", "Prenom Et2")
				.claim("family_name", "Nom Et2")
				.claim("email", "et2@example.test")
				.build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ETUDIANT");
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

		mockMvc.perform(get("/api/user/profile/et2").with(authentication(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("username").value("et2"))
				.andExpect(jsonPath("email").value("et2@example.test"));

		verify(userServiceMock).findUserByUsername("et2");
		verify(userServiceMock).saveUser(et2);
	}

	@Test
	void testGetUserInfoEt1FromVo() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("sub", "")
				.claim("scope", "openid email profile")
				.claim("preferred_username", "demo-admin")
				.claim("given_name", "Admin")
				.claim("family_name", "Demo")
				.claim("email", "demo-admin@example.test")
				.build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ADMIN");
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

		mockMvc.perform(get("/api/user/profile/et1").with(authentication(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("username").value("et1"))
				.andExpect(jsonPath("email").value("et1@example.test"));

		verify(userServiceMock).findUserByUsername("et1");
		verify(userServiceMock, times(0)).saveUser(null);
	}

	@Test
	void testGetUserInfoBrolFromVo() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("sub", "")
				.claim("scope", "openid email profile")
				.claim("preferred_username", "demo-admin")
				.claim("given_name", "Admin")
				.claim("family_name", "Demo")
				.claim("email", "demo-admin@example.test")
				.build();
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ADMIN");
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

		mockMvc.perform(get("/api/user/profile/brol").with(authentication(token)))
				.andExpect(status().isNotFound());

		verify(userServiceMock).findUserByUsername("brol");
		verify(userServiceMock, times(0)).saveUser(null);
	}
}
