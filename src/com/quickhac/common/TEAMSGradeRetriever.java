package com.quickhac.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocketFactory;

public class TEAMSGradeRetriever {
	public static String getAustinisdCookie(final String AISDuser,
			final String AISDpass) throws UnknownHostException, IOException {
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
			System.exit(1);
		}

		System.out.println(cstonecookie);
		// Split on first semicolon
		return cstonecookie.split(";")[0];
	}

	public static String getTEAMSCookie(final String CStoneCookie)
			throws UnknownHostException, IOException {
		final String query = "";
		final String response = postPageHTTPS("my-teams.austinisd.org", "/selfserve/EntryPointSignOnAction.do?parent=false", new String[]{
				"Cookie: " + CStoneCookie,
				"Accept: */*",
				"User-Agent: QHAC"
		}, query);
		String jcookie = null;

		for (String line : response.split("\n")) {
			if (line.startsWith("Set-Cookie: JSESSIONID=")) {
				jcookie = line.substring(12);
			}
		}

		if (jcookie == null) {
			System.out.println("No cookie received!");
			System.exit(1);
		}
		System.out.println(jcookie);
		// Split on first semicolon
		return jcookie.split(";")[0];
	}

	public static void postTEAMSLogin(final String AISDuser,
			final String AISDpass, final String CStoneCookie)
			throws UnknownHostException, IOException {
		final String query = "userLoginId=" + AISDuser + "&userPassword=" + AISDpass;
		postPageHTTPS("my-teams.austinisd.org", "/selfserve/SignOnLoginAction.do", new String[]{
				"Cookie: " + CStoneCookie,
				"Accept: */*",
				"User-Agent: QHAC"
		}, query);
	}
	
	public static String getTEAMSPage(final String path,
			final String gradeBookKey, String cookie) throws UnknownHostException, IOException {
		return postPageHTTPS("my-teams.austinisd.org", path, new String[]{
				"Cookie: " + cookie,
		}, gradeBookKey);
	}
	
	public static String postPageHTTPS(final String host, final String path, final String[] headers, final String postData) throws UnknownHostException, IOException {
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