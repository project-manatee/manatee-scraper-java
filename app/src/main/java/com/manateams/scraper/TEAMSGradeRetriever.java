package com.manateams.scraper;

import android.support.annotation.Nullable;

import com.manateams.scraper.data.ClassGrades;
import com.manateams.scraper.data.Course;
import com.manateams.scraper.districts.TEAMSUserType;
import com.manateams.scraper.districts.impl.AustinISDParent;
import com.manateams.scraper.districts.impl.AustinISDStudent;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;


public class TEAMSGradeRetriever {


    private final String REGEX_USER_TYPE = "^[sS]\\d{6,8}\\d?$";
    private final TEAMSGradeParser parser;

    public TEAMSGradeRetriever() {
        parser = new TEAMSGradeParser();
    }

    public TEAMSUserType getUserType(final String username) {
        if(username.matches(REGEX_USER_TYPE)) {
            return new AustinISDStudent();
        } else {
            return new AustinISDParent();
        }
    }

    @Nullable
    public String getNewCookie(final String username, final String password, final TEAMSUserType userType) {
        try {
            final String cStoneCookie = getAISDCookie(username, password);
            final String TEAMSCookie = getTEAMSCookie(cStoneCookie, userType);
            return cStoneCookie + ';' + TEAMSCookie;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public String getNewUserIdentification(final String username, final String password, final String studentID, final String teamsUser, final String teamsPassword, final String cookie, final TEAMSUserType userType) {
        try {
            if (teamsUser != null && teamsUser.length() > 0) {
                return postTEAMSLogin(teamsUser, teamsPassword, studentID, cookie, userType);
            } else {
                return postTEAMSLogin(username, password, studentID, cookie, userType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ClassGrades getCycleClassGrades(Course course, int cycle, String cookie, final TEAMSUserType userType, String userIdentification) throws  IOException{
        final TEAMSGradeParser parser = new TEAMSGradeParser();
        final String averageHtml = getTEAMSPage("/selfserve/PSSViewReportCardsAction.do", "", cookie, userType, userIdentification);

        final Element coursehtmlnode = parser.getCourseElement(averageHtml,course,cycle);
        final String gradeBookKey = "selectedIndexId=-1&smartFormName=SmartForm&gradeBookKey=" + URLEncoder.encode(coursehtmlnode.getElementsByTag("a").get(0).id(), "UTF-8");
        final String coursehtml = getTEAMSPage("/selfserve/PSSViewGradeBookEntriesAction.do", gradeBookKey, cookie, userType, userIdentification);
        //TODO hardcoded number of cycles
        return parser.parseClassGrades(coursehtml, course.courseId, cycle < 3 ? 0 : 1, cycle);
    }

    public String getTEAMSPage(final String path, final String gradeBookKey, final String cookie, final TEAMSUserType userType, final String userIdentification) throws IOException {
        final HashMap<String, String> data = new HashMap<>();
        data.put("Cookie", cookie);
        return doPOSTRequest("https://" + userType.teamsHost() + path, data, gradeBookKey + userIdentification);
    }

    /*
    Returns a new set of user information if user is a parent account.
     */
    public String postTEAMSLogin(final String username, final String password, final String studentID, final String cookie, final TEAMSUserType userType) throws IOException {
        final String query = "userLoginId=" + URLEncoder.encode(username, "UTF-8") + "&userPassword=" + URLEncoder.encode(password, "UTF-8");

        final String[] headers = new String[]{
                "Cookie: " + cookie,
                "Accept: */*",
                "User-Agent: QHAC"
        };

        doRawPOSTRequest(userType.teamsHost(), "/selfserve/SignOnLoginAction.do", headers, query);

        if (userType.isParent()) {
            try {
                String chooseUser = getTEAMSPage("/selfserve/ViewStudentListAction.do", "", cookie, userType, "");
                final int idIndex = parser.parseStudentInfoIndex(studentID, chooseUser);
                String studentInfoLocID = "";
                if (idIndex != -1) {
                    studentInfoLocID = parser.parseStudentInfoLocID(idIndex, chooseUser);
                } else {
                    return null;
                }
                //TODO Hardcoded user index 0 for now
                doRawPOSTRequest(userType.teamsHost(), "/selfserve/ViewStudentListChangeTabDisplayAction.do", new String[]{
                        "Cookie: " + cookie,
                        "Accept: */*",
                        "User-Agent: QHAC"
                }, "selectedIndexId=" + idIndex + "&studentLocId=" + studentInfoLocID + "&selectedTable=table");
                return "&selectedIndexId=" + idIndex + "&studentLocId=" + studentInfoLocID + "&selectedTable=table";
            } catch (IOException e) {
                return null;
            }
        } else {
            return "";
        }
    }

    private String getAISDCookie(final String username, final String password) throws IOException {
        final String rawQuery = "cn=" + username + "&[password]=" + password;
        final String query = URLEncoder.encode(rawQuery, "UTF-8");

        final String[] headers = new String[]{
                "User-Agent: QHAC",
                "Accept: */*"
        };

        final String response = doRawPOSTRequest("my.austinisd.org", "/WebNetworkAuth/", headers, query);

        for (final String line : response.split("\n")) {
            if (line.startsWith("Set-Cookie: CStoneSessionID=")) {
                return line.substring(12).split(";")[0];
            }
        }

        return null;
    }

    private String getTEAMSCookie(final String AISDCookie, final TEAMSUserType userType) throws IOException {
        final String[] headers = new String[]{
                "Cookie: " + AISDCookie,
                "Accept: */*",
                "User-Agent: Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
        };

        final String response = doRawPOSTRequest(userType.teamsHost(), "/selfserve/EntryPointSignOnAction.do?parent=" + userType.isParent(), headers, "");

        for (final String line : response.split("\n")) {
            if (line.startsWith("Set-Cookie: JSESSIONID=")) {
                return line.substring(12).split(";")[0];
            }
        }

        return null;
    }

    private String doPOSTRequest(final String url, final HashMap<String, String> headers, final String data) {
        final OkHttpClient client = new OkHttpClient();
        final MediaType type = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Cookie", headers.get("Cookie"))
                .post(RequestBody.create(type, data))
                .build();

        String responseString = null;
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            responseString = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString;
    }

    private String doRawPOSTRequest(final String host, final String path, final String[] headers, final String postData) throws IOException {
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
            int len;
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
