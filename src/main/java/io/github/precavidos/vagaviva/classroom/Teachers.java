package io.github.precavidos.vagaviva.classroom;

import org.springframework.stereotype.Component;

/** Who teaches: a person teaches once they own a classroom. */
@Component
public class Teachers {

	private final ClassroomRepository classrooms;

	Teachers(ClassroomRepository classrooms) {
		this.classrooms = classrooms;
	}

	public boolean teaches(String login) {
		return classrooms.existsByTeacherLogin(login);
	}

}
