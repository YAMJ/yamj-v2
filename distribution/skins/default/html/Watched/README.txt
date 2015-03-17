WATCHED README
==============================================================================

Watched for Yamj is dynamic and consists of two files. It works on the detail
page of a movie or TV Episodes page.

The Watched: value on the page is changed to Yes or No by pressing the RED
button on the Remote. When the page loads or an episode receives focus the
status will be shown automatically.

On a movie detail page the javascript var baseFileName is used as the file
created by watched. eg: Blood Diamond would be... Blood Diamond.watched.

On a TV Show or Multipart the video file name is used.
eg: Dexter S01E01.avi would be... Dexter S01E01.watched

There are two files.

watched.js
This file is a javascipt file and has been added to the detail.xls file.
This file would be best stored in the jukebox folder for ease of install.
Currently detail.xsl loads it from this folder. The file has comments, read!

watched.cgi
This is a Bash shell script. Thus it is not required to have the NMT apps installed.
The script gets its arguments from watched.js so there is no need to alter it.

WARNING: If you edit the file be sure to use an editor which saves the file
with Unix type line ends!

Being a cgi it must, WHEN installed on a local PCH disk have its permissions set
to a value of 755, this gives it execute rights.

On a network drive this is not necessary.
The location of this file is determined by the wScript var in the watched.js file.

In order for this all to work a folder must exist with write permissions to
store the .watched files.

Finally the details.xsl file has been modified to add the script and fix
a few issues, which until dynamic content was added did not really show.

The short story...
1) Replace the details.xsl file (after backup) in the skins/default folder of
Yamj. You will to re-run yamj to generate the new html files.

2) Copy the watched.js file to the jukebox folder.

3) Create a watched folder on a disk, network or local PCH. I suggest this NOT
be off the jukebox folder if that folder is off Video on the local PCH disk.

4) Copy the watched.cgi file to some folder, it could be the watched folder or
even the jukebox folder. Remember if on local PCH disk set the permissions to
755.

Once done, open the watched.js folder and adjust the wScript and wFiles vars
to suit your setup! SEE IMPORTANT UPDATE BELOW.

Your done. Press the red button on the detail page of a movie or TV episode to
toggle it's watched state, it should say Yes if successful, press again to toggle.
When you return to the page you will see that the state has been maintained.

NOTES:
If a user deletes .watched files, who cares.
If users have used the an NFO file and added watched, then
Yamj can generate the .watched file and place them in the required folder.
It would get the location from the properties.watched.js file.

Yamj would need to use the baseFileName var for single file movies and the
video.avi(mkv)(etc) when dealing with TV Episodes or multiparts, as
described above.


IMPORTANT UPDATE
There is now an additional Javascript file called watched.properties.js this
will make settings easier, it provides the key variables, wFiles and wScript.
Values here will override those in the watched.js file.


ENDS
