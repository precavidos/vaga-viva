package io.github.precavidos.vagaviva.archetype;

import java.util.List;

/** The archetype catalog file is inconsistent; the application does not boot with it. */
public class InvalidArchetypeCatalogException extends RuntimeException {

	InvalidArchetypeCatalogException(List<String> problems) {
		super("Invalid archetype catalog:\n- " + String.join("\n- ", problems));
	}

}
