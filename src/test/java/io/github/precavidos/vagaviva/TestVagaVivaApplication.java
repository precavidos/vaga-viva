package io.github.precavidos.vagaviva;

import org.springframework.boot.SpringApplication;

public class TestVagaVivaApplication {

	public static void main(String[] args) {
		SpringApplication.from(VagaVivaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
