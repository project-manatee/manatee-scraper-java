package com.quickhac.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.net.ssl.SSLSocketFactory;

import org.jsoup.nodes.Element;

import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;
import com.quickhac.common.districts.TEAMSUserType;

public class TEAMSGradeRetriever {
    final public static String LOGIN_ERR = null;
	public static String getAustinisdCookie(final String AISDuser,
			final String AISDpass) throws  IOException {
		final String query = "cn=" + URLEncoder.encode(AISDuser,"UTF-8") + "&%5Bpassword%5D=" + URLEncoder.encode(AISDpass,"UTF-8");
		
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

	public static String getTEAMSCookie(final String CStoneCookie, final TEAMSUserType userType)
			throws  IOException {
		final String query = "";
        final String response = postPageHTTPS(userType.teamsHost(), "/selfserve/EntryPointSignOnAction.do?parent=" + userType.isParent(), new String[]{
                "Cookie: " + CStoneCookie,
                "Accept: */*",
                "User-Agent: Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
        }, query);
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

	public static String postTEAMSLogin(final String user,
			final String pass, final String studentId,final String cookies, final TEAMSUserType userType)
			throws  IOException {
        final String query = "userLoginId=" + URLEncoder.encode(user,"UTF-8") + "&userPassword=" + URLEncoder.encode(pass,"UTF-8");

        final String response = postPageHTTPS(userType.teamsHost(), "/selfserve/SignOnLoginAction.do", new String[]{
                "Cookie: " + cookies,
                "Accept: */*",
                "User-Agent: QHAC"
        }, query);

        if (userType.isParent()) {
            TEAMSGradeParser parser = new TEAMSGradeParser();
            try{
                String chooseUser = getTEAMSPage("/selfserve/ViewStudentListAction.do","",cookies,userType,"");
                final int idIndex = parser.parseStudentInfoIndex(studentId,chooseUser);
                String studentInfoLocID = "";
                if(idIndex != -1){
                    studentInfoLocID = parser.parseStudentInfoLocID(idIndex,chooseUser);
                }
                else{
                    System.out.println("No student found with supplied student id!");
                    return LOGIN_ERR;
                }
                //TODO Hardcoded user index 0 for now
                postPageHTTPS(userType.teamsHost(), "/selfserve/ViewStudentListChangeTabDisplayAction.do", new String[]{
                        "Cookie: " + cookies,
                        "Accept: */*",
                        "User-Agent: QHAC"
                }, "selectedIndexId=" + idIndex + "&studentLocId=" + studentInfoLocID + "&selectedTable=table");
                return "&selectedIndexId=" + idIndex + "&studentLocId=" + studentInfoLocID + "&selectedTable=table";
            }
            catch (Exception e){
                return LOGIN_ERR;
            }

        } else {
            return "";
        }
	}
	public static ClassGrades getCycleClassGrades(Course course,int cycle, String averagesHtml,String cookies, final TEAMSUserType userType, String userIdentification) throws  IOException{
		TEAMSGradeParser parser = new TEAMSGradeParser();
		Element coursehtmlnode = parser.getCourseElement(averagesHtml,course,cycle);
		String gradeBookKey = "selectedIndexId=-1&smartFormName=SmartForm&gradeBookKey=" + URLEncoder.encode(coursehtmlnode.getElementsByTag("a").get(0).id(), "UTF-8");
		String coursehtml = getTEAMSPage("/selfserve/PSSViewGradeBookEntriesAction.do", gradeBookKey, cookies, userType, userIdentification);
		//TODO hardcoded number of cycles
		return parser.parseClassGrades(coursehtml, course.courseId, cycle < 3 ? 0:1  , cycle);
	}
	public static String getTEAMSPage(final String path,
			final String gradeBookKey, String cookie, final TEAMSUserType userType, String userIdentification) throws IOException {
        //return getPageHTTPS(userType.teamsHost(), path, cookie);
//        postPageHTTPS(userType.teamsHost(), path, new String[]{
//                "Cookie: " + cookie,
//        }, gradeBookKey + userIdentification);

        return postPageHTTPSNew(userType.teamsHost(), path, new String[][]{{"Cookie",cookie}},gradeBookKey+userIdentification);
    }
	public static String getPageHTTPS(final String host, final String path, final String finalcookie){
        URL url = null;
        try {
            url = new URL("https://" + host+path);
            URLConnection conn = url.openConnection();

            // Set the cookie value to send
            conn.setRequestProperty("Cookie", finalcookie);
            // Send the request to the server
            conn.connect();
            InputStream is = conn.getInputStream();
            String parsedString = convertinputStreamToString(is);
            return parsedString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String postPageHTTPSNew(final String host, final String path, final String[][] headers, final String post){
        try {
            byte[] postData = post.getBytes(Charset.forName("UTF-8"));
            int postDataLength = postData.length;
            String type = "application/x-www-form-urlencoded";
            String request = "https://" + host+path;
            URL url = new URL(request);

            HttpURLConnection  conn = (HttpURLConnection) url.openConnection();
            for (String[] s: headers){
                conn.setRequestProperty(s[0], s[1]);
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);
            try{
                DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
                wr.write( postData );
            }
            catch (Exception e){}
            Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            final char[] buffer = new char[1024];
            int len = 0;
            while ((len = reader.read(buffer)) > 0) {
                response.append(buffer, 0, len);
                if (response.length() >= 4 && response.substring(response.length() - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
    public static String convertinputStreamToString(InputStream ists)
            throws IOException {
        if (ists != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader r1 = new BufferedReader(new InputStreamReader(
                        ists, "UTF-8"));
                while ((line = r1.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                ists.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}