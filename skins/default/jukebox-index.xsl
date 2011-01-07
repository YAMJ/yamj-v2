<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="preferences.xsl"/>

<xsl:template match="/">
<html>
<head>
<link rel="StyleSheet" type="text/css" href="categories.css"></link>
<meta name="YAMJ" content="MovieJukebox" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<title>MovieJukebox</title>
<meta><xsl:attribute name="http-equiv">REFRESH</xsl:attribute> <xsl:attribute name="content">0; url=<xsl:value-of select="index/detailsDirName" />/<xsl:value-of select="index/homePage" />.html</xsl:attribute></meta>

</head>
</html>
</xsl:template>
</xsl:stylesheet>
