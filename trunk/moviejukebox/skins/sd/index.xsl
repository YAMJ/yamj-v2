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
  function show(x)
  {
    if ( title == 1 )
      title = document.getElementById('title');
    title.firstChild.nodeValue = document.getElementById('title'+x).firstChild.nodeValue;
  }
  function hide(x)
  {
    if ( title == 1 )
      title = document.getElementById('title');
    title.firstChild.nodeValue = "";
  }
</script>
</head>
<body bgproperties="fixed" background="pictures/background.jpg">
<xsl:attribute name="ONLOADSET"><xsl:value-of select="library/category/index[@current='true']/@first" /></xsl:attribute>

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <tr valign="top">
    <td COLSPAN="2" align="center"> 
        <xsl:for-each select="library/category[@name='Title']/index">
          <xsl:if test="position()!=1"> - </xsl:if>
          <a>
          <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute>
          <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute>
          <xsl:value-of select="@name" />
          </a>
        </xsl:for-each>
    </td>
  </tr>
  <tr align="left" valign="top">
    <td>
      <table class="categories" border="0">
        <xsl:for-each select="library/category[@name='Genres']/index">
          <tr valign="top"><td align="right">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute> 
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute> 
            <xsl:value-of select="@name" />
            </a>
          </td></tr> 
        </xsl:for-each>
        <tr><td><hr/></td></tr>

        <xsl:if test="$lastIndex != 1">
           <tr><td align="right"><table><tr>
             <td valign="center">
              <div class="counter"><xsl:value-of select="$currentIndex"/> / <xsl:value-of select="$lastIndex" /></div></td>
             <td valign="top">
                <a name="pgdn" tvid="pgdn" ONFOCUSLOAD=""><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@next" />.html</xsl:attribute><img src="pictures/nav_down.png" /></a>
                <a name="pgup" tvid="pgup" ONFOCUSLOAD=""><xsl:attribute name="href"><xsl:value-of select="//index[@current='true']/@previous" />.html</xsl:attribute><img src="pictures/nav_up.png" /></a>
             </td>
             </tr></table>
           </td></tr>
        </xsl:if>
        
      </table>
    </td>
    <td>
      <table class="movies" border="0">
        <xsl:for-each select="library/movies/movie[position() mod $nbCols = 1]">
          <tr>
            <xsl:apply-templates 
                 select=".|following-sibling::movie[position() &lt; $nbCols]">
              <xsl:with-param name="gap" select="(position() - 1) * $nbCols" />
              <xsl:with-param name="currentIndex" select="$currentIndex" />
              <xsl:with-param name="lastIndex" select="$lastIndex" />
              <xsl:with-param name="lastGap" select="($nbLines - 1) * $nbCols" />
            </xsl:apply-templates>
          </tr>
        </xsl:for-each>
      </table><br/>
      <table class="title"><tr><td id="title" align="center">&#160;</td></tr></table>
    </td>
  </tr>
</table>
     <xsl:for-each select="library/movies/movie">
           <div class="title">
               <xsl:attribute name="id">title<xsl:value-of select="position()"/></xsl:attribute>
               <xsl:value-of select="title"/><xsl:if test="season > 0"> Season <xsl:value-of select="season"/></xsl:if> 
           </div>
     </xsl:for-each>
  <div class="title">
    <a TVID="HOME"><xsl:attribute name="href"><xsl:value-of select="/library/preferences/homePage"/></xsl:attribute>Home</a>
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
          <xsl:attribute name="onfocus">show(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
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
            <xsl:attribute name="onmouseover">show(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
            <xsl:attribute name="onmouseout">hide(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
          </img>
        </a>
     </td>
</xsl:template>
</xsl:stylesheet>
