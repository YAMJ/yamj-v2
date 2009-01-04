// Based on some code from the opensubtitles.org subtitle upload java applet

package com.moviejukebox.plugin;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.logging.Logger;
import java.nio.*;
import java.nio.channels.*;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.plugin.ImdbPlugin;

public class OpenSubtitlesPlugin {

	private static Logger logger = Logger.getLogger("moviejukebox");

	private static String login="";
	private static String pass="";
	private static String useragent="moviejukebox 1.0.15";
	private static String OSdbServer="http://www.opensubtitles.org/xml-rpc";
	private static String token="";
	
	private static String sublanguageid;
	
	
	public OpenSubtitlesPlugin() {
		sublanguageid = PropertiesUtil.getProperty("opensubtitles.language", "");
		login = PropertiesUtil.getProperty("opensubtitles.username", "");
		pass = PropertiesUtil.getProperty("opensubtitles.password", "");

		// Check if subtitle language was selected
		if (!sublanguageid.equals("")) {
		
			// Part of opensubtitles.org protocol requirements
			logger.fine("OpenSubtitles Plugin: Subtitles service allowed by www.OpenSubtitles.org");
			
			// Login to opensubtitles.org system
			logIn();
		} else {
            logger.finest("OpenSubtitles Plugin: No language selected in moviejukebox.properties");
        }
	}

	protected void finalize() throws Throwable
	{
		try {
			if (token.equals("")) {
				logOut();
			}
		} finally {
			super.finalize();
		}
	}

	
	private static void logIn() {
		try {
			if (token.equals("")){
				String parm[]={login,pass,"",useragent};
				String xml=generateXMLRPC("LogIn", parm);
				String ret=sendRPC(xml);
				String status= getVaule("status",ret);
				token= getVaule("token",ret);
				if (token.equals(""))
					logger.severe("OpenSubtitles Plugin: Login error."+"\n"+ret);
				String l1=  login.equals("") ? "Anonymous" : login ;
			};
		} catch(Exception e) {
			logger.severe("OpenSubtitles Plugin: Login Failed");
		};
	}

	private static void logOut() {
		try {
			String p1[]={token};
			String xml=generateXMLRPC("LogOut", p1);
			String ret=sendRPC(xml);
		} catch(Exception e) {
			logger.severe("OpenSubtitles Plugin: Logout Failed");
		};
	}

	
	public void generate(Movie movie) {
	
		// Check if subtitle language was selected
		if (sublanguageid.equals(""))
			return;
		
		// Check that the login was successful
		if (token.equals(""))
			return;
		
		// Check if all files have subtitle
		boolean allSubtitleExist = true;
		
		
		// Go over all the movie files and check subtitle status
		for (MovieFile mf : movie.getMovieFiles()) {
        
			// Check if this movie already have subtitles for it
			String path = mf.getFile().getAbsolutePath();
			int index = path.lastIndexOf(".");
			String basename = path.substring(0, index + 1);
			
			File subtitleFile = new File(basename + "srt");
			
			if (!subtitleFile.exists()) {
				allSubtitleExist = false;
				break;
			}
		}
				

		// Check if all files have subtitle , or this is a tv show, that each episode is by itself
        if (!allSubtitleExist || movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
			
			// Go over all the movie files
			for (MovieFile mf : movie.getMovieFiles()) {
	        
				// Check if this movie already have subtitles for it
				String path = mf.getFile().getAbsolutePath();
				int index = path.lastIndexOf(".");
				String basename = path.substring(0, index + 1);
				
				File subtitleFile = new File(basename + "srt");
				
				if (!subtitleFile.exists()) {
					subtitleDownload(movie, mf.getFile(), subtitleFile);
				}
				else {
					File movieFileArray[] = new File[1];
					File subtitleFileArray[] = new File[1];

					movieFileArray[0] = mf.getFile();
					subtitleFileArray[0] = subtitleFile;

					subtitleUpload(movie, movieFileArray, subtitleFileArray);
				}
	        
			}
		}
		else
		{
			// Upload all movie files as a group
			File movieFileArray[] = new File[movie.getMovieFiles().size()];
			File subtitleFileArray[] = new File[movie.getMovieFiles().size()];
			int i=0;
		
			// Go over all the movie files
			for (MovieFile mf : movie.getMovieFiles()) {
	        
				// Check if this movie already have subtitles for it
				String path = mf.getFile().getAbsolutePath();
				int index = path.lastIndexOf(".");
				String basename = path.substring(0, index + 1);
				
				File subtitleFile = new File(basename + "srt");
				
				movieFileArray[i] = mf.getFile();
				subtitleFileArray[i] = subtitleFile;
				i++;
			}
			
			subtitleUpload(movie, movieFileArray, subtitleFileArray);
		}
	}

