package com.quickhac.common.test;
import java.io.IOException;
import java.net.URLEncoder;

import com.quickhac.common.TEAMSGradeParser;
import com.quickhac.common.TEAMSGradeRetriever;
import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;

public class Runner {
	public static void main(String args[]) throws IOException {
		final String AISDuser =  "";
		final String AISDpass =  "";
		
		//////////////////////////////
		
		final TEAMSGradeParser p = new TEAMSGradeParser();
		
		//Get cookies
		final String cstonecookie = TEAMSGradeRetriever.getAustinisdCookie(AISDuser, AISDpass);
		final String teamscookie = TEAMSGradeRetriever.getTEAMSCookie(cstonecookie);
		
		//Generate final cookie
		final String finalcookie = teamscookie + ';' + cstonecookie;
		
		//POST to login to TEAMS
		TEAMSGradeRetriever.postTEAMSLogin(AISDuser,AISDpass,finalcookie);
		
		//Get "Report Card"
		final String averageHtml = TEAMSGradeRetriever.getTEAMSPage("/selfserve/PSSViewReportCardsAction.do", "", finalcookie);
		final Course[] studentCourses = p.parseAverages(averageHtml);
		ClassGrades c = TEAMSGradeRetriever.getCycleClassGrades(studentCourses[0], 1, averageHtml, finalcookie);
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