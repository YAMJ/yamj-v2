<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="preferences.xsl"/>

<xsl:template match="/">
<html>
<head>
<link rel="StyleSheet" type="text/css" href="categories.css"></link>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<title>MovieJukebox</title>
</head>
<body bgproperties="fixed" background="pictures/background.jpg">
<table class="categories" cellpadding="8">
  <tr align="center"><th class="title" colspan="2">Movie Jukebox<hr width="300"/></th></tr>
  <xsl:for-each select="library/category">
    <tr valign="top">
      <th width="10"><xsl:value-of select="@name"/></th>
      <td>
        <xsl:for-each select="index">
          <a><xsl:attribute name="href"><xsl:value-of select="."/>.html</xsl:attribute><xsl:value-of select="@name" /><xsl:if test="position()!=last()">&#160;&#160;&#160;&#160;</xsl:if></a><xsl:text>
            </xsl:text>
        </xsl:for-each>
      </td>
    </tr>
  </xsl:for-each>
</table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