	private void subtitleDownload(Movie movie, File movieFile, File subtitleFile) {
		try {

			String ret;
			String xml;

			String moviehash = getHash(movieFile);
			String moviebytesize = String.valueOf(movieFile.length());
			
			xml=generateXMLRPCSS(moviehash, moviebytesize);
			ret=sendRPC(xml);

			String subDownloadLink= getVaule("SubDownloadLink",ret);

			if (subDownloadLink.equals(""))
			{
				// Try to find the subtitle using file name
				String subfilename = subtitleFile.getName();
				int index = subfilename.lastIndexOf(".");
				
				String query = subfilename.substring(0, index);
				
				xml=generateXMLRPCSS(query);
				ret=sendRPC(xml);

				subDownloadLink= getVaule("SubDownloadLink",ret);


				if (subDownloadLink.equals(""))
				{
					logger.finer("OpenSubtitles Plugin: Subtitle not found " + movie.getTitle());
					return;
				}
			}


			URL url = new URL(subDownloadLink);
			HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
			connection.setRequestProperty("Connection","Close");
			InputStream inputStream = connection.getInputStream();
		 
			int code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK)
			{
				logger.severe("OpenSubtitles Plugin: Download Failed");
				return;
			}

			GZIPInputStream  a = new GZIPInputStream(inputStream);
		 
			OutputStream out = new FileOutputStream(subtitleFile);
			byte buf[] = new byte[1024];
			int len;
			while((len = a.read(buf))>0)
				out.write(buf,0,len);
			out.close();


			movie.setSubtitles(true);
	 
		} catch(Exception e) {
			logger.severe("OpenSubtitles Plugin: Download Exception");
		};
	
	}
	
	private void subtitleUpload(Movie movie, File movieFile[], File subtitleFile[]) {
		try {

			String ret="";
			String xml="";

			String idmovieimdb = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID).substring(2);
			idmovieimdb = String.valueOf(Integer.parseInt(idmovieimdb));

			String subfilename[] = new String[movieFile.length];
			String subhash[] = new String[movieFile.length];
			String subcontent[] = new String[movieFile.length];

			String moviehash[] = new String[movieFile.length];
			String moviebytesize[] = new String[movieFile.length];
			String movietimems[] = new String[movieFile.length];
			String movieframes[] = new String[movieFile.length];
			String moviefps[] = new String[movieFile.length];
			String moviefilename[] = new String[movieFile.length];
			
			
			for (int i = 0; i < movieFile.length; i++) {

				subfilename[i] = subtitleFile[i].getName();
				subhash[i] = "";
				subcontent[i] = "";

				moviehash[i] = getHash(movieFile[i]);
				moviebytesize[i] = String.valueOf(movieFile[i].length());
				movietimems[i] = "";
				movieframes[i] = "";
				moviefps[i] = String.valueOf(movie.getFps());
				moviefilename[i] = movieFile[i].getName();

				FileInputStream f = new FileInputStream(subtitleFile[i]);
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte s[] = new byte[f.available()];
				f.read(s);
				md.update(s);
				subhash[i] = hashstring(md.digest()) ;
				ByteArrayOutputStream baos=new ByteArrayOutputStream();
				DeflaterOutputStream  a= new DeflaterOutputStream(baos);
				a.write(s);
				a.finish();
				a.close();	   
				subcontent[i] = tuBase64(baos.toByteArray());
			}			

			// Check if upload of this subtitle is required
			xml=generateXMLRPCTUS(subhash, subfilename, moviehash, moviebytesize, movietimems, movieframes, moviefps, moviefilename);

			ret=sendRPC(xml);
			
            String alreadyindb=getIntVaule("alreadyindb",ret);

			if(!alreadyindb.equals("0")) {
				return;
			}
			
			
			// Upload the subtitle
			xml=generateXMLRPCUS(idmovieimdb, subhash, subcontent, subfilename, moviehash, moviebytesize, movietimems, movieframes, moviefps, moviefilename);
			
			ret=sendRPC(xml);
	 
		} catch(Exception e) {
			logger.severe("OpenSubtitles Plugin: Update Failed");
		};
 	}

	private static String generateXMLRPCSS(String moviehash, String moviebytesize) {
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+="SearchSubtitles";
		str+="</methodName><params><param><value><string>";
		str+=token;
		str+="</string></value></param><param><value><struct>";

		str+="<member><value><struct>";

		str+=addymember("sublanguageid", sublanguageid);
		str+=addymember("moviehash", moviehash);
		str+=addymember("moviebytesize", moviebytesize);
		
		str+="</struct></value></member>";

		str+="</struct></value></param></params></methodCall>";
		return str;
	};

	private static String generateXMLRPCSS(String query) {
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+="SearchSubtitles";
		str+="</methodName><params><param><value><string>";
		str+=token;
		str+="</string></value></param><param><value><struct>";

		str+="<member><value><struct>";

		str+=addymember("sublanguageid", sublanguageid);
		str+=addymember("query", query);
		
		str+="</struct></value></member>";

		str+="</struct></value></param></params></methodCall>";
		return str;
	};

	private static String generateXMLRPCUS(String idmovieimdb, String subhash[], String subcontent[], String subfilename[], String moviehash[], String moviebytesize[], String movietimems[], String movieframes[], String moviefps[], String moviefilename[]) {
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+="UploadSubtitles";
		str+="</methodName><params><param><value><string>";
		str+=token;
		str+="</string></value></param><param><value><struct>";

		for (int i = 0; i < subhash.length; i++) {
			str+="<member><name>" + "cd" + String.valueOf(i+1) + "</name><value><struct>";
			str+=addymember("movietimems", movietimems[i]);
			str+=addymember("moviebytesize", moviebytesize[i]);
			str+=addymember("subfilename", subfilename[i]);
			str+=addymember("subcontent", subcontent[i]);
			str+=addymember("subhash", subhash[i]);
			str+=addymember("movieframes", movieframes[i]);
			str+=addymember("moviefps", moviefps[i]);
			str+=addymember("moviefilename", moviefilename[i]);
			str+=addymember("moviehash", moviehash[i]);
			
			str+="</struct></value></member>";
		}

		str+="<member><name>baseinfo</name><value><struct>";
		str+=addymember("sublanguageid", sublanguageid);
		str+=addymember("idmovieimdb", idmovieimdb);
		str+=addymember("subauthorcomment", "");		 
		str+=addymember("movieaka", "");
		str+=addymember("moviereleasename", "");
	   
		str+="</struct></value></member>";


		str+="</struct></value></param></params></methodCall>";
		return str;
	}
	
	private static String generateXMLRPCTUS(String subhash[], String subfilename[], String moviehash[], String moviebytesize[], String movietimems[], String movieframes[], String moviefps[], String moviefilename[]) {
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+="TryUploadSubtitles";
		str+="</methodName><params><param><value><string>";
		str+=token;
		str+="</string></value></param><param><value><struct>";

		for (int i = 0; i < subhash.length; i++) {
			str+="<member><name>" + "cd" + String.valueOf(i+1) + "</name><value><struct>";
			str+=addymember("movietimems", movietimems[i]);
			str+=addymember("moviebytesize", moviebytesize[i]);
			str+=addymember("subfilename", subfilename[i]);
			str+=addymember("subhash", subhash[i]);
			str+=addymember("movieframes", movieframes[i]);
			str+=addymember("moviefps", moviefps[i]);
			str+=addymember("moviefilename", moviefilename[i]);
			str+=addymember("moviehash", moviehash[i]);
			
			str+="</struct></value></member>";
		}
	
		str+="</struct></value></param></params></methodCall>";
		return str;
	};
	
	
	private static String sendRPCDetectLang(byte text[]) throws MalformedURLException, IOException {
		String str="";
		String strona =OSdbServer;
	   // String logowanie=xml;
		URL url = new URL(strona);
		URLConnection connection=url.openConnection();
		connection.setRequestProperty("Connection","Close");
	   
		// connection.setRequestProperty("Accept","text/html");
		connection.setRequestProperty("Content-Type","text/xml");
		connection.setDoOutput(true);
		//PrintWriter out= new PrintWriter(connection.getOutputStream());
		//out.print(logowanie);
		String  str2="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>"
				+"DetectLanguage"+"</methodName><params>"
				+"<param><value><string>"+token+"</string></value></param>"
				+"<param><value>"
				+"<struct><value><string>";
	   
				
		 String str3="</string></value></struct></value></param></params></methodCall>";
			   
		connection.getOutputStream().write(str2.getBytes("UTF-8"));
		connection.getOutputStream().write(text);
		connection.getOutputStream().write(str3.getBytes("UTF-8"));		

		Scanner in;
		in=new Scanner(   connection.getInputStream());
		while(in.hasNextLine()){
			str+=in.nextLine();
		};
		return str;
	};
	
	private static String sendRPC(String xml) throws MalformedURLException, IOException {
		
		String str="";
		String strona =OSdbServer;
		String logowanie=xml;
		URL url = new URL(strona);
		URLConnection connection=url.openConnection();
		connection.setRequestProperty("Connection","Close");
	   
		// connection.setRequestProperty("Accept","text/html");
		connection.setRequestProperty("Content-Type","text/xml");
		connection.setDoOutput(true);

		connection.getOutputStream().write(logowanie.getBytes("UTF-8"));

		Scanner in;
		in=new Scanner(   connection.getInputStream());
		while(in.hasNextLine()){
			str+=in.nextLine();
		};
		return str;
	}
	
	private static String getVaule(String find,String xml) {
		String str="";
		int a=xml.indexOf(find);
		if (a!=-1){
			int b=xml.indexOf("<string>",a);
			int c=xml.indexOf("</string>",b);
			if ((b!=-1) &&(c!=-1))
				return xml.substring(b+8,c);
		};
		return str;
	}
	
	private static String getIntVaule(String find,String xml) {
		String str="";
		int a=xml.indexOf(find);
		if (a!=-1){
			int b=xml.indexOf("<int>",a);
			int c=xml.indexOf("</int>",b);
			if ((b!=-1) && (c!=-1))
				return xml.substring(b+5,c);
		};
		return str;
	}
	
	
	
	
	private static String hashstring(byte[] arayhash) {
		String s="";
		String hex="0123456789abcdef";
		for (int i=0;i<arayhash.length;i++){
			int m=arayhash[i]&0xff;
			s=s+hex.charAt(m>>4)+hex.charAt(m&0xf);//do not use "s+="
		};
		return s;
	}
	
	private static String generateXMLRPCDetectLang(String body){
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+="DetectLanguage"+"</methodName><params>";
		
		str+="<param><value><string>"+token+"</string></value></param>";
		str+="<param><value>"+body+" </value></param>";
		
		str+="</params></methodCall>";
		return str;
	}
	
	private static String generateXMLRPC(String procname,String s[]){
		String str="";
		str+="<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
		str+=procname+"</methodName><params>";
		for (int i=0;i<s.length;i++)
			str+="<param><value><string>"+s[i]+"</string></value></param>";
		str+="</params></methodCall>";
		return str;
	}
	
	private static String addEncje(String a) {
		a=a.replace("&","&amp;");
		a=a.replace("<","&lt;");
		a=a.replace(">","&gt;");
		a=a.replace("'","&apos;");
		a=a.replace("\"","&quot;");
		return a;
	}
	
	
	private static String tuBase64(byte s[]) {
		// You may use this for lower  applet size
		// return new  sun.misc.BASE64Encoder().encode(s);
		
		char tx;
		long mili= Calendar.getInstance().getTimeInMillis() ;
		String str="";
		String t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
		char t2[]=t.toCharArray();		
		char wynik[]=new char[(s.length/3+1)*4];
		
		int tri=0;
		for (int i=0;i<(s.length/3+1) ;i++) {
			tri=0;
			int iii=i*3;
			try{
				tri|= (s[iii  ] & 0xff)<<16;
				tri|= (s[iii+1] & 0xff)<<8;
				tri|= (s[iii+2] & 0xff);			
			 } catch(Exception e) {};
			for(int j = 0; j < 4; j++)			  
				wynik[i*4+j]=(iii*8+j*6>=s.length*8) ? '=' : t2[(tri >> 6*(3-j)) & 0x3f];								  
			//  if((i+1) % 19 ==0 ) str +="\n";
		};
		str = new String(wynik);
		if ( str.endsWith("====") ) str=str.substring(0, str.length()-4);
		
		return str;
	}

	private static String addymember(String name,String value){
		return "<member><name>"+
				name+"</name><value><string>"+
				OpenSubtitlesPlugin.addEncje(value)+"</string></value></member>";
	}



	private static String getHash(File f) throws IOException {
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
		long sz = fc.size();
	   
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 65536);
		long sum=sz;
		
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i=0;i<65536/8;i++)
			sum+=bb.getLong();// sum(bb);
		
		bb = fc.map(FileChannel.MapMode.READ_ONLY, sz-65536, 65536);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i=0;i<65536/8;i++)
			sum+=bb.getLong();// sum(bb);
		
		sum=sum & 0xffffffffffffffffL;
		 
		String s=String.format("%016x", sum);
		fc.close();
		return s;
	}
   
}
