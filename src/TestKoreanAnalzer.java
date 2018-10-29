import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.ko.morph.AnalysisOutput;
import org.apache.lucene.analysis.ko.morph.CompoundEntry;
import org.apache.lucene.analysis.ko.morph.MorphAnalyzer;
import org.apache.lucene.analysis.ko.morph.MorphException;
import org.apache.lucene.analysis.ko.morph.WordSegmentAnalyzer;

public class TestKoreanAnalzer {
	
	/*
	 mysql fulltext search
	
	mysql 설정
	[mysqld]
	ft_min_word_len = 1
	innodb_ft_min_token_size = 1
	
	검색을 원하는 컬럼에  fulltext index 생성
	alter table 테이블이름  add FULLTEXT(컬럼이름);
	
	인덱스 재생성
	optimize table 테이블명;
	  
	  
	 */


	public void getAnswer(String keyword) throws Exception {
		String dbUrl = "jdbc:mysql://DB_IP:DB_PORT/DB_NAME";
		String user = "DB_USER";
		String password = "DB_PW";
		String dbClass = "com.mysql.jdbc.Driver";
		
		if (keyword == null || keyword.length() < 1) {
			throw new Exception("not found keyword.");
		}
		
		StringBuffer sb = new StringBuffer();
		String[] keywords = keyword.split(",");
		for (int i=0; i<keywords.length; i++) {
			if (i == (keywords.length -1)) {
				sb.append(keywords[i] + "*");
			} else {
				sb.append(keywords[i] + "* ");
			}
		}
		
		String query = 
				"SELECT question , MATCH (question) AGAINST ('"+ sb.toString() + "' IN BOOLEAN MODE) AS relevance \n"
				+ "FROM temp_tb \n"
				+ "WHERE MATCH (question) AGAINST ('"+ sb.toString() + "' IN BOOLEAN MODE) \n"
				+ "HAVING relevance > 1.0 \n"
				+ "ORDER BY relevance DESC \n"
				+ "LIMIT 0, 10";

		System.out.println(query);
		
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		String question_id = null, question = null;
		Double relevance;
		
		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, user, password);
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				question_id = rs.getString("question_id");
				question = rs.getString("question");
				relevance = rs.getDouble("relevance");
				
				System.out.println(question_id + " " + question + " " + relevance);
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
			if (con != null)
				con.close();
		}
		
	}
	
	public String wordSpaceAnalyze(String source, boolean force) throws MorphException {
		WordSegmentAnalyzer wsAnal = new WordSegmentAnalyzer();

		StringBuilder result = new StringBuilder();

		String s;
		if (force)
			s = source.replace(" ", "");
		else
			s = source;
		
		List<List<AnalysisOutput>> outList = wsAnal.analyze(s);
		for (List<AnalysisOutput> o : outList) {
			for (AnalysisOutput analysisOutput : o) {
				if (analysisOutput.getSource().trim().length() > 0) {
					result.append(analysisOutput.getSource().trim()).append(",");
				}
			}
		}

		return result.toString();
	}

	
	public String wordSpaceAnalyze(String source) throws MorphException {
		return wordSpaceAnalyze(source, false);
	}
	
	public String guideWord(String source) throws MorphException {
		MorphAnalyzer maAnal = new MorphAnalyzer(); // 형태소 분석기

		StringTokenizer stok = new StringTokenizer(source, " "); // 쿼리문을 뛰어쓰기 기준으로 토큰화

		StringBuilder result = new StringBuilder();

		while (stok.hasMoreTokens()) {

			String token = stok.nextToken();

			List<AnalysisOutput> outList = maAnal.analyze(token);
			for (AnalysisOutput o : outList) {

				result.append(o.getStem());

				for (CompoundEntry s : o.getCNounList()) {
					result.append("+" + s.getWord());
				}

				result.append(",");
			}
		}
		String s = result.toString();
		if (s.endsWith(","))
			s = s.substring(0, s.length() - 1);
		return s;
	}
	
	public String removeDuplicates(String userKeyword) {
		
		if (userKeyword == null || userKeyword.length() < 1) {
			return "";
		}
		if (userKeyword.indexOf(',') < 1) {
			return "";
		}
		
		String[] aKeywords = userKeyword.split(",");
		Arrays.sort(aKeywords);
		aKeywords = new HashSet<String>(Arrays.asList(aKeywords)).toArray(new String[0]);
		
		StringBuffer sb = new StringBuffer();
		int wordLength = aKeywords.length;
		for (int i = 0; i < wordLength; i++) {
			if (i < wordLength -1) {
				sb.append(aKeywords[i]).append(",");
			} else {
				sb.append(aKeywords[i]);
			}				
		}
		return sb.toString();
	}
	
	public static void main(String args[]) throws Exception {

		TestKoreanAnalzer t = new TestKoreanAnalzer();
		String inputText = "테스트 문자로 검새을 시도  ";
		String result = t.guideWord(inputText);
		System.out.println("색인어 추출 : " + result);
		
		String result1 = t.wordSpaceAnalyze(inputText,false);
		System.out.println("색인어 추출 : " + result1);
		
		String allKeywords = result + "," + result1;
		System.out.println("색인어 추출 : " + allKeywords);
		String outKeywords = t.removeDuplicates(allKeywords);
		System.out.println("색인어 추출 : " + outKeywords);
		
		
		t.getAnswer(outKeywords);
		
	}
}
