package io.github.precavidos.vagaviva.jobposting;

/** Seniority requested by a job posting, from lowest to highest. */
public enum SeniorityLevel {

	INTERN("estágio"), JUNIOR("júnior"), MID("pleno"), SENIOR("sênior");

	private final String label;

	SeniorityLevel(String label) {
		this.label = label;
	}

	/** Portuguese name of the level, shown to students. */
	public String label() {
		return label;
	}

}
