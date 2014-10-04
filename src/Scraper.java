import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class Scraper {

	public static void main(String[] args) {
		try{
				// Prompt user for username and password
				final JLabel usernameLabel = new JLabel("Username:");
				final JTextField usernameField = new JTextField();
				final JLabel passwordLabel = new JLabel("Password:");
				final JPasswordField passwordField = new JPasswordField();
				JOptionPane.showConfirmDialog(null,
				  new Object[]{usernameLabel, usernameField, passwordLabel, passwordField}, "Login",
				  JOptionPane.OK_CANCEL_OPTION);
				
				final String AISDuser = usernameField.getText();
				final String AISDpass = new String(passwordField.getPassword());
				
			   // Extract jSessionId from startURL for Cookies
				final WebClient webClient = new WebClient(BrowserVersion.CHROME);
				WebClientOptions config = webClient.getOptions();
				config.setThrowExceptionOnScriptError(false);
				config.setCssEnabled(false);
				cookieSet(webClient, AISDuser, AISDpass);
				
				// Get Teams Login Page
                HtmlPage page = webClient.getPage("https://my-teams.austinisd.org/selfserve/EntryPointSignOnAction.do?parent=false");
			    
                // Get and fill out form
                HtmlForm form = page.getFormByName("SmartForm");
			    HtmlTextInput username = form.getInputByName("userLoginId");
			    HtmlPasswordInput password = form.getInputByName("userPassword");
			    username.setValueAttribute(AISDuser);
			    password.setValueAttribute(AISDpass);
			    
			    // Submit form
			    String javaScriptCode = "var form = (document.forms['SmartForm']); form.action = \"https://my-teams.austinisd.org/selfserve/SignOnLoginAction.do?parent=false\"; form.submit();";
			    page.executeJavaScript(javaScriptCode);
			    
			    // Load Teams
			    final HtmlPage page3 = webClient.getPage("https://my-teams.austinisd.org/selfserve/PSSViewReportCardsAction.do");
			    System.out.println(page3.asText());
			 
			} catch (IOException e) {
			   e.printStackTrace();
			}


	

	}
	private static void cookieSet(final WebClient webClient, final String AISDuser, final String AISDpass) {
        try {
			HtmlPage page = webClient.getPage("https://my.austinisd.org/");
			HtmlForm form = page.getFormByName("loginForm");
		    HtmlTextInput username = form.getInputByName("cn");
		    HtmlPasswordInput password = form.getInputByName("[password]");
		    username.setValueAttribute(AISDuser);
		    password.setValueAttribute(AISDpass);
		    String javaScriptCode = "document.forms['loginForm'].submit();";
		    page.executeJavaScript(javaScriptCode);
		    
		    boolean failure = true;
		    for (final Cookie cookie: webClient.getCookieManager().getCookies()) {
		    	if (cookie.getName().equalsIgnoreCase("CSTONESESSIONID")) {
		    		failure = false;
		    	}
		    }
		    if (failure) {
		    	System.out.println("Incorrect username/password.");
		    	System.exit(1);
		    }
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

