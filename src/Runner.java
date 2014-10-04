import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import DataClasses.Student;
import DataClasses.StudentClass;


public class Runner {

	public static void main(String[] args) {
		// Prompt user for username and password
		final JLabel usernameLabel = new JLabel("Username:");
		final JTextField usernameField = new JTextField();
		final JLabel passwordLabel = new JLabel("Password:");
		final JPasswordField passwordField = new JPasswordField();
		JOptionPane.showConfirmDialog(null, new Object[] { usernameLabel,
				usernameField, passwordLabel, passwordField }, "Login",
				JOptionPane.OK_CANCEL_OPTION);

		final String AISDuser = usernameField.getText();
		final String AISDpass = new String(passwordField.getPassword());
		String html = GradeRetriever.getGradesPage(AISDuser, AISDpass);
		Student s = GradeParser.parse(html);
		for (StudentClass c: s.getClasses()){
			System.out.println("You are in " + c.getClassname() + " with teacher " + c.getTeacher());
		}
	}

}
