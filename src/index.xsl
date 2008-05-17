<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <HEAD>
    <LINK REL="StyleSheet" TYPE="text/css" HREF="exportindex_item_pch.css"></LINK>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <TITLE>Popcorn</TITLE>
  </HEAD>
  <BODY>
    <div>
      <div class="navigation" align="center">-       
        <xsl:for-each select="library/indexes/index">
        	<a>
          		<xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute> 
          		<xsl:value-of select="@name" />
        	</a> - 
		</xsl:for-each>
      </div>
      <br></br>
	<div>
      <xsl:for-each select="library/movies/movie">
   			<a id="thumbimage">
   			  <xsl:attribute name="href"><xsl:value-of select="details"/>.html</xsl:attribute> 
   			  <xsl:attribute name="title"><xsl:value-of select="title"/></xsl:attribute> 
			  <img>
  				<xsl:attribute name="src"><xsl:value-of select="details"/>_small.jpg</xsl:attribute> 
           	  </img>
          	</a>
		</xsl:for-each>
   	</div>
    </div>
  </BODY>
</html>
</xsl:template>

</xsl:stylesheet>