<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
<head>
<link rel="StyleSheet" type="text/css" href="exportindex_item_pch.css"></link>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<title>MovieJukebox</title>
    <script type="text/javascript">
		function hideTitle(x){
		  document.styleSheets[0].cssRules[x].style.visibility="hidden";
		};
		
		function showTitle(x){
		  document.styleSheets[0].cssRules[x].style.visibility="visible";
                }
    </script>
</head>
<body bgproperties="fixed" background="background.jpg">
<xsl:attribute name="ONLOADSET">
  <xsl:for-each select="library/indexes/index"><xsl:if test="@current='true'"><xsl:value-of select="@name" /></xsl:if></xsl:for-each>
</xsl:attribute>

<!-- Navigation using remote keys PageUP/PageDown and Prev/Next -->
<a tvid="pgup"><xsl:attribute name="href"><xsl:value-of select="//index[@name='previous']" />.html</xsl:attribute></a>
<a tvid="pgdn"><xsl:attribute name="href"><xsl:value-of select="//index[@name='next']" />.html</xsl:attribute></a>
<a tvid="prev"><xsl:attribute name="href"><xsl:value-of select="//index[@name='previous']" />.html</xsl:attribute></a>
<a tvid="next"><xsl:attribute name="href"><xsl:value-of select="//index[@name='next']" />.html</xsl:attribute></a>

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <tr valign="top">
    <td COLSPAN="2" align="center">- 
        <xsl:for-each select="library/indexes/index[@type=1]">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute> 
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute> 
            <xsl:value-of select="@name" />
            </a> - 
        </xsl:for-each>
    </td>
  </tr>
  <tr align="left" valign="top">
    <td width="120">
      <table class="categories" border="0">
        <xsl:for-each select="library/indexes/index[@type=2]">
          <tr valign="top"><td align="right">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute> 
            <xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute> 
            <xsl:value-of select="@name" />
            </a>
          </td></tr> 
        </xsl:for-each>
        <tr><td><hr/></td></tr>

        <xsl:if test="//index[@name='first'] != //index[@name='last']">
           <tr><td align="right"><table><tr>
             <td valign="center">
              <div class="counter"><xsl:value-of select="//index[@name='currentIndex']" /> / <xsl:value-of select="//index[@name='lastIndex']" /></div></td>
             <td valign="top"><img src="nav.png"/></td>
             </tr></table>
           </td></tr>
        </xsl:if>
        
      </table>
    </td>
    <td>
      <table class="movies" border="0">
        <xsl:for-each select="library/movies/movie[position() mod //@cols = 1]">
          <tr>
            <xsl:apply-templates 
                 select=".|following-sibling::movie[position() &lt; //@cols]">
              <xsl:with-param name="gap" select="(position() - 1) * //@cols" />
            </xsl:apply-templates>
          </tr>
        </xsl:for-each>
      </table>
    </td>
  </tr>
</table>
     <xsl:for-each select="library/movies/movie">
           <div class="title">
               <xsl:attribute name="id">title<xsl:value-of select="position()"/></xsl:attribute>
               <xsl:value-of select="title"/>
           </div>
     </xsl:for-each>
</body>
</html>
</xsl:template>

<xsl:template match="movie">
  <xsl:param name="gap" />
     <td>
        <a>
          <xsl:attribute name="href"><xsl:value-of select="details"/></xsl:attribute>
          <xsl:attribute name="TVID"><xsl:value-of select="position()+$gap"/></xsl:attribute> 
          <xsl:attribute name="onfocus">showTitle(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
          <xsl:attribute name="onblur">hideTitle(<xsl:value-of select="position()+$gap"/>)</xsl:attribute>
          <img><xsl:attribute name="src"><xsl:value-of select="thumbnail"/></xsl:attribute></img>
        </a>
     </td>
</xsl:template>
</xsl:stylesheet>
