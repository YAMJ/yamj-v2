**************************************************************
** MovieJukebox V1.0 (beta1)
***************************************************************

Features
--------

	- Recursively scan your video library for video files (AVI, MKV, etc.)
	- Analyze each video file filename to extract information like: year, language, subtitles, container, part, fps, audio and video codecs
	- Analyze NFO files (for IMDb id)
	- Support for TV shows (season, episode number, episode title)
	- IMDb search plugin
	- Library sorting and indexing	
	- Generate XML persistence for movie details and indexes (allowing XSL transformation for generating HTML reports)
	- Generate HTML reports for movies index and details
	- Support for Popcorn Hour links in the movie details reports (clicking will launch the movie)
	- Reports are generated outside your actual library (to avoid risks of altering your data)

How does it work?
-----------------

	- Install Java Runtime Environment (JRE) 1.5
	- Mount a network drive to your NMT internal hard disk
	- Then Execute MovieJukebox command line tool. For instance on a windows environment:
	
				MovieJukebox Z:\Video
				
	- MovieJukebox uses the specified input directory as the root path of your video files library.
	- MovieJukebox will create the following files:
	
               Z:\Video\Jukebox\exportdetails_item_popcorn.css
                               \Movie1.xml                  <-- "Movie1" xml information from IMDb
                               \Movie1.jpg                  <-- poster for "Movie1" 
                               \Movie1_small.jpg            <-- poster thumbnail for "Movie1" 
                               \Movie1.html                 <-- movie detail html report for "Movie1"

                               \Movie2.xml
                               \Movie2.jpg
                               \Movie2_small.jpg
                               \Movie2.html					
	                           ...
	
               Z:\Video\exportindex_item_pch.css
                        index_09AF.xml              <-- index content
                        index_09AF.html             <-- movies index for [0-9],[A-F]
                        index_GL.xml
                        index_GL.html               <-- movies index for [G-L]
                        index_MR.xml
                        index_MR.html               <-- movies index for [M-R]
                        index_SZ.xml
                        index_SZ.html               <-- movies index for [S-Z]
                                                
	- On your PCH, go to "Video", then select one of the index files.


Things you should know about
----------------------------
 
 	- The basic data flow is the following:

 	    Step1: scan directory  Video/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.mkv
 	                           Video/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.nfo
                                                          |
                                                          V
 	    Step2: scan IMDb       Jukebox/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.jpg
                                   Jukebox/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD_small.jpg
 	                           Jukebox/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.xml
                                                          |
                                                          V
            Step3: build HTML      Jukebox/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.html
        

	- When executed twice in the same directory, MovieJukebox will not overwrite the files 
	  that are already generated in the Jukebox. This way you can easily customize your 
	  movie posters for instance without the risk of loosing your changes once you run 
	  the tool again.
	  
	- If you want to regenerate one file in the Jukebox, just erase this file, then run
	  MovieJukebox again. It will recreate it without overwriting other files.
	  
	- When MovieJukebox searches IMDb for the movie data, the generated IMDb request may lead
	  to more than one result for the specified keywords. In this case, MovieJukebox will
	  take into account the first result.
	  
	  Sometimes, this rule leads the detection of wrong film IDs. In this case, you can
	  do the following:
	  
	      * either you can rename your video file in order to make it unambiguous to IMDb,
	        by adding, the production year or the director's name for instance.
	        
	        you can test that request into IMDb itself to check that the new filename 
	        will return the appropriate film first.

              * either you can add a new file ".NFO" next to your video file (same name as the    
                video file), that will contain the IMDb url of the film matching the video file
                For instance:           

                                 http://www.imdb.com/title/tt0097576
            
          In both cases, you'll have to delete the generated files those for this movie (.xml, 
          .jpg, _small.jpeg, .html) in the Jukebox directory, then relaunch MovieJukebox.
     
     
       - When the poster used in the IMDb is not good enought, you can replace it with a good 
         one into the Jukebox directory.
      
         If you then delete the correspondng thumbail file (XXXXX_small.jpg), and relaunch the 
         Movie lib tool, the thumbnail file will be regenerated with you new poster.
      


Important notes
---------------

    If you find this tool useful for you and that you would like to improve it, please do so!!
	
    I'm looking for guys with Java, XSL, HTML or graphics skills in order to improve the basic HTML 
    reports provided in this first version.
	
    Especially, you can customize the following files:
	
	detail.xsl:    XSL file that generate the HTML code, given the .xml detail of the movie.
        index.xsl:     XSL file that generate the HTML code, given the .xml files of the library indexes.
        dummy.jpg:     This is the default picture used when the movie is not found.
    
        exportdetails_item_popcorn.css, exportindex_item_pch.css:
                   CSS used for the movie details and indexes. 
                   These CSS files are taken from the sample pchtest.zip provided 
                   by Michaal T. (michael1t), Thanx!
                   (http://www.networkedmediatank.com/viewtopic.php?p=22825&sid=5feac8722f0aed6c6691c7a7a353b45b)

    Please report bugs, comments, feature requests to: jjulien@hotmail.fr

                   