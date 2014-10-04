import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import DataClasses.Student;
import DataClasses.StudentClass;


public class GradeParser {

	public static Student parse(String html) {
		Document d = Jsoup.parse(html);
		//Get table
		Element div = d.getElementById("finalTablebotLeft1");
		Element metadataTable = div.getElementById("tableHeaderTable");
		Elements rows = metadataTable.getElementsByTag("tr");
		
		Student s = new Student("Bob Joe",new StudentClass[rows.size()-1]);
	    for(int i = 0; i < rows.size(); i++) {
	    	 Element row = rows.get(i);
	         if (row.getElementsByTag("td").size() > 1){
	        	 String teacher = row.getElementsByTag("td").get(2).text();
		         String studentClass = row.getElementsByTag("td").get(3).text();
		         String period = row.getElementsByTag("td").get(4).text();
		         s.getClasses()[i-1] = new StudentClass(teacher,studentClass,period,null);
	         }
	    }
		return s;
	}

}
