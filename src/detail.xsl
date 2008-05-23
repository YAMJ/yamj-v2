<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <HEAD>
    <LINK REL="StyleSheet" TYPE="text/css" HREF="exportdetails_item_popcorn.css"></LINK>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <TITLE><xsl:value-of select="movie/titleSort"/></TITLE>
  </HEAD>
  <BODY marginheight="0" marginwidth="0" leftmargin="0" rightmargin="0" topmargin="0" bottommargin="0" border="0" class="bodybg">
    <div align="center">
      <div class="navigation" align="center">
        <div class="navigationline">
        
          <div class="navlink">
                <a><xsl:attribute name="href"><xsl:value-of select="movie/next"/>.html</xsl:attribute>Next</a>
              - <a><xsl:attribute name="href"><xsl:value-of select="movie/previous"/>.html</xsl:attribute>Previous</a>
              - <a href="../index_09AF.html">Up</a>
              - <a><xsl:attribute name="href"><xsl:value-of select="movie/first"/>.html</xsl:attribute>First</a>
              - <a><xsl:attribute name="href"><xsl:value-of select="movie/last"/>.html</xsl:attribute>Last</a>
          </div>
        </div>
      </div>
    </div>
    <xsl:apply-templates/>
  </BODY>
</html>

</xsl:template>
  
<xsl:template match="movie" >
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
      <tr>
        <td class="maintitleleft"></td>
        <td class="maintitle" width="100%">
        		<xsl:value-of select="titleSort"/> 
                <xsl:if test="season!=-1"> Season <xsl:value-of select="season" /></xsl:if>
        		(<xsl:value-of select="year"/>)</td>
        <td class="maintitleright"></td>
        <td align="right" bgcolor="#FFFFFF">
          <div id="formatlogo"></div>
        </td>
      </tr>
    </table>
    <table border="0" cellspacing="1" cellpadding="0" width="100%">
      <tr valign="top">
        <td width="1" class="detailsbg">
          <img width="350">
          		<xsl:attribute name="src">
    				<xsl:value-of select="posterFile" />
  				</xsl:attribute> 
  		  </img>
        </td>
        <td class="detailsbg" valign="top">
          <table width="100%" height="100%" border="0" cellspacing="1" cellpadding="0" >
            <tr valign="top">
              <td colspan="2" class="subtitle" height="17" align="left">Plot</td>
            </tr>
            <tr valign="top">
              <td class="cellalt" colspan="2"><xsl:value-of select="plot" /></td>
            </tr>

            <tr valign="top">
              <td colspan="2" class="celllabel" height="17" align="left"> </td>
            </tr>

            <tr valign="top">
              <td colspan="2" class="subtitle" height="17" align="left">Additional Details</td>
            </tr>

            <tr valign="top">
              <td width="15%" nowrap="1" class="celllabel">Director</td>
              <td class="cellvalue"><xsl:value-of select="director" /></td>
            </tr>
            <tr valign="top">
              <td width="15%" nowrap="1" class="celllabel">Genre</td>
              <td class="cellvalue"><xsl:value-of select="genres" /></td>
            </tr>
            <tr valign="top">
              <td class="celllabel" width="10%" nowrap="1">Rating</td>
              <td class="cellvalue"><xsl:value-of select="rating" /></td>
            </tr>
            <tr valign="top">
              <td class="celllabel" width="10%" nowrap="1">Release Date</td>
              <td class="cellvalue"><xsl:value-of select="releaseDate" /></td>
            </tr>
            <tr valign="top">
              <td class="celllabel" width="10%" nowrap="1">Country</td>
              <td class="cellvalue"><xsl:value-of select="country" /></td>
            </tr>
            <tr valign="top">
              <td class="celllabel" width="10%" nowrap="1">Language</td>
              <td class="cellvalue"><xsl:value-of select="language" /></td>
            </tr>
            <tr valign="top">
              <td class="celllabel" width="10%" nowrap="1">Running Time</td>
              <td class="cellvalue"><xsl:value-of select="runtime" /></td>
            </tr>
            <tr valign="top">
              <td width="15%" nowrap="1" class="celllabel">Company</td>
              <td class="cellvalue"><xsl:value-of select="company" /></td>
            </tr>
            <tr valign="top">
              <td width="15%" nowrap="1" class="celllabel">Subtitles</td>
              <td class="cellvalue"><xsl:value-of select="subtitles" /></td>
            </tr>
            <tr valign="top">
              <td width="15%" nowrap="1" class="celllabel">Video output</td>
              <td class="cellvalue"><xsl:value-of select="videoOutput" /></td>
            </tr>
                        
            <tr valign="top">
              <td colspan="2" class="celllabel" height="17" align="left"> </td>
            </tr>

            <tr valign="top">
              <td colspan="2" class="subtitle" height="17" align="left">Movie files</td>
            </tr>

            <xsl:for-each select="files/file">
            
            <tr valign="top">
              <td class="cellvalue" colspan="2">
             	 <a class="link">
	          		<xsl:attribute name="href"><xsl:value-of select="." /></xsl:attribute> 
  					<xsl:if test="//movie/container = 'ISO'">
	          		  <xsl:attribute name="zcd">2</xsl:attribute> 
  					</xsl:if>
	          		<xsl:attribute name="vod"/> 
	          		<xsl:value-of select="@title" />
  				 </a>
              </td>
            </tr>
            
			</xsl:for-each>
            
          </table>
        </td>
      </tr>
    </table>
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
      <tr></tr>
    </table>
</xsl:template >

</xsl:stylesheet>