package com.moviejukebox.scanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.moviejukebox.model.Movie;

/**
 * @author Grael
 */
public class MediaInfoScanner {
	//mediaInfo repository
	private File mediaInfoPath;
	
	//mediaInfo command line, depend on OS
	private String[] mediaInfoExe;
	private String[] mediaInfoExeWindows = { "cmd.exe", "/E:1900", "/C",
			"MediaInfo.exe", null };
	private String[] mediaInfoExeLinux = { "./mediainfo", null };

	public final static String OS_NAME = System.getProperty("os.name");

	public final static String OS_VERSION = System.getProperty("os.version");

	public final static String OS_ARCH = System.getProperty("os.arch");

	/**
	 * @param mediaInfoPath
	 */
	public MediaInfoScanner(Properties props) {
		mediaInfoPath = new File(
				props.getProperty("mediainfo.home", "./mediaInfo/"));
		
		System.out.println("OS name : " + OS_NAME);
		System.out.println("OS version : " + OS_VERSION);
		System.out.println("OS archi : " + OS_ARCH);

		if (OS_NAME.startsWith("Linux")) {
			mediaInfoExe = mediaInfoExeLinux;
		} else {
			mediaInfoExe = mediaInfoExeWindows;
		}
	}

	public void scan(Movie currentMovie) {
		try {
			String[] commandMedia = mediaInfoExe;
			commandMedia[commandMedia.length - 1] = currentMovie.getFile()
					.getAbsolutePath();

			ProcessBuilder pb = new ProcessBuilder(commandMedia);

			// set up the working directory.
			pb.directory(mediaInfoPath);

			Process p = pb.start();

			HashMap<String, String> infosGeneral = new HashMap<String, String>();
			ArrayList<HashMap<String, String>> infosVideo = new ArrayList<HashMap<String, String>>();
			ArrayList<HashMap<String, String>> infosAudio = new ArrayList<HashMap<String, String>>();

			parseMediaInfo(p, infosGeneral, infosVideo, infosAudio);

			updateMovieInfo(currentMovie, infosGeneral, infosVideo, infosAudio);

		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	private String localInputReadLine (BufferedReader input) throws IOException {
		// Suppress empty lines
		String line = input.readLine();
		while ((line!=null) && (line.equals(""))) {
			line = input.readLine();
		}
		return line;
	}
	
	private void parseMediaInfo(Process p,
			HashMap<String, String> infosGeneral,
			ArrayList<HashMap<String, String>> infosVideo,
			ArrayList<HashMap<String, String>> infosAudio) throws IOException {

		
		BufferedReader input = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		
		String line = localInputReadLine(input);
		
		while (line != null) {
			if (line.equals("General")) {
				int indexSeparateur = -1;
				while (((line = localInputReadLine(input)) != null)
						&& ((indexSeparateur = line.indexOf(" : ")) != -1)) {
					int longueurUtile = indexSeparateur - 1;
					while (line.charAt(longueurUtile) == ' ') {
						longueurUtile--;
					}
					infosGeneral.put(line.substring(0, longueurUtile + 1), line
							.substring(indexSeparateur + 3));
				}
			}
			else if (line.startsWith("Video")) {
				HashMap<String, String> vidCourante = new HashMap<String, String>();

				int indexSeparateur = -1;
				while (((line = localInputReadLine(input)) != null)
						&& ((indexSeparateur = line.indexOf(" : ")) != -1)) {
					int longueurUtile = indexSeparateur - 1;
					while (line.charAt(longueurUtile) == ' ') {
						longueurUtile--;
					}
					vidCourante.put(line.substring(0, longueurUtile + 1), line
							.substring(indexSeparateur + 3));
				}
				infosVideo.add(vidCourante);
			}
			else if (line.startsWith("Audio")) {
				HashMap<String, String> audioCourant = new HashMap<String, String>();

				int indexSeparateur = -1;
				while (((line = localInputReadLine(input)) != null)
						&& ((indexSeparateur = line.indexOf(" : ")) != -1)) {
					int longueurUtile = indexSeparateur - 1;
					while (line.charAt(longueurUtile) == ' ') {
						longueurUtile--;
					}
					audioCourant.put(line.substring(0, longueurUtile + 1), line
							.substring(indexSeparateur + 3));
				}
				infosAudio.add(audioCourant);
			}
			else {
				line = localInputReadLine(input);
			}

		}
		input.close();

	}

	private void updateMovieInfo(Movie movie,
			HashMap<String, String> infosGeneral,
			ArrayList<HashMap<String, String>> infosVideo,
			ArrayList<HashMap<String, String>> infosAudio) {

		String infoValue;

		// get Container from Genral Section
		infoValue = infosGeneral.get("Format");
		if (infoValue != null) {
			movie.setContainer(infoValue);
		}

		// get Infos from first Video Stream
		// - can evolve to get info from longuest Video Stream
		if (infosVideo.size() > 0) {
			//Duration
			HashMap<String, String> infosMainVideo = infosVideo.get(0);
			infoValue = infosMainVideo.get("Duration");
			if (infoValue != null) {
				movie.setRuntime(infoValue);
			}

			//Codec (most relevant Info depending on mediainfo result)
			infoValue = infosMainVideo.get("Codec ID/Hint");
			if (infoValue != null) {
				movie.setVideoCodec(infoValue);
			} else {
				infoValue = infosMainVideo.get("Codec ID");
				if (infoValue != null) {
					movie.setVideoCodec(infoValue);
				} else {
					infoValue = infosMainVideo.get("Format");
					if (infoValue != null) {
						movie.setVideoCodec(infoValue);
					}
				}
			}

			//Resolution
			int width = 0;

			infoValue = infosMainVideo.get("Width");
			if (infoValue != null) {
				width = Integer.parseInt(infoValue.substring(0, infoValue
						.indexOf(" ")));

				infoValue = infosMainVideo.get("Height");
				if (infoValue != null) {
					movie.setResolution(width + "x"
							+ infoValue.substring(0, infoValue.indexOf(" ")));
				}

			}

			//Frames per second
			infoValue = infosMainVideo.get("Frame rate");
			if (infoValue != null) {
				movie.setFps(Integer.parseInt(infoValue.substring(0, infoValue
						.indexOf("."))));
			}

			
			// Guessing Video Output

			String normeHD = "SD";
			if (width > 1280) {
				normeHD = "1080";
			} else if (width > 720) {
				normeHD = "720";
			}

			if (!normeHD.equals("SD")) {
				infoValue = infosMainVideo.get("Scan type");
				if (infoValue != null) {
					if (infoValue.equals("Progressive")) {
						normeHD += "p";
					} else {
						normeHD += "i";
					}
				}
				movie.setVideoOutput(normeHD + " " + movie.getFps());

			} else {
				String videoOutput;
				switch (movie.getFps()) {
				case 23:
					videoOutput = "NTSC 23";
					break;
				case 24:
					videoOutput = "NTSC 24";
					break;
				case 25:
					videoOutput = "PAL 25";
					break;
				case 29:
					videoOutput = "NTSC 29";
					break;
				case 30:
					videoOutput = "NTSC 30";
					break;
				case 49:
					videoOutput = "PAL 49";
					break;
				case 50:
					videoOutput = "PAL 50";
					break;
				case 60:
					videoOutput = "NTSC 60";
					break;
				default:
					videoOutput = "NTSC";
					break;
				}
				infoValue = infosMainVideo.get("Scan type");
				if (infoValue != null) {
					if (infoValue.equals("Progressive")) {
						videoOutput += " p";
					} else {
						videoOutput += " i";
					}
				}
				movie.setVideoOutput(videoOutput);

			}
		}
		
		//Cycle through Audio Streams
		movie.setAudioCodec("UNKNOWN");

		for (int numAudio = 0; numAudio < infosAudio.size(); numAudio++) {
			HashMap<String, String> infosCurAudio = infosAudio.get(numAudio);

			String infoLanguage = "";
			infoValue = infosCurAudio.get("Language");
			if (infoValue != null) {
				infoLanguage = " (" + infoValue + ")";
			}

			infoValue = infosCurAudio.get("Codec ID/Hint");
			if (infoValue == null) {
				infoValue = infosCurAudio.get("Format");
			}
			
			if (infoValue != null) {
				String oldInfo = movie.getAudioCodec();
				if (oldInfo.equals("UNKNOWN")) {
					movie.setAudioCodec(infoValue + infoLanguage);
				} else {
					movie.setAudioCodec(oldInfo + " / " + infoValue
							+ infoLanguage);
				}
			}

		}
		
		// Langage, same as codecAudio (should change maybe)
		String oldInfo = movie.getLanguage();
		if (oldInfo.equals("UNKNOWN")) {
			movie.setLanguage(movie.getAudioCodec());
		}
	}

}
