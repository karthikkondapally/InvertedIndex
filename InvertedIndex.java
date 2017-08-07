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
	static String path = "C:\\Users\\kartkond\\Downloads\\redis-unstable\\redis-unstable\\src\\";
	Map<String, List<Tuple>> index = new HashMap<String, List<Tuple>>();
	Map<String, List<Tuple2>> paramList = new HashMap<String, List<Tuple2>>();
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
				 ArrayList<String> list= Search.pattern(line);
				 String funcName=list.get(0);
				 if (stopwords.contains(funcName)) continue;
				//System.out.println(funcName);
				List <String> listOfSubStr = anagrams(funcName); 
				for (String word : listOfSubStr){
					List<Tuple> idx = index.get(word);
					if (idx == null) {
						idx = new LinkedList<Tuple>();
						index.put(word, idx);
					}
					String arr[] = file.getAbsolutePath().toString().split("\\\\");
					int s = arr.length;
					idx.add(new Tuple(funcName, arr[s-1], fileno, pos));
				}
				list.remove(0);
				for(String str:list ){
					String arr[] = str.split("\\?");
					String type = arr[0];
					String name = arr[1];
				List<String> listOfSubstrP = anagrams(name);
				for (String word : listOfSubstrP){
					List<Tuple2> idx = paramList.get(word);
					if (idx == null) {
						idx = new LinkedList<Tuple2>();
						paramList.put(word, idx);
					}
					String arr1[] = file.getAbsolutePath().toString().split("\\\\");
					int s = arr1.length;
					idx.add(new Tuple2(funcName,name, arr1[s-1], type, pos));
				}
				
				
				
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
					sb.append("<td>"+tuple.funcName.replace(_word, "<font style=\"background:yellow;\">"+_word+"</font>")+" </td><td>"+path+"\\"+tuple.fileName+"</td><td>"+tuple.position+"</td>");
					sb.append("</tr>");
				}
			}
		}
		sb.append("</table>");
		return sb.toString();
	}

	private String searchParamList(List<String> words) {
		StringBuffer sb =new StringBuffer();
		sb.append("\n");
		sb.append("<table style=\"width:100%\">");
		sb.append("<tr>"+ 
				"<th align=\"left\">Param Name</th>"+
				"<th align=\"left\">Param Type</th>"+
				"<th align=\"left\">Function Name</th>"+
				"<th align=\"left\">File Location</th>"+ 
				"<th align=\"left\">Position</th>"+
				"</tr>");
		for (String _word : words) {
			Set<String> answer = new HashSet<String>();
			System.out.println();
			List<Tuple2> idx = paramList.get(_word);
			if(idx !=null){
				for(Tuple2 tuple2 :idx){
					sb.append("<tr>");
					sb.append("<td>"+tuple2.paramName.replace(_word, "<font style=\"background:yellow;\">"+_word+"</font>")+" </td><td>"+tuple2.paramType+" </td><td>"+tuple2.funcName+" </td><td>"+path+"\\"+tuple2.fileName+"</td><td>"+tuple2.position+"</td>");
					sb.append("</tr>");
				}
			}
		}
		sb.append("</table>");
		return sb.toString();	}
	
	public static void main(String[] args) {
		try {
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


			ServerSocket server = new ServerSocket(8081);
			System.out.println("Listening for connection on port 8081 ....");
			while (true) {

				try (Socket socket = server.accept()) {
					InputStream readRequest = socket.getInputStream();
					BufferedReader bf = new BufferedReader(new InputStreamReader(readRequest));
					String bodyMessageEncoded;
					bodyMessageEncoded = bf.readLine();
					System.out.println("request is"+bodyMessageEncoded);
					String funcRegex = ".*[funcName|paramList]=(\\b\\w+\\b).*";
					Pattern pattern = Pattern.compile(funcRegex);
					Matcher match = null;
					if(bodyMessageEncoded != null)
						match = pattern.matcher(bodyMessageEncoded);
					String name = "";
					if(match != null)
						while(match.find()){
							name = match.group(1);
						}
					System.out.println(name);
					String search = "";
					if(bodyMessageEncoded.contains("funcName"))
					 search = idx.search(Arrays.asList(name));	
					if(bodyMessageEncoded.contains("paramList"))
						 search = idx.searchParamList(Arrays.asList(name));
					String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + search;
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
	
	private class Tuple2 {
		private String paramType;
		private int position;
		private String fileName;
		private String paramName;
		private String funcName;

		public Tuple2(String funcName,String paramName, String fileName, String paramType, int position) {
			this.funcName = funcName;
			this.paramName = paramName;
			this.fileName = fileName;
			this.paramType = paramType;
			this.position = position;
		}
	}

	private static class Search{
		/*static String pattern(String line){
			String funcRegex = ".*[ ]*(\\b[\\*]*\\w+\\b)(?=\\(.*\\)).*";
			String str = "";
			Pattern pattern = Pattern.compile(funcRegex);
			Matcher match = pattern.matcher(line);
			while(match.find()){
				str = match.group(1);
			}
			return str;
		}*/
		
		static ArrayList<String> pattern(String line){
			String funcRegex = ".*[ ]*(\\b[\\*]*\\w+\\b)(?=\\((.*)\\)).*";
			String funcName = "";
			
			String str = "";
			Pattern pattern = Pattern.compile(funcRegex);
			Matcher match = pattern.matcher(line);
			while(match.find()){
				funcName = match.group(1);
				str = match.group(2);
			}
			ArrayList <String> list = new ArrayList<String>();
			list.add(funcName);
			String arr [] =str.split(",");
			String name = "";
			String type = "";
			for(String dtype :arr){
				Pattern pattern1 = Pattern.compile("(.*)(\\b[ ]*\\w+\\b)$");
				Matcher match1 = pattern1.matcher(dtype);
				while(match1.find()){
					type = match1.group(1).trim();
					name = match1.group(2).trim();
					if(type.contains("int ") || type.contains("long ")||type.contains("float ")||type.contains("char ")||type.contains("void "))
					list.add(type+"?"+name);
				}
			}
			return list;
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