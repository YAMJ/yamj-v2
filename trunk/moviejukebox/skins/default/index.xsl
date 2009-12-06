<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:include href="preferences.xsl"/>

<xsl:include href="skin-options.xsl"/>

<xsl:template match="/">
<xsl:variable name="currentIndex" select="//index[@current='true']/@currentIndex"/>
<xsl:variable name="lastIndex" select="//index[@current='true']/@lastIndex"/>
<xsl:variable name="nbCols" select="//@cols"/>
<xsl:variable name="nbLines" select="ceiling(count(library/movies/movie) div $nbCols)"/>
<html>
<head>
<link rel="StyleSheet" type="text/css" href="exportindex_item_pch.css"></link>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<title>MovieJukebox</title>
<script type="text/javascript">
var cols = <xsl:value-of select="$nbCols"/>;
var rows = <xsl:value-of select="$nbLines"/>;
var nmov = <xsl:value-of select="count(library/movies/movie)"/>;
<xsl:text disable-output-escaping="yes">
//<![CDATA[
  var title = 1;
  var play = 1;
  var pgup = 1, pgupload = 1;
  var pgupHref;
  var pgdn = 1, pgdnload = 1;
  var pgdnHref;
  function bind() {
    if ( title == 1 ) title = document.getElementById('title');
    if ( play == 1 ) play = document.getElementById('play');
    if ( pgup == 1 ) {
    	pgup = document.getElementById('pgup');
    	pgupload = document.getElementById('pgupload');
    	pgupHref = (pgup == null) ? null : pgup.getAttribute('href');
    	}
    if ( pgdn == 1 ) {
    	pgdn = document.getElementById('pgdn');
    	pgdnload = document.getElementById('pgdnload');
    	pgdnHref = (pgdn == null) ? null : pgdn.getAttribute('href');
    	}
  }
  
  function show(x, jsp) {
    title.firstChild.nodeValue = document.getElementById('title'+x).firstChild.nodeValue;
    play.setAttribute('href', jsp);
    play.setAttribute('tvid', 'PLAY');
    if (pgup != null) pgup.setAttribute('href', pgupHref + '?t=u1&s=' + x);
    if (pgdn != null) pgdn.setAttribute('href', pgdnHref + '?t=d1&s=' + x);
    if (pgupload != null) pgupload.setAttribute('href', pgupHref + '?t=u2&s=' + x);
    if (pgdnload != null) pgdnload.setAttribute('href', pgdnHref + '?t=d2&s=' + x);
  }
  
  function hide(x) {
    title.firstChild.nodeValue = "";
    play.setAttribute('tvid', '#');
  }
  
  function init() {
  	bind();
  	
	if (cols <= 1 || rows < 1) return;

 	var l = "" + location.href;
 	var it = l.indexOf("?t=");
 	var is = l.indexOf("&s=");

 	if (it <= 0 || is <= 0 || is < it) return;
 	
	try {
		var x = parseInt(l.substr(is + 3));
		var t = l.substr(it + 3, 2).toLowerCase();
		var c = ((x - 1) % cols) + 1;
		
		if (t == "u2") {
			x =	((rows - 1) * cols) + c;
		} else if (t == "d2") {
			x = c;
		}
		
		if (x > nmov) {
			if (nmov <= c) x = nmov;
			else x = (Math.floor(nmov / cols) * cols) + c;
		}
		
		document.body.setAttribute('onloadset', "" + x);
	} catch (e) {}
  }
//]]>
</xsl:text>
</script>
</head>
<body bgproperties="fixed" background="pictures/background.jpg" onloadset="1">

<a>
    <xsl:attribute name="href"></xsl:attribute>
    <xsl:attribute name="tvid">#</xsl:attribute>
    <xsl:attribute name="id">play</xsl:attribute> 
    <xsl:attribute name="vod">playlist</xsl:attribute>
</a>
        
