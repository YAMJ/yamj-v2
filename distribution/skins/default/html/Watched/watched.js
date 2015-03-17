/* Yamj Watched Module
 * ==========================================================================
 * Variables in the watched function that can be changed to suit...
 * wExt    = The file extension applied to the movie/show name
 * wScript = The path and name of the CGI script
             this is a URL, it must pass thru the web server, a HTTP path
 *           (If on local disk must be executable 755)
 * wFiles  = The path only location of .watched files
 *           this is a network path not a url!
 *
 * Note that wScript and wFiles can be set in the watched.properties.js file
 *  which will override values here, thus nothing needs to changed here.
 *
 * Example wFiles
 * /opt/sybhttpd/localhost.drives/SATA_DISK/watched/
 * /opt/sybhttpd/localhost.drives/NETWORK_SHARE/PC1:USBDRIVES/Disk_2/watched/
 * /opt/sybhttpd/localhost.drives/NETWORK_SHARE/192.168.1.10:USBDRIVES/Disk_2/watched/
 *
 * Example wScript a URL is used
 * http://localhost.drives:8883/SATA_DISK/watched/watched.cgi
 * http://localhost.drives:8883/NETWORK_SHARE/PC1:USBDRIVES/Disk_2/somefolder/watched.cgi
 * http://localhost.drives:8883/NETWORK_SHARE/192.168.1.10:USBDRIVES/Disk_2/somefolder/watched.cgi
 *
 * What we might call... Create a watched file for the movie Blood Diamond
 * http://localhost.drives:8883/SATA_DISK/Video/Jukebox/watched/watched.cgi?/opt/sybhttpd/localhost.drives/SATA_DISK/Video/Jukebox/watched/Blood Diamond.watched&1&watchedCallback
 *
 * ? is the watched file name, our args are then '&' delimited, not key/value pairs
 * &0 means check if watched file exists, 1 is create or delete a toggle
 * &watchedCallback is the javascript function to call once the script is done.
 *                  the cgi prints this out, with some params eg:
 *                  watchedCallback(wAction, wStatus)
 *                  wAction what we asked, 0=check, 1=create, 1=delete (toggles)
 *                  wStatus the result, file exists, 0=false, 1=true.
 *                    Used to set the watched status the detail html page.
 *
 * If you wish to check a watched file on page load, use the body onload(...)
 *
 * The values for wScript and wFiles will be overwritten by watched.properties.js
 * ========================================================================== */

var wStatusSaved = 0;
function watched(wAction) {
    var wExt = ".watched";
    var wScript = "http://localhost.drives:8883/"+wScriptLocation+"watched.cgi";
    var wFiles = "/opt/sybhttpd/localhost.drives/"+wlocation;
    var wSrc = "";

    if ( curFocus == "" ) {
        wSrc = wScript + "?" + wFiles + baseFilename + wExt;
    } else {
        wSrc = wScript + "?" + wFiles + curFocus.href.substring(curFocus.href.lastIndexOf("/")+1, curFocus.href.lastIndexOf(".")) + wExt;
    }
    switch(wAction) {
        case 0: // Check existence only
            wSrc += "&0&watchedCallback";
            break;

        default:
            wSrc += "&1&watchedCallback";
            break;
    }
    try {
        //With the new value 2, the watched file will be created only if it does not already exists
        if (!(wAction == 2 && wStatusSaved == 1)){
            document.getElementById("watchedjs").setAttribute('src', unescape(wSrc));
        }
    } catch(e) { }
}

function watchedCallback(wAction, wStatus) {
    var watchedStr = wStatus == 1 ? "Yes" : "No";
    wStatusSaved = wStatus;
    if ( wStatus > 1) {
        watchedStr = "Unknown: " + wStatus + "::" + wAction;
    }

    try {
        // This is were you would do some Dynamic stuff, change text, pic etc
        document.getElementById("watched").firstChild.nodeValue = watchedStr;
        document.getElementById("watchedjs").setAttribute('src', "empty.js");
    } catch(e) { }
}
