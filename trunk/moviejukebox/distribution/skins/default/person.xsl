<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:include href="preferences.xsl"/>

<xsl:include href="skin-options.xsl"/>

<xsl:include href="../functions.xsl"/>

<xsl:template match="/">
<xsl:for-each select="details/person">
<html>
<head>
  <link rel="StyleSheet" type="text/css" href="exportdetails_item_popcorn.css"></link>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <title><xsl:value-of select="name"/></title>
</head>

<body bgproperties="fixed" background="pictures/background.jpg" onloadset="Play">

<xsl:attribute name="background">pictures/background.jpg</xsl:attribute>

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <tr height="30">
    <td height="50" align="center">
      <!-- Navigation using remote keys: Home, PageUP/PageDown (Previous/Next) -->
      <a>
        <xsl:attribute name="TVID">HOME</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="$mjb.homePage" /></xsl:attribute>
      </a>
    </td>
  </tr>

  <tr align="left" valign="top">
    <td width="420px">
      <img width="400"><xsl:attribute name="src"><xsl:value-of select="photoFile" /></xsl:attribute></img>
    </td>

    <td>
      <table border="0" width="85%">
        <xsl:if test="$use-fanart='true'">
          <xsl:attribute name="bgcolor">black-alpha2</xsl:attribute>
        </xsl:if>
        <tr>
          <td class="title1" valign="top">
            <xsl:value-of select="name"/> 
          </td>
        </tr>
        <xsl:if test="count(aka/name) != 0">
            <tr>
              <td>
                (<xsl:for-each select="aka/name">
                    <xsl:if test="position()!=1">, </xsl:if>
                    <xsl:value-of select="."/>
                </xsl:for-each>)
              </td>
            </tr>
        </xsl:if>
        <tr>
          <td class="title2" valign="top">
            <xsl:if test="birthday != 'UNKNOWN'">
                <xsl:value-of select="birthday"/>
            </xsl:if>
            <xsl:if test="birthplace != 'UNKNOWN'">
                <xsl:if test="birthday != 'UNKNOWN'">, </xsl:if>
                <xsl:value-of select="birthplace"/>
            </xsl:if>
          </td>
        </tr>
        <tr class="divider"><td><xsl:text> </xsl:text></td></tr>

        <xsl:if test="biography != 'UNKNOWN'">
          <tr>
            <td width="85%">
              <xsl:attribute name="class">
                <xsl:if test="string-length(biography) >= 700">x</xsl:if>
                <xsl:if test="string-length(biography) >= 250">large-</xsl:if>plot</xsl:attribute>

              <xsl:variable name="plotLinebreakPreserved">
                <xsl:call-template name="PreserveLineBreaks">
                  <xsl:with-param name="text" select="biography"/>
                </xsl:call-template>
              </xsl:variable>
              
                <xsl:call-template name="string-replace-plot-BR">
                    <xsl:with-param name="text" select='$plotLinebreakPreserved' />
                </xsl:call-template>
              
            </td>
          </tr>
        </xsl:if>

        <tr class="divider"><td><xsl:text> </xsl:text></td></tr>

        <xsl:if test="knownMovies &gt; 0">
            <tr>
              <td class="title3">
                Known movies: <xsl:value-of select="knownMovies"/>
              </td>
            </tr>
        </xsl:if>

        <xsl:if test="count(filmography/movie) != 0">
            <tr>
                <td class="title3">
                    <xsl:for-each select="filmography/movie">
                        <xsl:if test="position()!=1">, </xsl:if>
                        <xsl:variable name="name">
                            <xsl:value-of select="@name"/>
                            <xsl:text> [</xsl:text>
                            <xsl:choose>
                                <xsl:when test="@department = 'Actors'">
                                    <xsl:value-of select="@character"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@job"/>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:text>]</xsl:text>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test=". != 'UNKNOWN'">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:if test="$mjb.people.folder!=''">../</xsl:if>
                                        <xsl:value-of select="."/>
                                        <xsl:text>.html</xsl:text>
                                    </xsl:attribute>
                                    <xsl:value-of select="$name"/>
                                </a>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$name"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
</xsl:for-each>
</xsl:template>


<xsl:template name="PreserveLineBreaks">
    <xsl:param name="text"/>
    <xsl:choose>
        <xsl:when test="contains($text,'&#xA;')">
            <xsl:value-of select="substring-before($text,'&#xA;')"/>
            <br/>
            <xsl:call-template name="PreserveLineBreaks">
                <xsl:with-param name="text">
                    <xsl:value-of select="substring-after($text,'&#xA;')"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$text"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>
