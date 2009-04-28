<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes"/>

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
  var title = 1;
  function show(x, jsp)
  {
    if ( title == 1 )
      title = document.getElementById('title');
    title.firstChild.nodeValue = document.getElementById('title'+x).firstChild.nodeValue;
    var play = document.getElementById('play');
    play.setAttribute('href', jsp);
    play.setAttribute('tvid', 'PLAY');
  }
  function hide(x)
  {
    if ( title == 1 )
      title = document.getElementById('title');
    title.firstChild.nodeValue = "";
    document.getElementById('play').setAttribute('tvid', '#');
  }
</script>
</head>
<body bgproperties="fixed" background="pictures/background.jpg" onloadset="1">

<a>
    <xsl:attribute name="href"></xsl:attribute>
    <xsl:attribute name="tvid">#</xsl:attribute>
    <xsl:attribute name="id">play</xsl:attribute> 
    <xsl:attribute name="vod">playlist</xsl:attribute>
</a>

<!-- Navigation using remote keys PageUP/PageDown and Prev/Next -->
<table class="header" border="0" cellpadding="0" cellspacing="0">
<tr valign="center" align="center">
<xsl:for-each select="library/category[@current='true']/index[@current='true']">
  <td width="*">
    <a class="heading" tvid="HOME">
      <xsl:attribute name="href"><xsl:value-of select="//preferences/homePage" /></xsl:attribute><xsl:value-of select="../@name" />: <xsl:value-of select="@name" /></a></td>
  <xsl:if test="@lastIndex != 1">
    <!-- a tvid="prev"><xsl:attribute name="href"><xsl:value-of select="@first" />.html</xsl:attribute>&#160;|&lt;&#160;</a -->
    <td class="counter"><xsl:value-of select="@currentIndex" />&#160;of&#160;<xsl:value-of select="@lastIndex" />&#160;</td>
    <td class="nav">
    <xsl:choose>
      <xsl:when test="@currentIndex = 1">
        <img src="pictures/nav_up_inactive.png" />
      </xsl:when>
      <xsl:otherwise>
        <a tvid="pgup">
          <xsl:attribute name="href"><xsl:value-of select="@previous" />.html</xsl:attribute>
          <img src="pictures/nav_up.png"/>
        </a>
      </xsl:otherwise>
    </xsl:choose>
    </td>
    <td class="nav">
      <xsl:choose>
        <xsl:when test="@currentIndex = @lastIndex">
          <img src="pictures/nav_down_inactive.png" />
        </xsl:when>
        <xsl:otherwise>
          <a tvid="pgdn">
            <xsl:attribute name="href"><xsl:value-of select="@next" />.html</xsl:attribute>
            <img src="pictures/nav_down.png" />
          </a>
        </xsl:otherwise>
      </xsl:choose>
    <!-- a tvid="next"><xsl:attribute name="href"><xsl:value-of select="@last" />.html</xsl:attribute>&#160;&gt;|&#160;</a -->
    </td>
  </xsl:if>
</xsl:for-each>
</tr>
</table>

<table class="movies" border="0" cellpadding="0" cellspacing="2">
  <xsl:for-each select="library/movies/movie[position() mod $nbCols = 1]">
    <tr valign="top">
      <xsl:apply-templates select=".|following-sibling::movie[position() &lt; $nbCols]">
        <xsl:with-param name="gap" select="(position() - 1) * $nbCols" />
        <xsl:with-param name="currentIndex" select="$currentIndex" />
        <xsl:with-param name="lastIndex" select="$lastIndex" />
        <xsl:with-param name="lastGap" select="($nbLines - 1) * $nbCols" />
      </xsl:apply-templates>
    </tr>
  </xsl:for-each>
</table>

<table class="title"><tr><td id="title" align="center">&#160;</td></tr></table>
<xsl:for-each select="library/movies/movie">
  <div class="title">
    <xsl:attribute name="id">title<xsl:value-of select="position()" /></xsl:attribute>
    <xsl:if test="season != -1">&quot;</xsl:if><xsl:value-of select="title"/>
    <xsl:if test="season != -1">&quot;
      <xsl:choose>
        <xsl:when test="season > 0"> Season <xsl:value-of select="season"/></xsl:when>
        <xsl:when test="season = 0"> Specials</xsl:when>
      </xsl:choose>
    </xsl:if>
    <xsl:if test="certification!='' and certification!='UNKNOWN'"> (<xsl:value-of select="certification" />)</xsl:if>
  </div>
</xsl:for-each>
  <div class="title">
    <a name="pgdnload" onfocusload=""><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@next" />.html</xsl:attribute></a>
    <a name="pgupload" onfocusload=""><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@previous" />.html</xsl:attribute></a>
  </div>
</body>
</html>
</xsl:template>

<xsl:template match="movie">
  <xsl:param name="gap" />
  <xsl:param name="currentIndex" />
  <xsl:param name="lastIndex" />
  <xsl:param name="lastGap" />
  <td>
    <a>
      <xsl:attribute name="href"><xsl:value-of select="details"/></xsl:attribute>
      <xsl:attribute name="TVID"><xsl:value-of select="position()+$gap"/></xsl:attribute> 
      <xsl:attribute name="name"><xsl:value-of select="position()+$gap"/></xsl:attribute> 
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