<table class="main" border="0" cellpadding="0" cellspacing="0">
  <tr valign="top">
    <td COLSPAN="2" align="center"> 
    	<xsl:apply-templates select="library/category[@name='Title']" mode="t9TitleNavigation"/>
	</td>
  </tr>
  <tr align="left" valign="top">
    <td width="120">
      <table class="categories" border="0">
        <xsl:for-each select="library/category[@name='Other']/index">
          <tr valign="top"><td align="right">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute>
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute>
            <xsl:if test="@current = 'true'"><xsl:attribute name="class">current</xsl:attribute></xsl:if>
            <!-- xsl:if test="position() = 1"><xsl:attribute name="tvid">0</xsl:attribute></xsl:if-->
            <xsl:value-of select="@name" />
            </a>
            <!-- xsl:if test="position() = 1"><small>0</small></xsl:if-->
          </td></tr>
        </xsl:for-each>
        <xsl:for-each select="library/category[@name='Genres']/index">
          <tr valign="top"><td align="right">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute>
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute>
            <xsl:if test="@current = 'true'"><xsl:attribute name="class">current</xsl:attribute></xsl:if>
            <xsl:value-of select="@name" />
            </a>
          </td></tr>
        </xsl:for-each>
        <xsl:for-each select="library/category[@name='Library']/index">
          <tr valign="top"><td align="right">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute>
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute>
            <xsl:if test="@current = 'true'"><xsl:attribute name="class">current</xsl:attribute></xsl:if>
            <xsl:value-of select="@name" />
            </a>
          </td></tr>
        </xsl:for-each>
        <tr><td><hr><xsl:text> </xsl:text></hr></td></tr>

        <xsl:if test="$lastIndex != 1">
           <tr><td align="right"><table><tr>
             <td valign="center">
              <div class="counter"><xsl:value-of select="$currentIndex"/> / <xsl:value-of select="$lastIndex" /></div></td>
             <td valign="top">
                 <a name="pgdn" tvid="pgdn" id="pgdn"><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@next" />.html</xsl:attribute><img src="pictures/nav_down.png" /></a>
                 <a name="pgup" tvid="pgup" id="pgup"><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@previous" />.html</xsl:attribute><img src="pictures/nav_up.png" /></a>
             </td>
             </tr></table>
           </td></tr>
        </xsl:if>

      </table>
    </td>
    <td>
      <table layout="fixed" class="movies" border="0" width="100%">
        <xsl:for-each select="library/movies/movie[$nbCols = 1 or position() mod $nbCols = 1]">
          <tr>
            <xsl:apply-templates
                 select=".|following-sibling::movie[position() &lt; $nbCols]">
              <xsl:with-param name="gap" select="(position() - 1) * $nbCols" />
              <xsl:with-param name="currentIndex" select="$currentIndex" />
              <xsl:with-param name="lastIndex" select="$lastIndex" />
              <xsl:with-param name="lastGap" select="($nbLines - 1) * $nbCols" />
            </xsl:apply-templates>
            <xsl:if test="count(following-sibling::movie[position() &lt; $nbCols]) &lt; ($nbCols - 1)">
            	<td>
            		<xsl:attribute name="colspan"><xsl:value-of select="($nbCols - 1) - count(following-sibling::movie[position() &lt; $nbCols])" /></xsl:attribute>
            		<xsl:attribute name="width"><xsl:value-of select="($nbCols - 1) - count(following-sibling::movie[position() &lt; $nbCols])" /></xsl:attribute>
            		<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            	</td>
            </xsl:if>
          </tr>
        </xsl:for-each>
      </table><br/>
      <table class="title" width="100%"><tr><td id="title" align="center">&#160;</td></tr></table>
    </td>
  </tr>
