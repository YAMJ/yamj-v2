<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
<head>
<link rel="StyleSheet" type="text/css" href="exportindex_item_pch.css"></link>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<title>Popcorn</title>
</head>
<body>
	<div class="navigation" align="center">-       
		<xsl:for-each select="library/indexes/index">
       		<a>
			<xsl:attribute name="href"><xsl:value-of select="." />.html</xsl:attribute> 
			<xsl:value-of select="@name" />
			</a> - 
		</xsl:for-each>
	</div>

	<table>
		<xsl:for-each select="library/movies/movie">
			<xsl:if test="(position() mod 7) = 1"><tr></tr></xsl:if>
			<td>
	 	    <a id="thumbimage">
				<xsl:attribute name="href"><xsl:value-of select="details"/>.html</xsl:attribute>
				<xsl:attribute name="TVID"><xsl:value-of select="position()"/></xsl:attribute> 
				<img><xsl:attribute name="src"><xsl:value-of select="details"/>_small.jpg</xsl:attribute></img>
			</a>
			<center><xsl:value-of select="position()"/><xsl:text>. </xsl:text><xsl:value-of select="title"/></center>
			</td>
		</xsl:for-each>
	</table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
