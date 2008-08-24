'==============================================================================
' NAME:		PCH100_JS_GENERATOR.vbs
' REV:		5.3
' DATE:		07/30/2008
' AUTHOR:	Andrew Mil (aka sexhex.ru) | reachable via: sexhex[at]yandex.ru
' Updated:      08/09/2008 by Michael T. Keerl          
' COMMENT:	1)	PCH100_JS_GENERATOR.vbs generates all `sel*.js` files in the folder 
'				where it is placed.
'			2)	It takes MoC TEXT (CSV) export and parses it to 28 `sel?.js` files 
'				(where `?` stands for definitions assigned in arrLetter at line #61)
'			3)	For MISSING letters lists it generates a generic `sel?.js`.
'			4)	It also parses same input list to 14 `sel?.js` files 
'				(where `?` stands for definitions assigned in arrGenre at line #63)
'			5)	It also parses same input list to extract local `movie links` and
'				generate series of `?.jsp` files in \details subdir
'				(where `?` stands for movie-ID and the exact number of links triggering
'				the .JSP generation is defined via con iLinksTrigger on line #37)
'			6)	`movie links` are formated based on con iSubdirsTrigger value (line #38)
'==============================================================================
' USAGE:    Just export from MoC ALL your movies to `!movies.txt` in some folder.
'==============================================================================
'			You need only 7 fields in your export:	[ID] - [Title] - [Title First Letter]
'			- [Movie Release Date] - [Front Cover] - [Movie Files].
'			NB:  !!! THESE  FIELDS  MUST  BE  IN  THIS  EXACT  ORDER !!!
'			Export settings are:
'			Destination file: `X:\..\!movies.txt
'			Sort: `Title`; Include Field Names on First Row: `No`; 
'			Delimiter: `TAB`; Text Qualifier: `Double Quote` (or `None`)
'==============================================================================
'			Put this script in same folder where `!movies.txt` is located and run it.
'			HINT: You can leave this script in this folder to use whenever you wish.
'			Feel free to modify, use at your own risk.
'==============================================================================
Const ForReading = 1
Const ForWriting = 2
'====	[ begin:	USER CFG Section ]	========================================
'<--- .JSP files will be generated ONLY for movies that have exactly this number of movie links
Const iLinksTrigger = 2		'<--- exact number of local movie links required to trigger the .jsp generation
Const iSubdirsTrigger = 1	'<--- 0 = all videos are in mapDriveBaseURI; 1 = all videos are in separate subdirs of mapDriveBaseURI
Const netAddress = "file:///opt/sybhttpd/localhost.drives/"				'<--- Base netAddress part for movie links
Const mapDriveBaseURI = "%5BNFS%5D%20192.168.254.147::media/my movies/"	'<--- Base URI path part for movie links
srcFilePath = rootFolder & "!movies.txt"	'<--- MoC txt export path
delimiter = vbTab							'<--- Text delimiter is a Tab
'====	[ e n d:	USER CFG Section ]	========================================
rootFolder = Mid(WScript.ScriptFullName,1,InStrRev(WScript.ScriptFullName, "\"))	'<--- root dir path
detailsFolder = rootFolder & "details\"												'<--- details dir path
spacer = "====================================================="
strUsage= vbLf & vbLf & vbTab & vbTab & "<==   U S A G E   ==>" & vbLf & vbLf & "Export ALL movies to TEXT from MoC menu:  File | Export to | Text" & vbLf & _
"with the following settings:" & vbLf & vbLf & "Destination file :" & vbTab & srcFilePath  & vbLf & "Movies to export :" & vbTab & "All Movies" & vbLf & _
spacer & vbLf & "Fields :" & vbLf & "1). ID " & vbTab & "2). Title " & vbTab & "3). Title First Letter" & vbTab & "4). Movie Release Year" & vbLf & "5). Front Cover" & vbTab & "6). Genre" & vbTab  & vbTab & "7). Movie Files" & vbLf & vbLf & _
"!!! Note :   These field should be exactly in this order !!!" & vbLf & "Hint:" & vbTab & "Drag the fields with your mouse to set them in correct order." & vbLf & spacer & vbLf & _
"Sort on :" & vbTab & vbTab & vbTab & vbTab & "Title" & vbLf &"Include Field Names on First Row :" & vbTab & "No" & vbLf & _
"Delimiter :" & vbTab & vbTab & vbTab & vbTab & "Tab" & vbLf & "Text Qualifier :" & vbTab & vbTab & vbTab & "Double Quote or None" 
Set objFSO = CreateObject("Scripting.FileSystemObject")
If Not objFSO.FileExists(srcFilePath) Then Call Fail("Movie Collector export [ " & srcFilePath & " ] file not found!" & strUsage)
Set objSrcFile = objFSO.GetFile(srcFilePath)
If objSrcFile.Size > 0 Then
	Set objReadFile = objFSO.OpenTextFile(srcFilePath, 1)
	strContents = Replace(objReadFile.ReadAll,Chr(34),"")
	objReadFile.Close
	AllItems = Split(Left(strContents,Len(strContents)-2), vbCrLf)	'movie data as tab-delimited strings array
	arrLetter = Array("all","0","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z")
	'set your genres here (exactly as they are listed in your MoC db, case sensitive!)
	arrGenre = Array("Action","Animation","Comedy","Crime","Drama","Family","Fantasy","History","Horror","HD","Musical","Other","RealityTV","Sci-Fi","Thriller","TVShow")
	For Each lst In arrLetter	'<--- generate {Letters and Genres lists} .JS series in rootFolder
		Call TxtFileWrite(rootFolder & "sel" & LCase(lst) & ".js", Join(LinesOut(lst,AllItems,2),vbCrLf))
	Next
	For Each lst In arrGenre
		Call TxtFileWrite(rootFolder & "sel" & LCase(Replace(lst,"-","")) & ".js", Join(LinesOut(lst,AllItems,5),vbCrLf))
	Next
	totalLinksJsp = MoviesLinksOut(AllItems, iLinksTrigger)	'<--- generate {Movies' Links} .JSP series in detailsFolder
	logMsg = "{1}. All " & (UBound(arrGenre)+UBound(arrLetter)+2) & " sel*.js files were generated in:" & vbLf & vbLf & rootFolder & vbLf & vbLf & (UBound(ArrLetter)+1) & " Title-1st-letter lists + " & (UBound(arrGenre)+1) & " Genre-lists processed." & vbLf
	If totalLinksJsp > 0 Then logMsg = logMsg & Left(spacer, 40) & vbLf &  vbLf & "{2}. Total of " & totalLinksJsp & " nnn{id}.jsp (multi-disk links files) generated in:" & vbLf & vbLf & detailsFolder
	Call Fail(logMsg)
Else
	Call Fail("Movie Collector export [ " & srcFilePath & " ] file is empty!" & strUsage)
End If
Function FormatMovieLink(mocLink)
	out = netAddress & mapDriveBaseURI
	crumbs = Split(mocLink,"\")
	Select Case iSubdirsTrigger	'<--- Separate subdirs enabled?
	Case 0:
		out = out & crumbs(UBound(crumbs))
	Case 1:
		If UBound(crumbs) > 1 Then
			out = out & crumbs(UBound(crumbs)-1) & "/" & crumbs(UBound(crumbs))
		End if
	End Select
	FormatMovieLink = out
End Function
Function MoviesLinksOut(arrData, iTrigger)
	jCount=0
	For i=LBound(arrData) To UBound(arrData)	'<--- loop on all records
		f=Split(arrData(i),delimiter)'<--- fields in arr f (0:Id; 1:Title; 2:1stLetter; 3:Release; 4:Cover; 5:Genre; 6:MovieLinks)	
		If Len(f(6))>1 Then '<--- has link(s)
			arrLinks = Split(f(6),"; ")'<--- links in arrLinks 
			If (UBound(arrLinks)+1)=iTrigger Then	'<--- switch cond to >= for all multi-disc records
				out = ""
				For j=LBound(arrLinks) To UBound(arrLinks)
					out = out & "CD" & (j+1) & "|0|0|" & FormatMovieLink(Trim(arrLinks(j))) & "|" 
					If j < UBound(arrLinks) Then out = out & vbCrLf
				Next
				jCount = jCount+1
				If jCount=1 And Not objFSO.FolderExists(detailsFolder) Then objFSO.CreateFolder detailsFolder '<--- details dir chk
				Call TxtFileWrite(detailsFolder & f(0) & ".jsp", out)
			End if
		End if
	Next
	MoviesLinksOut=jCount
End Function
Function TestList(szDef,arrVal,iArr)
	boolX = False
	Select case iArr	'<--- Conditional listType-sensitive test
	case 2
		If szDef="all" Or Left(arrVal(2),1) = UCase(szDef) Then boolX = True
	case 5
		If Instr(1,arrVal(5),szDef) <> 0 Then boolX = True
	End Select
	TestList = boolX
End Function
Function LinesOut(list,arrData,iType)
	q=Chr(34)
	arrCount=1
	lCount=0
	Dim arrOut()	'<--- Initializing the array
	Redim Preserve arrOut(arrCount)
        arrOut(0) = "function sel" & LCase(Replace(list,"-","")) & "(){"	'<--- strip dash
	arrOut(1) = ""
	For i=LBound(arrData) To UBound(arrData)
		'<--- fields in arr f (0:Id; 1:Title; 2:1stLetter; 3:Release; 4:Cover; 5:Genre; 6:MovieLinks)
		f=Split(arrData(i),delimiter)
		If TestList(list,f,iType) = True Then
			'<---------- title
			j=arrCount+1
			arrCount=arrCount+3
			Redim Preserve arrOut(arrCount)
			arrOut(j)="title[" & lCount & "]=" & q & f(1)
			If f(3) <> "" Then arrOut(j)=arrOut(j) & " [" & f(3) & "]"
			arrOut(j)=arrOut(j) & q & ";"
			'<---------- image
			j=j+1
			arrOut(j)="images[" & lCount & "]=" & q
			If f(4) <> "" Then arrOut(j)=arrOut(j) & "images/" & f(0) & "r.png"
			arrOut(j)=arrOut(j) & q & ";"
			'<---------- url
			j=j+1
			arrOut(j)="url[" & lCount & "]=" & q
			If f(0) <> "" Then arrOut(j)=arrOut(j) & "javascript:passit('details/" & f(0) & ".html');"
			arrOut(j)=arrOut(j) & q & ";"
			lCount = lCount+1
		End if
	Next
	arrOut(1)="total=" & lCount & ";"
	If lCount=0 Then
		arrCount=arrCount+3
		Redim Preserve arrOut(arrCount)
		arrOut(1)="total=1;"
		arrOut(2)="title[0]=" &q & "There are No Movies for this Selection" & q & ";"
		arrOut(3)="images[0]=" & q & "backgrounds/missing-in-action.png" & q & ";"
		arrOut(4)="url[0]=" &q & q & ";"
	End if
	Redim Preserve arrOut(arrCount+1)
	arrOut(arrCount+1)="}"
	LinesOut=arrOut
End Function
Sub TxtFileWrite(file, txt)
	Set objFile = objFSO.CreateTextFile(file, True)
	objFile.Write txt
	objFile.Close
End Sub
Sub Fail(sMessage)
	WScript.Echo sMessage
	WScript.Quit 0
End Sub