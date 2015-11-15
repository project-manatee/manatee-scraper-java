package com.manateams.scraper.test;
import java.io.IOException;
import java.util.Scanner;

import com.manateams.scraper.TEAMSGradeParser;
import com.manateams.scraper.TEAMSGradeRetriever;
import com.manateams.scraper.data.ClassGrades;
import com.manateams.scraper.data.Course;
import com.manateams.scraper.districts.TEAMSUserType;
import com.manateams.scraper.districts.impl.AustinISDParent;
import com.manateams.scraper.districts.impl.AustinISDStudent;

public class Runner {
	public static void main(String args[]) throws IOException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter a username");
		final String AISDuser = scan.next();
        System.out.println("Enter a password");
        final String AISDpass = scan.next();
        System.out.println("Enter a student id");
        final String AISDstudentid = scan.next();

        final TEAMSGradeRetriever retriever = new TEAMSGradeRetriever();
        final TEAMSGradeParser parser = new TEAMSGradeParser();


        final TEAMSUserType userType = retriever.getUserType(AISDuser);
        final String cookie = retriever.getNewCookie(AISDuser, AISDpass, userType);
        final String userIdentification = retriever.postTEAMSLogin(AISDuser, AISDpass, AISDstudentid, cookie, userType);
        final String averageHtml = retriever.getTEAMSPage("/selfserve/PSSViewReportCardsAction.do", "", cookie, userType, userIdentification);
        final Course[] courses = parser.parseAverages(averageHtml);
        System.out.println(courses[0].title);
        ClassGrades c = retriever.getCycleClassGrades(courses[0], 0, averageHtml, cookie, userType, userIdentification);
        System.out.println(c.average);
	}
}