</table>
  <xsl:for-each select="library/movies/movie">
    <div class="title">
      <xsl:attribute name="id">title<xsl:value-of select="position()"/></xsl:attribute>
      <xsl:value-of select="title"/>
      <xsl:choose>
        <xsl:when test="season > 0"> Season <xsl:value-of select="season"/></xsl:when>
        <xsl:when test="season = 0"> Specials</xsl:when>
      </xsl:choose>
      <xsl:if test="year != '' and year != 'UNKNOWN'">
        <xsl:if test="season = -1 and $skin-year = 'true'"> - <xsl:value-of select="year" /></xsl:if>
        <xsl:if test="season != -1 and $skin-yearTV = 'true'"> - <xsl:value-of select="year" /></xsl:if>
      </xsl:if>
      <xsl:if test="$skin-certification = 'true' and certification != '' and certification != 'UNKNOWN'"> (<xsl:value-of select="certification" />)</xsl:if>
    </div>
  </xsl:for-each>
  <div class="title">
    <a TVID="HOME"><xsl:attribute name="href"><xsl:value-of select="$homePage"/></xsl:attribute>Home</a>
    <a name="pgdnload" onfocusload="" id="pgdnload"><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@next" />.html</xsl:attribute></a>
    <a name="pgupload" onfocusload="" id="pgupload"><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@previous" />.html</xsl:attribute></a>
  </div>

	<script type="text/javascript" defer="defer">
	init();
	</script>
</body>

</html>
</xsl:template>

