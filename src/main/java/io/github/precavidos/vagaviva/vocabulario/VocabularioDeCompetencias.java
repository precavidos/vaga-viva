package io.github.precavidos.vagaviva.vocabulario;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import tools.jackson.databind.json.JsonMapper;

/**
 * Vocabulário fechado de competências e tecnologias nomeadas de um arquétipo.
 * Só existe em estado válido: qualquer inconsistência impede a criação.
 */
public final class VocabularioDeCompetencias {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final String arquetipo;

	private final Map<String, Competencia> competencias;

	private final List<TecnologiaNomeada> tecnologias;

	private final List<TermoRecusado> termosRecusados;

	private VocabularioDeCompetencias(Arquivo arquivo) {
		this.arquetipo = arquivo.arquetipo();
		this.tecnologias = arquivo.tecnologias() == null ? List.of() : List.copyOf(arquivo.tecnologias());
		this.termosRecusados = arquivo.termosRecusados() == null ? List.of() : List.copyOf(arquivo.termosRecusados());
		var problemas = new ArrayList<String>();
		this.competencias = indexar(arquivo.competencias(), problemas);
		validarTecnologias(problemas);
		validarTermos(problemas);
		if (!problemas.isEmpty()) {
			throw new VocabularioInvalidoException(arquetipo, problemas);
		}
	}

	public static VocabularioDeCompetencias ler(InputStream entrada) {
		return new VocabularioDeCompetencias(JSON.readValue(entrada, Arquivo.class));
	}

	public static VocabularioDeCompetencias doClasspath(String caminho) {
		try (InputStream entrada = VocabularioDeCompetencias.class.getClassLoader().getResourceAsStream(caminho)) {
			if (entrada == null) {
				throw new IllegalArgumentException("Vocabulário não encontrado no classpath: " + caminho);
			}
			return ler(entrada);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String arquetipo() {
		return arquetipo;
	}

	public List<Competencia> competencias() {
		return List.copyOf(competencias.values());
	}

	public Optional<Competencia> competencia(String id) {
		return Optional.ofNullable(competencias.get(id));
	}

	public List<TecnologiaNomeada> tecnologias() {
		return tecnologias;
	}

	public List<TermoRecusado> termosRecusados() {
		return termosRecusados;
	}

	private static Map<String, Competencia> indexar(List<Competencia> lista, List<String> problemas) {
		var indice = new LinkedHashMap<String, Competencia>();
		for (Competencia competencia : lista == null ? List.<Competencia>of() : lista) {
			if (indice.putIfAbsent(competencia.id(), competencia) != null) {
				problemas.add("competência repetida: " + competencia.id());
			}
			if (competencia.rotulo() == null || competencia.rotulo().isBlank()) {
				problemas.add("competência " + competencia.id() + " sem rótulo");
			}
			if (competencia.esco() != null && !competencia.esco().uriValida()) {
				problemas.add("competência " + competencia.id() + " tem URI ESCO inválida");
			}
		}
		return indice;
	}

	private void validarTecnologias(List<String> problemas) {
		Set<String> ids = new HashSet<>();
		for (TecnologiaNomeada tecnologia : tecnologias) {
			if (!ids.add(tecnologia.id())) {
				problemas.add("tecnologia repetida: " + tecnologia.id());
			}
			if (tecnologia.termos().isEmpty()) {
				problemas.add("tecnologia " + tecnologia.id() + " não tem termos");
			}
			if (tecnologia.competencias().isEmpty()) {
				problemas.add("tecnologia " + tecnologia.id() + " não aponta para nenhuma competência");
			}
			for (String id : tecnologia.competencias()) {
				if (!competencias.containsKey(id)) {
					problemas.add("tecnologia " + tecnologia.id() + " aponta para competência inexistente: " + id);
				}
			}
		}
	}

	private void validarTermos(List<String> problemas) {
		var recusados = new HashMap<String, TermoRecusado>();
		termosRecusados.forEach(r -> recusados.put(normalizar(r.termo()), r));
		var donos = new HashMap<String, String>();
		competencias.values()
			.forEach(c -> validarTermos("competência " + c.id(), c.termos(), donos, recusados, problemas));
		tecnologias.forEach(t -> validarTermos("tecnologia " + t.id(), t.termos(), donos, recusados, problemas));
	}

	private static void validarTermos(String dono, List<String> termos, Map<String, String> donos,
			Map<String, TermoRecusado> recusados, List<String> problemas) {
		for (String termo : termos) {
			String chave = normalizar(termo);
			TermoRecusado recusado = recusados.get(chave);
			if (recusado != null) {
				problemas.add("termo recusado em uso: " + termo + " (" + recusado.motivo() + ")");
			}
			String donoAnterior = donos.putIfAbsent(chave, dono);
			if (donoAnterior != null && !donoAnterior.equals(dono)) {
				problemas.add("termo \"" + termo + "\" aparece em mais de uma entrada");
			}
		}
	}

	/** Forma usada para comparar termos: sem acentos, minúscula e com espaços simples. */
	static String normalizar(String termo) {
		String semAcentos = Normalizer.normalize(termo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcentos.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
	}

	private record Arquivo(String arquetipo, List<Competencia> competencias, List<TecnologiaNomeada> tecnologias,
			List<TermoRecusado> termosRecusados) {
	}

}
