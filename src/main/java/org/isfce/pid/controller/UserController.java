package org.isfce.pid.controller;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.dto.UserDto;
import org.isfce.pid.model.User;
import org.isfce.pid.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/user", produces = "application/json")
@CrossOrigin("*")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Route admin : liste complete des utilisateurs applicatifs.
	 */
	@PreAuthorize("hasAnyRole('ADMIN')")
	@GetMapping(params = "all")
	public ResponseEntity<List<UserDto>> listerTousLesUtilisateurs() {
		return ResponseEntity.ok(userService.getAllUsersDto());
	}

	/**
	 * Regle de securite importante :
	 * - un admin peut consulter n'importe quel profil
	 * - un etudiant ne peut consulter que son propre profil
	 *
	 * #username = nom utilisateur demande dans l'URL
	 * authentication.name = nom utilisateur reconstruit depuis le JWT
	 */
	@GetMapping("/profile/{username}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserDto> consulterProfilUtilisateur(
			@PathVariable("username") String username,
			JwtAuthenticationToken authentication) {

		if (!peutConsulterProfil(username, authentication)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse au profil");
		}

		Optional<User> userExistant = userService.findUserByUsername(username);
		User userLocal = userExistant.orElse(null);

		// Si le profil local n'existe pas encore, on le cree a partir du token,
		// mais seulement pour l'utilisateur lui-meme.
		if (userExistant.isEmpty() && authentication.getName().equals(username)) {
			userLocal = userService.saveUser(creerUtilisateurDepuisToken(username, authentication));
		}

		// On renvoie 404 si le profil n'existe pas, pour ne pas exposer plus d'infos.
		if (userLocal == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(convertirEnUserDto(username, userLocal));
	}

	private boolean peutConsulterProfil(String username, JwtAuthenticationToken authentication) {
		boolean estAdmin = authentication.getAuthorities().stream()
				.anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()));
		return estAdmin || authentication.getName().equals(username);
	}

	private User creerUtilisateurDepuisToken(String username, JwtAuthenticationToken authentication) {
		var token = authentication.getToken();
		String email = token.getClaimAsString("email");
		String nom = token.getClaimAsString("family_name");
		String prenom = token.getClaimAsString("given_name");
		return new User(username, email, nom, prenom);
	}

	private UserDto convertirEnUserDto(String username, User user) {
		return new UserDto(username, user.getEmail(), user.getNom(), user.getPrenom());
	}
}
