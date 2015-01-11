package com.quickhac.common;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.quickhac.common.data.Assignment;
import com.quickhac.common.data.Category;
import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;
import com.quickhac.common.data.Cycle;
import com.quickhac.common.data.GradeValue;
import com.quickhac.common.data.Semester;
import com.quickhac.common.util.Hash;
import com.quickhac.common.util.Numeric;

public class TEAMSGradeParser {

	/**
	 * The regex to compare against assignment titles to guess if it's extra
	 * credit or not.
	 */
	static final Pattern EXTRA_CREDIT_REGEX = Pattern.compile(
			"^extra credit$|^ec$", Pattern.CASE_INSENSITIVE);

	/**
	 * The regex to compare against assignment notes to guess if it's extra
	 * credit or not.
	 */
	static final Pattern EXTRA_CREDIT_NOTE_REGEX = Pattern.compile(
			"extra credit", Pattern.CASE_INSENSITIVE);

	static final Pattern NUMERIC_REGEX = Pattern.compile("(\\d+)");
	static final Pattern CYCLE_HEADER_REGEX = Pattern.compile("cycle (\\d+)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern EXAM_HEADER_REGEX = Pattern.compile("exam (\\d+)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern SEMESTER_HEADER_REGEX = Pattern.compile("sem (\\d+)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern GRADE_CELL_URL_REGEX = Pattern
			.compile("\\?data=([\\w\\d%]*)");

	static final Pattern CLASS_NAME_REGEX = Pattern
			.compile("(.*) \\(Period (\\d+)\\)");
	static final Pattern CATEGORY_NAME_REGEX = Pattern
			.compile("^(.*) - (\\d+)%$");
	static final Pattern ALT_CATEGORY_NAME_REGEX = // IB-MVPS grading
	Pattern.compile("^(.*) - Each assignment counts (\\d+)");


	public Course[] parseAverages(final String html) {
		// set up DOM for parsing
		final Document doc = Jsoup.parse(html);

		// Define Grade/Metadata Table
		final Element $metadataTable = doc.getElementById("finalTablebotLeft1")
				.getElementById("tableHeaderTable");
		final Elements $metadataRows = $metadataTable.getElementsByTag("tr");
		final Element $gradeTable = doc
				.getElementById("finalTablebottomRight1").getElementById(
						"tableHeaderTable");
		final Elements $gradeRows = $gradeTable.getElementsByTag("tr");
		// make semester parameters
		final SemesterParams semParams = new SemesterParams();

		//TODO These semester params are explicitly set for TEAMS
		semParams.cyclesPerSemester = 3;
		semParams.hasExams = true;
		semParams.hasSemesterAverages = true;
		semParams.semesters = 2;

		final Course[] courses;
		courses = new Course[$gradeRows.size() - 1];

		// parse each course (ignore the first row as it is headers)
		for (int i = 1; i < $metadataRows.size(); i++) {
			courses[i - 1] = parseCourse($metadataRows.get(i),
					$gradeRows.get(i), semParams);
		}

		return courses;
	}

	public ClassGrades parseClassGrades(final String html,
			final String courseId, final int semesterIndex, final int cycleIndex) {
		// set up DOM for parsing
		final Document doc = Jsoup.parse(html);

		// get categories
		final Element $categoriesDiv = doc
				.getElementById("pssViewGradeBookEntriesDiv");
		final Elements $categories = $categoriesDiv.children();
		//Split <br> for category info later
		$categories.select("br").append("split");

        // parse categories
        final ArrayList<Category> cats = new ArrayList<Category>();
        for (int i = 0; i < $categories.size(); i++)
            cats.add( parseCategory(
                    $categories.get(i).getElementsByTag("div").first(),
                    courseId));
        for (int i = 0; i < cats.size();i++){
            Category c = cats.get(i);
            if(c == null || c.assignments == null){
                cats.remove(c);
                i--;
            }
        }
        Category catfinal[] = new Category[cats.size()];
        catfinal = cats.toArray(catfinal);
        //Workaround if $gradeinfo not returned
        Elements $gradeInfo = null;
        if(doc.getElementsByClass("studentAttendance") != null && doc.getElementsByClass("studentAttendance")
                .first() != null && doc.getElementsByClass("studentAttendance")
                .first().getElementsByTag("tr").size() > 2) {
            $gradeInfo = doc.getElementsByClass("studentAttendance")
                    .first().getElementsByTag("tr").get(2).getElementsByTag("td");
            // parse category average
            if($gradeInfo != null && $gradeInfo.size() < 4){
                //No grades yet
                return null;
            }
            final Matcher averageMatcher = NUMERIC_REGEX.matcher($gradeInfo.get(3)
                    .text());
            boolean averageMatchSuccess = averageMatcher.find();
            // parse class period
            final Matcher periodMatcher = NUMERIC_REGEX.matcher($gradeInfo.get(1)
                    .text());
            periodMatcher.find();
            // return class grades
            final ClassGrades grades = new ClassGrades();
            // Get name from CLASS ID - Name format
            grades.title = $gradeInfo.get(0).text().split("-")[1].trim();
            grades.urlHash = "";
            grades.period = Integer.valueOf(periodMatcher.group(0));
            grades.semesterIndex = semesterIndex;
            grades.cycleIndex = cycleIndex;
            //If we can't find an average, set it to -1
            if (!averageMatchSuccess)
                grades.average = -1;
            else
                grades.average = Integer.valueOf(averageMatcher.group(0));
            grades.categories = catfinal;
            return grades;
        }
        else{
            final ClassGrades grades = new ClassGrades();
            grades.title = "Default";
            grades.urlHash = "";
            grades.period = -1;
            grades.semesterIndex = semesterIndex;
            grades.cycleIndex = cycleIndex;
            grades.average = -1;
            grades.categories = catfinal;
            return grades;
        }
	}

	Course parseCourse(final Element $metadataRow, Element $gradeRow,
			final SemesterParams semParams) {
		// find the cells in this row
		final Elements $metadataCells = $metadataRow.getElementsByTag("td");
		final Elements $gradeCells = $gradeRow.getElementsByTag("td");

		// find the teacher name and email
		final Element $teacherCell = $metadataCells.get(2);

		// get the course number
		final String courseId = $metadataCells.get(0).text();

		// parse semesters
		final Semester[] semesters = new Semester[semParams.semesters];
		for (int i = 0; i < semParams.semesters; i++) {
			// get cells for the semester
			final Element[] $semesterCells = new Element[semParams.cyclesPerSemester];
			final int cellOffset = i
					* (semParams.cyclesPerSemester
							+ (semParams.hasExams ? 1 : 0) + (semParams.hasSemesterAverages ? 1
								: 0));

			// find the cycle cells for this semester
			for (int j = 0; j < $semesterCells.length; j++)
				$semesterCells[j] = $gradeCells.get(cellOffset + j);

			// exam cell is after that
			final Element $examCell = semParams.hasExams ? $gradeCells
					.get(cellOffset + semParams.cyclesPerSemester) : null;

			// and semester cell is after that
			final Element $semAvgCell = semParams.hasSemesterAverages ? $gradeCells
					.get(cellOffset + semParams.cyclesPerSemester
							+ (semParams.hasExams ? 1 : 0)) : null;

			// parse the semester
			semesters[i] = parseSemester($semesterCells, $examCell,
					$semAvgCell, i, semParams);
		}

		// create the course
		final Course course = new Course();
		course.title = $metadataCells.get(3).text();
		course.teacherName = $teacherCell.text();
		course.teacherEmail = "";
		course.courseId = courseId;
		course.semesters = semesters;
		return course;
	}

	Semester parseSemester(Element[] $cycles, Element $exam, Element $semAvg,
			int index, SemesterParams semParams) {
		// parse cycles
		final Cycle[] cycles = new Cycle[semParams.cyclesPerSemester];
		for (int i = 0; i < semParams.cyclesPerSemester; i++)
			cycles[i] = parseCycle($cycles[i], i);

		// parse exam grade
		GradeValue examGrade = new GradeValue();
		if (semParams.hasExams) {
			final String examText = $exam.getElementsByTag("a").first().text();
			if (examText.equals("EX") || examText.equals("Exc")) {
				examGrade.type = GradeValue.TYPE_NONE;
				examGrade.value = GradeValue.VALUE_EXEMPT;
			} else if (examText.matches("\\d+"))
				examGrade = new GradeValue($exam.text());
		} else {
			examGrade.type = GradeValue.TYPE_NONE;
			examGrade.value = GradeValue.VALUE_NOT_APPLICABLE;
		}

		// return a semester
		final Semester semester = new Semester();
		semester.index = index;
		semester.examGrade = examGrade;
		semester.cycles = cycles;

		// calculate our own semester average unless using letter grades
		if (semParams.hasSemesterAverages) {
			GradeValue parsedSemAvg = new GradeValue($semAvg.text());
			if (parsedSemAvg.type == GradeValue.TYPE_LETTER)
				semester.average = parsedSemAvg;
			else
				semester.average = GradeCalc.semesterAverage(semester, 25);
		} else {
			GradeValue semAvg = new GradeValue();
			semAvg.type = GradeValue.TYPE_NONE;
			semAvg.value = GradeValue.VALUE_NOT_APPLICABLE;
		}

		return semester;
	}

	Cycle parseCycle(Element $cell, int index) {
		// Get link
		final Elements $link = $cell.getElementsByTag("a");
		boolean isNumber;
		try {
			Integer.parseInt($link.text());
			isNumber = true;
		} catch (NumberFormatException e) {
			isNumber = false;
		}
		// if there is no link, the cell is empty; return empty values
		if (!isNumber) {
			final Cycle cycle = new Cycle();
			cycle.index = index;
			return cycle;
		}

		// find a grade
		final GradeValue average = new GradeValue($link.text());
		// return it
		final Cycle cycle = new Cycle();
		cycle.index = index;
		cycle.average = average;
		cycle.urlHash = "";
		return cycle;
	}

	Category parseCategory(final Element $cat, final String courseId) {
        // Try to retrieve a weight for each category. Since we have to support
        // IB-MYP grading,
        // category weights are not guaranteed to add up to 100%. However,
        // regardless of which
        // weighting scheme we are using, grade calculations should be able to
        // use the weights
        // as they are parsed below.
        //Get category info out of <br> tags

        //0=Title, 1=Average, 2=Weight
        String[] $catInfo;
        try{
            $catInfo = $cat.getElementsByTag("h1").text().split("split");
        }
        catch (Exception e){
            return null;
        }
        // Some teachers don't put their assignments out of 100 points. Check if
        // this is the case.
        final boolean is100Pt = $cat.select("td.AssignmentPointsPossible")
            .size() == 0;
		String categoryTableId = ($catInfo[0].trim().replace(" ", "_") + "BodyTable").trim();
		// Find all of the assignments using category name since assginment table id is CategoryName + "BodyTable"
        //TODO Ghost category sometimes inserted
		Elements $assignments;
        try{
             $assignments = $cat.getElementById(categoryTableId).getElementsByTag("tr");
        }
        catch (Exception E){
            return null;
        }

		// parse category average
		final Matcher averageMatcher = NUMERIC_REGEX.matcher($catInfo[1]);

		// parse class weight
		final Matcher weightdMatcher = NUMERIC_REGEX.matcher($catInfo[2]);
		// generate category ID
		final String catId = Hash.SHA1(courseId + '|' + $catInfo[0].trim());

		// parse assignments
		Assignment[] assignments = new Assignment[$assignments.size()];
		for (int i = 0; i < assignments.length; i++) {
			assignments[i] = parseAssignment($assignments.get(i), is100Pt,
					catId);
		}

		final Category cat = new Category();
		cat.id = catId;
		cat.title = $catInfo[0].trim();
		cat.weight = weightdMatcher.find() ? Integer.valueOf(weightdMatcher.group(0)) : null;
		cat.average = averageMatcher.find() ? Double
				.valueOf(averageMatcher.group(0)) : null;
		cat.bonus = GradeCalc.categoryBonuses(assignments);
		cat.assignments = assignments;
		return cat;
	}

	Assignment parseAssignment(final Element $row, final boolean is100Pt,
			final String catId) {
		Elements $cells = $row.getElementsByTag("td");
		// Format - 0= Title 1= pts earned 2=Assign Date 3= Due Date 4 = scale 5=Max Val 6=Count 7=Note
		final String title =  $cells.get(0).text();
		final String dateDue =  $cells.get(3).text();
		final String dateAssigned = $cells.get(2).text();
		//TODO: Very weird that we have to catch an exception here... but sometimes array length is only 7
		String note = "";
		try{
			note = $cells.get(7).text();
		}
		catch (Exception e){note = "";}
		final String ptsEarned = $cells.get(1).text();
		//TODO: Very weird that we have to catch an exception here... but sometimes cell value is "100 2000"
		int ptsPossNum = 100;
		try{
			ptsPossNum = Integer.parseInt($cells.get(4).text());
		}
		catch (Exception e){ptsPossNum = 100;}
		// Retrieve both the points earned and the weight of the assignment.
		// Some teachers
		// put in assignments with weights; if so, they look like this:
		// 88x0.6
		// 90x0.2
		// 100x0.2
		// The first number is the number of points earned on the assignment;
		// the second is
		// the weight of the assignment within the category.
		// If the weight is not specified, it is assumed to be 1.
		Double ptsEarnedNum = null;
		final double weight;
		if (ptsEarned.contains("x")) {
			String[] ptsSplit = ptsEarned.split("x");
			// some teachers like to enter 'Exc' for grades apparently, so check
			// if our split is actually parseable.
			if (Numeric.isNumeric(ptsSplit[0])
					&& Numeric.isNumeric(ptsSplit[1])) {
				ptsEarnedNum = Double.valueOf(ptsSplit[0]);
				weight = Double.valueOf(ptsSplit[1]);
			} else {
				weight = 1;
			}
		} else {
			ptsEarnedNum = Numeric.isNumeric(ptsEarned) ? Double
					.valueOf(ptsEarned) : null;
			weight = 1;
		}
		// turn points earned into grade value
		final GradeValue grade;
		if (ptsEarnedNum == null) {
			grade = new GradeValue(ptsEarned);
		} else {
			grade = new GradeValue((ptsEarnedNum/ptsPossNum) * 100);
		}

		// generate the assignment ID
		final String assignmentId = Hash.SHA1(catId + '|' + title);

		// Guess if the assignment is extra credit or not. GradeSpeed doesn't
		// exactly
		// just tell us if an assignment is extra credit or not, but we can
		// guess
		// from the assignment title and the note attached.
		// If either contains something along the lines of 'extra credit', we
		// assume
		// that it is extra credit.
		final boolean extraCredit = title
				.matches(EXTRA_CREDIT_REGEX.toString())
				|| note.matches(EXTRA_CREDIT_NOTE_REGEX.toString());

		// return an assignment
		final Assignment assignment = new Assignment();
		assignment.id = assignmentId;
		assignment.title = title;
		assignment.dateDue = dateDue;
		assignment.dateAssigned = dateAssigned;
		assignment.ptsEarned = grade;
		assignment.ptsPossible = ptsPossNum;
		assignment.weight = weight;
		assignment.note = note;
		assignment.extraCredit = extraCredit;
		return assignment;
	}

	String getCourseIdFromHash(String hash) {
		return hash.split("\\|")[3];
	}

	String decodeURIComponent(String str) {
		try {
			return URLDecoder.decode(str.replace("+", "%2B"), "UTF-8").replace(
					"%2B", "+");
		} catch (UnsupportedEncodingException e) {
			System.err
					.println("URLDecoder threw UnsupportedEncodingException; ignoring.");
			e.printStackTrace();
			return str;
		}
	}

	String getTextByClass(Element parent, String klass) {
		return parent.getElementsByClass(klass).first().text();
	}

    public String parseStudentInfoLocID(int idIndex,final String html) {
        Document d = Jsoup.parse(html);
        Element studentTable = d.getElementById("tableBodyTable");
        return studentTable.getElementsByTag("tr").get(idIndex).attributes().get("locid").toString();
    }
    public int parseStudentInfoIndex(String studentID,final String html) {
        Document d = Jsoup.parse(html);
        Element studentTable = d.getElementById("tableBodyTable");
        Elements possibleStudents = studentTable.getElementsByTag("tr");
        for (int i = 0; i < possibleStudents.size();i++){
            String localStudentID = possibleStudents.get(i).getElementsByTag("td").first().text();
            if(localStudentID.equals(studentID)){
                return i;
            }
        }
        return -1;
    }

    class SemesterParams {
		public int semesters;
		public int cyclesPerSemester;
		public boolean hasExams;
		public boolean hasSemesterAverages;
	}

	public Element getCourseElement(String averagesHtml, Course course, int cycle) {
		Document doc = Jsoup.parse(averagesHtml);
		// Define Grade/Metadata Table
		final Element $metadataTable = doc.getElementById("finalTablebotLeft1")
				.getElementById("tableHeaderTable");
		final Elements $metadataRows = $metadataTable.getElementsByTag("tr");
		final Element $gradeTable = doc
				.getElementById("finalTablebottomRight1").getElementById(
						"tableHeaderTable");
		final Elements $gradeRows = $gradeTable.getElementsByTag("tr");
		int rownum = 0;
		for (int i = 1; i < $metadataRows.size(); i++){
			Elements $metadataCells = $metadataRows.get(i).getElementsByTag("td");
			if(course.courseId.equals($metadataCells.get(0).text())){
				rownum = i;
				break;
			}
		}
		//get the same row in the grades table
		Elements $course = $gradeRows.get(rownum).getElementsByTag("td");
		if (cycle < 3){
			return $course.get(cycle);
		}
		else{
			return $course.get(cycle+2);
		}
	}

}