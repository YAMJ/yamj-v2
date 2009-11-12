**************************************************************
** MovieJukebox V1.7
***************************************************************

Features
--------
    - Recursively scan your video library for video files (AVI, MKV, etc.)
    - Generate jukebox from multiple video library sources
    - Analyze each video file filename to extract information like:
            year, language, subtitles, container, part, FPS, audio and video codecs
    - Analyze NFO files (for IMDb id)
    - Support for TV shows (season, episode number, episode title)
    - IMDb search plugin
    - Library sorting and indexing    
    - Generate XML persistence for movie details and indexes (allowing XSL transformation for generating HTML reports)
    - Generate HTML reports for movies index and details
    - Support for Popcorn Hour links in the movie details reports (clicking will launch the movie)
    - Reports are generated outside your actual library (to avoid risks of altering your data)

Please read the Wiki
--------------------

The online Wiki has more information than is contained here. It can be found at:
    http://code.google.com/p/moviejukebox/w/list

How does it work?
-----------------

    - Install Java Runtime Environment (JRE) 1.6

    - Unpack the MovieJukebox archive

    - Install MediaInfo CLI (not full version, only command line version)
      right into the movieJukebox directory (moviejukebox/mediaInfo)
          * Tested with MediaInfo version 0.7.7.0
          * You can download at: http://sourceforge.net/project/showfiles.php?group_id=86862&package_id=90612

    - Edit the file moviejukebox.properties and setup according to your needs

    - Select the desired skin from the skin directory and set in the 
      mjb.skin.dir property in the moviejukebox.properties file default is 
      a HD skin. An SD skin is also available.

    - Edit the skin.properties in the selected skin dir and setup according to your needs

    - In a command shell (AKA DOS box), Execute the MovieJukebox command. 
      The syntax for the MovieJukebox command is the following:

              MovieJukebox [movieLibrary] [-o jukeboxRoot] [-c] [-p propertiesFile]

      Where parameters are:

            movieLibrary        : OPTIONAL
                                  The directory (local or network)
                                  where your movie files are stored.
                                  - or -   
                                  The XML configuration file specifying one or
                                  more to be scanned for movies.  See 
                                  "Using a libraries XML file as input" below.

                                  If not specified on the command line, the 
                                  mjb.movieLibrary property  from the properties file
                                  will be used.

            -o jukeboxRoot      : OPTIONAL
                                  output directory (local or network directory)
                                  This is where the jukebox files will be written.
                                  If not specified on the the command line, the 
                                  mjb.jukeboxRoot property from the properties file will be used.
                                  If that property also isn't defined, it'll default to the value of  
                                  movieLibrary.

            -c                  : OPTIONAL
                                  Clean the jukebox directory after running.
                                  This will delete any unused files from the jukebox
                                  directory at the end of the run.

            -p propertiesFile   : OPTIONAL      
                                  The properties file to use instead of the default
                                  moviejukebox.properties                            

      For instance, the following command will analyze video files from directory Z:\Video
      The jukebox files will be stored into this directory as well.
      drive "Z:\" can be a local drive, a USB drive or a network drive. 

                MovieJukebox Z:\Video

      If you want the Jukebox files to be generated separately, you may use the following command:

                MovieJukebox Z:\Video -o C:\MyJukebox

      In this case, all generated file will go to the C:\MyJukebox directory

    - MovieJukebox uses the specified input directory as the root path of your video files library.
    - MovieJukebox will create the following files:
 
               Z:\Video\Jukebox\exportdetails_item_popcorn.css
                               \Movie1.fanart.jpg           <-- fanart background for "Movie1" 
                               \Movie1.html                 <-- movie detail html report for "Movie1"
                               \Movie1.jpg                  <-- source poster for "Movie1" 
                               \Movie1_large.png            <-- resized poster for "Movie1" 
                               \Movie1_small.png            <-- poster thumbnail for "Movie1" 
                               \Movie1.xml                  <-- "Movie1" xml information from IMDb

                               \Movie2.fanart.jpg
                               \Movie2.html
                               \Movie2.jpg
                               \Movie2_large.png
                               \Movie2_small.png
                               \Movie2.xml
                               ...
    
               Z:\Video\index.htm                    <-- movies index

    - On your PCH when prompted by the four icons, choose the last one, and then
        navigate to the "Video" folder, then select one of the YAMJ index files.

