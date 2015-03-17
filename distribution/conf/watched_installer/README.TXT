YAMJ_INSTALLER FOR WATCHED
==========================
These files make it easy to implement the watched script on a PCH local disk.

This prevent the user from having to FTP to the PCH and set permissions on watched.cgi.

There are two files.

yamj_installer.html and yamj_installer.cgi

Simply copy the files onto a USB stick, place the USB stick in the PCH, and click on yamj_installer.html.

By default the message reads:

Setting Script Permissions
Please Wait . . .

If you see

Setting Script Permissions
DONE!

Then the script is finished, it's very fast so you may not even see the "Please Wait" message.

The script will find watched.cgi regardless of it's folder location, using both SATA_DISK and HARD_DISK.

Remember this is only internal drives.

ENDS