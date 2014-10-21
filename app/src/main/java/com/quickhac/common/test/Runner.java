package com.quickhac.common.test;
import java.io.Console;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Scanner;

import com.quickhac.common.TEAMSGradeParser;
import com.quickhac.common.TEAMSGradeRetriever;
import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;
import com.quickhac.common.districts.TEAMSUserType;
import com.quickhac.common.districts.impl.AustinISDParent;
import com.quickhac.common.districts.impl.AustinISDStudent;

public class Runner {
	public static void main(String args[]) throws IOException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter a username");
		final String AISDuser = scan.next();
        System.out.println("Enter a password");
        final String AISDpass = scan.next();

        final TEAMSUserType userType;
        if (AISDuser.matches("^s\\d{7}$")) {
            userType = new AustinISDStudent();
        } else {
            userType = new AustinISDParent();
        }

		//////////////////////////////
		
		final TEAMSGradeParser p = new TEAMSGradeParser();
		
		//Get cookies
		final String cstonecookie = TEAMSGradeRetriever.getAustinisdCookie(AISDuser, AISDpass);
		final String teamscookie = TEAMSGradeRetriever.getTEAMSCookie(cstonecookie, userType);
		
		//Generate final cookie
		final String finalcookie = teamscookie + ';' + cstonecookie;
		
		//POST to login to TEAMS
		String userIdentification = TEAMSGradeRetriever.postTEAMSLogin(AISDuser,AISDpass,finalcookie, userType);
		
		//Get "Report Card"
		final String averageHtml = TEAMSGradeRetriever.getTEAMSPage("/selfserve/PSSViewReportCardsAction.do", "", finalcookie, userType, userIdentification);
		final Course[] studentCourses = p.parseAverages(averageHtml);
		ClassGrades c = TEAMSGradeRetriever.getCycleClassGrades(studentCourses[0], 1, averageHtml, finalcookie, userType, userIdentification);
//		/*Logic to get ClassGrades. TEAMS looks for a post request with the "A" tag id of a specific grade selected, 
//		 * so we iterate through all the a tags we got above and send/store the parsed result one by one*/
//		final ArrayList<ClassGrades> classGrades = new ArrayList<ClassGrades>();
//		final Elements avalues = Jsoup.parse(averageHtml).getElementById("finalTablebottomRight1").getElementsByTag("a");
//		for (Element e: avalues) {
//			if (Numeric.isNumeric(e.text())) {
//				String gradeBookKey = "selectedIndexId=-1&smartFormName=SmartForm&gradeBookKey=" + URLEncoder.encode(e.id(), "UTF-8");
//				classGrades.add( p.parseClassGrades(TEAMSGradeRetriever.getTEAMSPage("/selfserve/PSSViewGradeBookEntriesAction.do", gradeBookKey, finalcookie), "", 0, 0));
//			}
//		}
	}
}