Using a libraries XML file as input
-----------------------------------

    In order to generate en jukebox for several movie libraries, you have to 
    create an XML file specifying every library you need.

    Here is an example of such a file:

        <?xml version="1.0" encoding="utf-8"?>
        <libraries>
          <library>
              <path>/media/HD1/Movies/</path>
              <nmtpath>file:///opt/sybhttpd/localhost.drives/HARD_DISK/Movies/</nmtpath>
              <exclude name="sample,tmp/,temp/"/>
          </library>

          <library>
              <path>/media/HD1/TV/</path>
              <nmtpath>file:///opt/sybhttpd/localhost.drives/USB_DRIVE_A-1/TV/</nmtpath>
              <exclude name="sample,tmp/,temp/"/>
          </library>

          <library>
              <path>E:\Misc\</path>
              <nmtpath>file:///opt/sybhttpd/localhost.drives/NETWORK_SHARE/PC/Misc/</nmtpath>
              <exclude name="sample,tmp/,temp/"/>
          </library>
        </libraries>

    In this example, 3 libraries are defined. For each of them, The following data must be specified:

        path: root path of your movie library
        nmtpath: NMT root path 
        exclude: a comma separated list of excluded strings. when found in the filename, the file is excluded.


    Once this file is created, you may run moviejukebox. 
    For instance:

         MovieJukebox libraries.xml -o MyJukebox


Things you should know about
----------------------------
 
     - The basic data flow is the following:

         Step1: scan directory    Video/I.Am.Legend.720p23.FRENCH.BRDRiP.x264.AC3-iDHD.mkv
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

          * you can test that request into IMDb itself to check that the new filename 
            will return the appropriate film first.

          * either you can add a new file ".NFO" next to your video file (same name as the 
            video file), that will contain the IMDb url of the film matching the video file

    For instance:

                                 http://www.imdb.com/title/tt0097576

         In both cases, you'll have to delete the generated files those for this movie (.xml, 
          .jpg, _small.jpeg, .html) in the Jukebox directory, then relaunch MovieJukebox.

       - When the poster used in the IMDb is not good enough, you can replace it with a good 
         one into the Jukebox directory.

         If you then delete the corresponding thumbnail file (XXXXX_small.jpg), and relaunch the 
         Movie jukebox tool, the thumbnail file will be regenerated with you new poster.

Multi-Threading Performance
---------------------------

    Version 1.5 of YAMJ introduces multi-threading to speed up the performance of YAMJ.
    Initially the scanning parameters are set to a value of one each, meaning that there is
    one scanning thread and one processing thread. You can try increasing these parameters to
    larger values to increase performance. Increasing these parameters beyond "1" can lead to
    unstable performance, such as skin compilation errors and downloaded image errors.


Important notes
---------------

    If you find this tool useful for you and that you would like to improve it, please do so!!

    I'm looking for guys with Java, XSL, HTML or graphics skills in order to improve the basic HTML 
    reports provided in this version.

    Especially, you can customize the following files:

       detail.xsl:    XSL file that generate the HTML code, given the .xml detail of the movie.
       index.xsl:     XSL file that generate the HTML code, given the .xml files of the library indexes.
       dummy.jpg:     This is the default picture used when the movie is not found.

       exportdetails_item_popcorn.css, exportindex_item_pch.css:
                      CSS used for the movie details and indexes. 
                      These CSS files are taken from the sample pchtest.zip provided 
                      by Michaal T. (michael1t), Thanx!
                      (http://www.networkedmediatank.com/viewtopic.php?p=22825&sid=5feac8722f0aed6c6691c7a7a353b45b)

    Please report bugs, comments, feature requests to: http://code.google.com/p/moviejukebox/issues/list

