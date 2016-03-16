package com.importsource.pattern.mvc;

/**
 * <b>MVC Pattern</b> stands for Model-View-Controller Pattern. This pattern is used to
 * separate application's concerns.
 * <p>
 * <b>Model</b> - Model represents an object or JAVA POJO carrying data. It can also
 * have logic to update controller if its data changes.
 * <p>
 * <b>View</b> - View represents the visualization of the data that model contains.
 * <p>
 * <b>Controller</b> - Controller acts on both model and view. It controls the data
 * flow into model object and updates the view whenever data changes. It keeps
 * view and model separate.
 * 
 * @author Hezf
 *
 */
public class MVCPatternDemo {
	public static void main(String[] args) {

		// fetch student record based on his roll no from the database
		Student model = retriveStudentFromDatabase();

		// Create a view : to write student details on console
		StudentView view = new StudentView();

		StudentController controller = new StudentController(model, view);

		controller.updateView();

		// update model data
		controller.setStudentName("John");

		controller.updateView();
	}

	private static Student retriveStudentFromDatabase() {
		Student student = new Student();
		student.setName("Robert");
		student.setRollNo("10");
		return student;
	}
}