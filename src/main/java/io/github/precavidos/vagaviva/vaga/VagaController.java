package io.github.precavidos.vagaviva.vaga;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.vocabulario.Competencia;

@RestController
@RequestMapping("/api/vagas")
class VagaController {

	private final ExtratorDeVaga extrator;

	VagaController(ExtratorDeVaga extrator) {
		this.extrator = extrator;
	}

	@PostMapping("/extracao")
	RespostaDeExtracao extrair(@Valid @RequestBody PedidoDeExtracao pedido) {
		return RespostaDeExtracao.de(extrator.extrair(pedido.texto()));
	}

	record PedidoDeExtracao(@NotBlank @Size(max = ExtratorDeVaga.TAMANHO_MAXIMO) String texto) {
	}

	record RespostaDeExtracao(String arquetipo, List<Nivel> niveis, List<Tecnologia> tecnologias,
			List<CompetenciaEncontrada> competencias) {

		static RespostaDeExtracao de(VagaExtraida vaga) {
			var r = vaga.reconhecimento();
			return new RespostaDeExtracao(vaga.arquetipo(), vaga.niveis(),
					r.tecnologias()
						.stream()
						.map(t -> new Tecnologia(t.tecnologia().id(), t.tecnologia().nome(), t.termosEncontrados()))
						.toList(),
					r.competencias().stream().map(c -> CompetenciaEncontrada.de(c.competencia(), c.evidencias())).toList());
		}

	}

	record Tecnologia(String id, String nome, List<String> termosEncontrados) {
	}

	record CompetenciaEncontrada(String id, String rotulo, Competencia.Origem origem, String escoUri,
			List<String> evidencias) {

		static CompetenciaEncontrada de(Competencia c, List<String> evidencias) {
			return new CompetenciaEncontrada(c.id(), c.rotulo(), c.origem(), c.esco() == null ? null : c.esco().uri(),
					evidencias);
		}

	}

}
