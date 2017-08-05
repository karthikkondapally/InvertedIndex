import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvertedIndex {

	List<String> stopwords = Arrays.asList("","if","while","O","for","switch","int","double","float","char");

	Map<String, List<Tuple>> index = new HashMap<String, List<Tuple>>();
	List<String> files = new ArrayList<String>();

	public void indexFile(File file) throws IOException {
		int fileno = files.indexOf(file.getPath());
		if (fileno == -1) {
			files.add(file.getPath());
			fileno = files.size() - 1;
		}

		int pos = 0;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			{
				pos++;
				String funcName = Search.pattern(line);
				if (stopwords.contains(funcName)) continue;
				//System.out.println(funcName);
				List <String> listOfSubStr = anagrams(funcName); 
				for (String word : listOfSubStr){
					List<Tuple> idx = index.get(word);
					if (idx == null) {
						idx = new LinkedList<Tuple>();
						index.put(word, idx);
					}
					idx.add(new Tuple(funcName,file.getAbsolutePath().toString(),fileno, pos));
				}
			}
		}
		//System.out.println("indexed " + file.getPath() + " " + pos + " words");
	}

	private List<String> anagrams(String word) {
		List<String> listOfWords = new ArrayList<String>();
		for (int from = 0; from < word.length(); from++) {
			for (int to = from + 1; to <= word.length(); to++) {
				listOfWords.add(word.substring(from, to));
			}
		}		return listOfWords;
	}

	public String search(List<String> words) {
		StringBuffer sb =new StringBuffer();
		sb.append("\n");
		sb.append("<table style=\"width:100%\">");
		sb.append("<tr>"+ 
 "<th align=\"left\">Function Name</th>"+
 "<th align=\"left\">File Location</th>"+ 
  "<th align=\"left\">Position</th>"+
  "</tr>");
		for (String _word : words) {
			Set<String> answer = new HashSet<String>();
			System.out.println();
			List<Tuple> idx = index.get(_word);
			if(idx !=null){
			for(Tuple tuple :idx){
				sb.append("<tr>");
				sb.append("<td>"+tuple.funcName.replace(_word, "<font style=\"background:yellow;\">"+_word+"</font>")+" </td><td>"+tuple.fileName+"</td><td>"+tuple.position+"</td>");
				sb.append("</tr>");
			  }
			}
		}
		sb.append("</table>");
		return sb.toString();
	}

	public static void main(String[] args) {
		try {
			String path = "C:\\Users\\karthik\\Downloads\\redis-unstable\\src";
			File folder = new File(path);
			File[] listOfFiles = folder.listFiles();
			InvertedIndex idx = new InvertedIndex();
			System.out.println("Creating inverted index for *.c and *.h files");
			for (File file: listOfFiles) {
				if(file.getAbsolutePath().toString().endsWith(".h") || file.getAbsolutePath().toString().endsWith(".c"))
				idx.indexFile(file);
			}
			System.out.println("done");
			//idx.search(Arrays.asList("dictFetchValue"));
			
			
		    ServerSocket server = new ServerSocket(8080);
	        System.out.println("Listening for connection on port 8080 ....");
	        while (true) {

		                try (Socket socket = server.accept()) {
		                    InputStream readRequest = socket.getInputStream();
		                    BufferedReader bf = new BufferedReader(new InputStreamReader(readRequest));
		                    String bodyMessageEncoded;
		                    bodyMessageEncoded = bf.readLine();
		                    System.out.println("request is"+bodyMessageEncoded);
		                    String funcRegex = ".*funcName=(\\b\\w+\\b).*";
		        			Pattern pattern = Pattern.compile(funcRegex);
		        			Matcher match = pattern.matcher(bodyMessageEncoded);
		        			String name = "";
		        			while(match.find()){
		        				name = match.group(1);
		        			}
		        				System.out.println(name);	
		                    String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + idx.search(Arrays.asList(name));
		                    socket.getOutputStream().write(httpResponse.getBytes("UTF-8"));
		                } catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	        	 


	        }
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class Tuple {
		private int fileno;
		private int position;
		private String fileName;
		private String funcName;

		public Tuple(String funcName, String fileName, int fileno, int position) {
			this.funcName = funcName;
			this.fileName = fileName;
			this.fileno = fileno;
			this.position = position;
		}
	}

	private static class Search{
		static String pattern(String line){
			String funcRegex = ".*[ ]*(\\b[\\*]*\\w+\\b)(?=\\(.*\\)).*";
			String str = "";
			Pattern pattern = Pattern.compile(funcRegex);
			Matcher match = pattern.matcher(line);
			while(match.find()){
				str = match.group(1);
			}
			return str;
		}
	}
	
	public class Test {

		public void test(String[] args) {
			// TODO Auto-generated method stub
			String word = "zmalloc(int abc, unsingned int *abdef)";
			String funcRegex = ".*[ ]*(\\b[\\*]*\\w+\\b)(?=\\((.*)\\)).*";
			String str = "";
			Pattern pattern = Pattern.compile(funcRegex);
			Matcher match = pattern.matcher(word);
			while(match.find()){
				str = match.group(2);
			}	
			String arr [] =str.split(",");
			for(String dtype :arr){
				Pattern pattern1 = Pattern.compile("(.*)(\\b[ ]*\\w+\\b)$");
				Matcher match1 = pattern1.matcher(dtype);
			
				while(match1.find()){
					str = match1.group(2);
				}
				System.out.println(str.trim());
			}
			System.out.println();
		}

	}

}