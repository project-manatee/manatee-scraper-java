package DataClasses;

public class Student {
	String name;
	private StudentClass[] classes;
	public Student(String string, StudentClass[] studentClasses) {
		name = string;
		classes = studentClasses;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public StudentClass[] getClasses() {
		return classes;
	}
	public void setClasses(StudentClass[] classes) {
		this.classes = classes;
	}
}
