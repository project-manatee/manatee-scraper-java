package com.quickhac.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;

import javax.net.ssl.SSLSocketFactory;

import org.jsoup.nodes.Element;

import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;

public class TEAMSGradeRetriever {
    final static String LOGIN_ERR = "-1";
    final static String studentLogin = "^s\\d{7}$";
	public static String getAustinisdCookie(final String AISDuser,
			final String AISDpass) throws  IOException {
		final String query = "cn=" + AISDuser + "&%5Bpassword%5D=" + AISDpass;
		
		final String response = postPageHTTPS("my.austinisd.org", "/WebNetworkAuth/", new String[]{
				"User-Agent: QHAC",
				"Accept: */*"
		}, query);
		
		String cstonecookie = null;

		for (String line : response.split("\n")) {
			if (line.startsWith("Set-Cookie: CStoneSessionID=")) {
				cstonecookie = line.substring(12);
			}
		}

		if (cstonecookie == null) {
			System.out.println("No cookie received!");
			return LOGIN_ERR;
		}

		System.out.println(cstonecookie);
		// Split on first semicolon
		return cstonecookie.split(";")[0];
	}

	public static String getTEAMSCookie(final String CStoneCookie, final String AISDuser)
			throws  IOException {
		final String query = "";
        final String response;
        if (AISDuser.matches(studentLogin)) {
            response = postPageHTTPS("my-teams.austinisd.org", "/selfserve/EntryPointSignOnAction.do?parent=false", new String[]{
                    "Cookie: " + CStoneCookie,
                    "Accept: */*",
                    "User-Agent: QHAC"
            }, query);
        }
        else{
            response = postPageHTTPS("my-teamsselfserve.austinisd.org", "/selfserve/EntryPointSignOnAction.do?parent=true", new String[]{
                    "Cookie: " + CStoneCookie,
                    "Accept: */*",
                    "User-Agent: QHAC"
            }, query);
        }
		String jcookie = null;

		for (String line : response.split("\n")) {
			if (line.startsWith("Set-Cookie: JSESSIONID=")) {
				jcookie = line.substring(12);
			}
		}

		if (jcookie == null) {
			System.out.println("No cookie received!");
			return LOGIN_ERR;
		}
		System.out.println(jcookie);
		// Split on first semicolon
		return jcookie.split(";")[0];
	}

	public static String postTEAMSLogin(final String AISDuser,
			final String AISDpass, final String cookies)
			throws  IOException {
        final String query = "userLoginId=" + AISDuser + "&userPassword=" + AISDpass;
        if (AISDuser.matches(studentLogin)) {
            postPageHTTPS("my-teams.austinisd.org", "/selfserve/SignOnLoginAction.do", new String[]{
                    "Cookie: " + cookies,
                    "Accept: */*",
                    "User-Agent: QHAC"
            }, query);
            return "";
        }
        else{
            String toParse = postPageHTTPS("my-teamsselfserve.austinisd.org", "/selfserve/SignOnLoginAction.do", new String[]{
                    "Cookie: " + cookies,
                    "Accept: */*",
                    "User-Agent: QHAC"
            }, query);
            TEAMSGradeParser parser = new TEAMSGradeParser();
            //TODO Hardcoded user index 0 for now
            postPageHTTPS("my-teamsselfserve.austinisd.org", "/selfserve/ViewStudentListChangeTabDisplayAction.do", new String[]{
                    "Cookie: " + cookies,
                    "Accept: */*",
                    "User-Agent: QHAC"
            }, "selectedIndexId=0&studentLocId="+ parser.parseStudentInfoLocID(toParse) + "&selectedTable=table");
            return "&selectedIndexId=0&studentLocId="+ parser.parseStudentInfoLocID(toParse) + "&selectedTable=table";
        }
	}
	public static ClassGrades getCycleClassGrades(Course course,int cycle, String averagesHtml,String cookies,String AISDuser, String userIdentification) throws  IOException{
		TEAMSGradeParser parser = new TEAMSGradeParser();
		Element coursehtmlnode = parser.getCourseElement(averagesHtml,course,cycle);
		String gradeBookKey = "selectedIndexId=-1&smartFormName=SmartForm&gradeBookKey=" + URLEncoder.encode(coursehtmlnode.getElementsByTag("a").first().id(), "UTF-8");
		String coursehtml = getTEAMSPage("/selfserve/PSSViewGradeBookEntriesAction.do", gradeBookKey, cookies,AISDuser, userIdentification);
		//TODO hardcoded number of cycles
		return parser.parseClassGrades(coursehtml, course.courseId, cycle < 3 ? 0:1  , cycle);
	}
	public static String getTEAMSPage(final String path,
			final String gradeBookKey, String cookie,String AISDuser, String userIdentification) throws IOException {
        if (AISDuser.matches(studentLogin)) {

            return postPageHTTPS("my-teams.austinisd.org", path, new String[]{
                    "Cookie: " + cookie,
            }, gradeBookKey);
        }
        else{
            return postPageHTTPS("my-teamsselfserve.austinisd.org", path, new String[]{
                    "Cookie: " + cookie,
            }, gradeBookKey + userIdentification);
        }
    }
	
	public static String postPageHTTPS(final String host, final String path, final String[] headers, final String postData) throws  IOException {
		final Socket socket = SSLSocketFactory.getDefault().createSocket(host,
				443);
		try {
			final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					socket.getOutputStream()));
			writer.println("POST " + path + " HTTP/1.1");
			writer.println("Host: " + host);
			for (String header : headers) {
				writer.println(header);
			}
			writer.println("Content-Length: " + postData.length());
			writer.println("Content-Type: application/x-www-form-urlencoded");
			writer.println();
			writer.println(postData);
			writer.println();
			writer.flush();
	
			StringBuilder response = new StringBuilder();
	
			final BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			final char[] buffer = new char[1024];
			int len = 0;
			while ((len = reader.read(buffer)) > 0) {
				response.append(buffer, 0, len);
				if (response.length() >= 4 && response.substring(response.length() - 4).equals("\r\n\r\n")) {
					break;
				}
			}
	
			return response.toString();
		} finally {
			socket.close();
		}
		
	}
}