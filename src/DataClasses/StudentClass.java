package DataClasses;

public class StudentClass {
	private String classname;
	private String teacher;
	private Cycle[] Cycles;
	public String getClassname() {
		return classname;
	}
	public void setClassname(String classname) {
		this.classname = classname;
	}
	public String getTeacher() {
		return teacher;
	}
	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}
	public Cycle[] getCycles() {
		return Cycles;
	}
	public void setCycles(Cycle[] cycles) {
		Cycles = cycles;
	}
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	private String period;
	public StudentClass(String teacher2, String studentClass, String period,
			Object object) {
		teacher = teacher2;
		classname = studentClass;
		this.period = period;
	}
	
}
