package org.isfce.pid.controller.error;

/**
 * Exception simple utilisee quand on essaye de creer un element deja present.
 * Exemple typique : une UE avec un code deja existant.
 */
public class DuplicateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String champ;

	public DuplicateException(String message, String champ) {
		super(message);
		this.champ = champ;
	}

	public String getChamp() {
		return champ;
	}
}