<xsl:template match="movie">
  <xsl:param name="gap" />
  <xsl:param name="currentIndex" />
  <xsl:param name="lastIndex" />
  <xsl:param name="lastGap" />
     <td width="1">
        <a>
          <xsl:attribute name="href"><xsl:value-of select="details"/></xsl:attribute>
          <xsl:attribute name="name"><xsl:value-of select="position()+$gap"/></xsl:attribute>
          <xsl:attribute name="id">movie<xsl:value-of select="position()+$gap"/></xsl:attribute>
          <xsl:attribute name="onfocus">show(<xsl:value-of select="position()+$gap"/>
            <xsl:text>, '</xsl:text>
            <xsl:value-of>
              <xsl:call-template name="jsEscapeSingleQuotes">
                <xsl:with-param name="string" select="baseFilename"/>
              </xsl:call-template>
            </xsl:value-of>
            <xsl:text>.playlist.jsp')</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="onblur">hide(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
          <xsl:if test="$lastIndex != 1">
            <xsl:if test="$gap=0 and $currentIndex != 1">
              <xsl:attribute name="onkeyupset">pgupload</xsl:attribute>
            </xsl:if>
            <xsl:if test="$gap=$lastGap and $currentIndex != $lastIndex">
              <xsl:attribute name="onkeydownset">pgdnload</xsl:attribute>
            </xsl:if>
          </xsl:if>
          <img>
            <xsl:attribute name="src"><xsl:value-of select="thumbnail"/></xsl:attribute>
            <xsl:attribute name="onmouseover">show(<xsl:value-of select="position()+$gap"/>
              <xsl:text>, '</xsl:text>
              <xsl:value-of>
                <xsl:call-template name="jsEscapeSingleQuotes">
                  <xsl:with-param name="string" select="baseFilename"/>
                </xsl:call-template>
              </xsl:value-of>
              <xsl:text>.playlist.jsp')</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="onmouseout">hide(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
          </img>
        </a>
     </td>
</xsl:template>

<xsl:template mode="t9TitleNavigation" match="category[@name='Title']">
	<!-- 1-0...9 2-ABC 3-DEF 4-GHI 5-JKL 6-MNO 7-PQRS 8-TUV 9-WXYZ-->

	<table id="t9">
	<tr>
		<xsl:call-template name="letter">
			<xsl:with-param name="name">09</xsl:with-param>
			<xsl:with-param name="navigate">1</xsl:with-param>
			<xsl:with-param name="neighbors">09</xsl:with-param>
		</xsl:call-template>
		
		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">ABC</xsl:with-param>
			<xsl:with-param name="navigate">2</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">DEF</xsl:with-param>
			<xsl:with-param name="navigate">3</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">GHI</xsl:with-param>
			<xsl:with-param name="navigate">4</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">JKL</xsl:with-param>
			<xsl:with-param name="navigate">5</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">MNO</xsl:with-param>
			<xsl:with-param name="navigate">6</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">PQRS</xsl:with-param>
			<xsl:with-param name="navigate">7</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">TUV</xsl:with-param>
			<xsl:with-param name="navigate">8</xsl:with-param>
		</xsl:call-template>

		<td class="separator"></td>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters">WXYZ</xsl:with-param>
			<xsl:with-param name="navigate">9</xsl:with-param>
		</xsl:call-template>
	</tr>
	</table>
</xsl:template>


<xsl:template name="lettersgroup">
	<xsl:param name="letters"/>
	<xsl:param name="navigate"/>
	<xsl:param name="neighbors"/>
	
	<xsl:variable name="nb">
		<xsl:choose>
			<xsl:when test="$neighbors"><xsl:value-of select="$neighbors"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$letters"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:if test="string-length($letters) > 0">
		<xsl:call-template name="letter">
			<xsl:with-param name="name" select="substring($letters, 1, 1)"/>
			<xsl:with-param name="navigate" select="$navigate"/>
			<xsl:with-param name="neighbors" select="$nb"/>
		</xsl:call-template>

		<xsl:call-template name="lettersgroup">
			<xsl:with-param name="letters"><xsl:value-of select="substring($letters, 2)"/></xsl:with-param>
			<xsl:with-param name="navigate" select="$navigate"/>
			<xsl:with-param name="neighbors" select="$nb"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>


<xsl:template name="letter">
	<xsl:param name="name"/>
	<xsl:param name="navigate"/>
	<xsl:param name="neighbors"/>
	
	<td>

    <xsl:variable name="lastcurrent" select="index[contains($neighbors, @name)][last()]/@current" />
	
	<xsl:choose>
		<xsl:when test="index[@name=$name]">
			<xsl:for-each select="index[@name=$name]">
			
			<xsl:variable name="tvid">
				<xsl:if test="preceding-sibling::*[position() = 1 and contains($neighbors, @name) and @current]">
		        	<xsl:value-of select="normalize-space($navigate)" />
				</xsl:if>
		        <xsl:if test="not(preceding-sibling::*[contains($neighbors, @name)])">
			        <xsl:if test="$lastcurrent or (not(@current) and not(following-sibling::*[contains($neighbors, @name) and @current]))">
			        	<xsl:value-of select="normalize-space($navigate)" />
			        </xsl:if>
		        </xsl:if>
			</xsl:variable>
	        <a>
		        <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute>
		        <xsl:if test="@current"><xsl:attribute name="class">current</xsl:attribute></xsl:if>
		        <xsl:if test="string-length($tvid) > 0"><xsl:attribute name="tvid"><xsl:value-of select="$tvid" /></xsl:attribute></xsl:if>
		        <xsl:value-of select="@name" />
	        </a>
        	<xsl:if test="string-length($tvid) > 0"><small><xsl:value-of select="$tvid" /></small></xsl:if>
			</xsl:for-each>
		</xsl:when>
		<xsl:otherwise><span class="a"><xsl:value-of select="$name" /></span></xsl:otherwise>
	</xsl:choose>

	</td>
	
</xsl:template>

<!-- http://www.dpawson.co.uk/xsl/sect4/N9745.html#d15577e189 -->
<xsl:template name="jsEscapeSingleQuotes">
  <xsl:param name="string"/>
  
  <xsl:choose>
    <xsl:when test="contains($string, &quot;'&quot;)">
      <xsl:value-of
        select="substring-before($string, &quot;'&quot;)"/>
      <xsl:text>\'</xsl:text>
      <xsl:call-template name="jsEscapeSingleQuotes">
        <xsl:with-param name="string"
          select="substring-after($string, &quot;'&quot;)"/> <!-- '" Fix syntax highlighting -->
      </xsl:call-template>
     </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
