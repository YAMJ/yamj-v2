<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:include href="preferences.xsl"/>

<xsl:include href="skin-options.xsl"/>

<xsl:include href="../functions.xsl"/>

<xsl:template match="details/preferences"></xsl:template>

<xsl:template match="details/movie">
<html>
<head>
  <link rel="StyleSheet" type="text/css" href="exportdetails_item_popcorn.css"></link>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <title><xsl:value-of select="title"/></title>
</head>

<body bgproperties="fixed" background="pictures/background.jpg" onloadset="Play">
<xsl:attribute name="onloadset"><xsl:value-of select="//index[@current='true']/@name"/></xsl:attribute>

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <xsl:choose>
    <xsl:when test="$use-fanart='true'">
      <xsl:attribute name="background"><xsl:value-of select="fanartFile"/></xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
      <xsl:attribute name="background">pictures/background.jpg</xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>

  <tr>
    <td align="center" colspan="2">
      <!-- Navigation using remote keys: Home, PageUP/PageDown (Previous/Next) -->
      <a>
        <xsl:attribute name="TVID">HOME</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="$homePage" /></xsl:attribute>
      </a>
      <a TVID="PGDN">
        <xsl:attribute name="href"><xsl:choose><xsl:when
          test="contains(next,'UNKNOWN')"><xsl:value-of select="first" />.html</xsl:when><xsl:otherwise><xsl:value-of
          select="next" />.html</xsl:otherwise></xsl:choose></xsl:attribute>
      </a>
      <a TVID="PGUP">
        <xsl:attribute name="href"><xsl:choose><xsl:when
          test="contains(previous,'UNKNOWN')"><xsl:value-of select="last" />.html</xsl:when><xsl:otherwise><xsl:value-of
          select="previous" />.html</xsl:otherwise></xsl:choose></xsl:attribute>
      </a>
    </td>
  </tr>

  <tr align="left" valign="top">
    <td width="205px">
       <img width="200"><xsl:attribute name="src"><xsl:value-of select="detailPosterFile" /></xsl:attribute></img>
    </td>
    
    <td>
      <table border="0" width="100%">
        <xsl:if test="$use-fanart='true'">
          <xsl:attribute name="bgcolor">black-alpha2</xsl:attribute>
        </xsl:if>
        <tr>
          <td class="title1" valign="top" colspan="2">
            <xsl:value-of select="title"/> 
            <xsl:if test="season &gt; 0"> Season <xsl:value-of select="season" /></xsl:if>
            <xsl:if test="season = 0"> Specials</xsl:if>
            <xsl:if test="year != 'UNKNOWN'">
              <xsl:text> (</xsl:text>
              <xsl:choose>
                <xsl:when test="year/@index != ''">
                  <a>
                    <xsl:attribute name="href"><xsl:value-of select="year/@index" />.html</xsl:attribute>
                    <xsl:value-of select="year" />
                  </a>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="year" />
                </xsl:otherwise>
              </xsl:choose>
              <xsl:text>) </xsl:text>
            </xsl:if>
          </td>
        </tr>
        <tr>
          <td class="title2">
            <xsl:if test="rating != -1">
              <img><xsl:attribute name="src">pictures/rating_<xsl:value-of select="round(rating div 10)*10" />.png</xsl:attribute></img>
            </xsl:if>
            <xsl:if test="top250 != -1">
                <xsl:text>&#160;&#160;</xsl:text>Top 250: #<xsl:value-of select="top250" />
            </xsl:if>
          </td>
        </tr>
        <tr>
          <td class="title2" valign="top" colspan="2">
            <xsl:if test="director != 'UNKNOWN'">
              <xsl:choose>
                <xsl:when test="director/@index != ''">
                  <a>
                    <xsl:attribute name="href"><xsl:value-of select="director/@index" />.html</xsl:attribute>
                    <xsl:value-of select="director" /> 
                  </a>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="director" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
            <xsl:if test="company != 'UNKNOWN'">
              <xsl:if test="director != 'UNKNOWN'">, </xsl:if>
              <xsl:value-of select="company" /> 
            </xsl:if>
            <xsl:if test="country != 'UNKNOWN'">
              <xsl:text>(</xsl:text>
              <xsl:choose>
                <xsl:when test="country != 'UNKNOWN' and country/@index != ''">
                  <a>
                    <xsl:attribute name="href"><xsl:value-of select="country/@index" />.html</xsl:attribute>
                    <xsl:value-of select="country" />
                  </a>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="country" />
                </xsl:otherwise>
              </xsl:choose>
              <xsl:text>)</xsl:text>
            </xsl:if>
          </td>
        </tr>

        <tr>
          <td class="title2" valign="top" colspan="2">
            <xsl:if test="count(genres) != 0">
              <xsl:for-each select="genres/genre[position() &lt;= $genres.max]">
                <xsl:if test="position()!= 1">, </xsl:if>
                <xsl:choose>
                  <xsl:when test="@index != ''">
                    <a>
                      <xsl:attribute name="href"><xsl:value-of select="@index" />.html</xsl:attribute>
                      <xsl:value-of select="." /> 
                    </a>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="." /> 
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </xsl:if>
            <xsl:if test="runtime != 'UNKNOWN'">
              <xsl:if test="count(genres) != 0">, </xsl:if>
              <xsl:value-of select="runtime" /> 
            </xsl:if>
            <xsl:if test="certification != 'UNKNOWN'">
              <xsl:if test="count(genres) != 0 or runtime != 'UNKNOWN'">, </xsl:if>
              <xsl:value-of select="certification" /> 
            </xsl:if>
            <xsl:if test="language != 'UNKNOWN'">
              <xsl:if test="count(genres) != 0 or runtime != 'UNKNOWN' or certification != 'UNKNOWN'">, </xsl:if>
              <xsl:value-of select="language" /> 
            </xsl:if>
          </td>
        </tr>

        <tr><td><hr/></td></tr>

        <tr>
          <td width="100%" class="normal" colspan="2">
            <xsl:if test="plot != 'UNKNOWN'">
               <xsl:variable name="plotLinebreakPreserved">
                <xsl:call-template name="PreserveLineBreaks">
                  <xsl:with-param name="text" select="plot"/>
                </xsl:call-template>
              </xsl:variable>
              
                <xsl:call-template name="string-replace-plot-BR">
                      <xsl:with-param name="text" select='$plotLinebreakPreserved' />
                </xsl:call-template>
              
            </xsl:if>
          </td>
        </tr>

        <tr height="12"><td> </td></tr>

        <tr>
          <td colspan="2">
          <center><table width="100%">
            <tr>
              <td class="title3" width="5%">Source</td>
              <td class="normal" width="45%"><xsl:value-of select="videoSource" /></td>
              <td class="title3" width="5%">Subtitles</td>
              <td class="normal" width="45%"><xsl:value-of select="subtitles" /></td>
            </tr>
            <tr>
              <td class="title3" width="5%">System</td>
              <td class="normal" width="45%"><xsl:value-of select="container" /></td>
              <td class="title3" width="5%">Dimension</td>
              <td class="normal" width="45%"><xsl:value-of select="resolution" /></td>
            </tr>
            <tr>
              <td class="title3" width="5%">Video</td>
              <td class="normal" width="45%"><xsl:value-of select="videoCodec" /></td>
              <td class="title3" width="5%">Output</td>
              <td class="normal" width="45%"><xsl:value-of select="videoOutput" /></td>
            </tr>
            <tr>
              <td class="title3" width="5%">Audio</td>
              <td class="normal" width="45%"><xsl:value-of select="audioCodec" /></td>
              <td class="title3" width="5%">FPS</td>
              <td class="normal" width="45%"><xsl:value-of select="fps" /></td>
            </tr>
            <tr>
              <td class="title3" width="5%" valign="top">Channels</td>
              <td class="normal" width="45%" valign="top"><xsl:value-of select="audioChannels" /></td>
              <td class="title3" width="5%" valign="top">
                <xsl:if test="libraryDescription != 'UNKNOWN'">
                  Library
                </xsl:if>
              </td>
              <td class="normal" width="45%" valign="top">
                <xsl:if test="libraryDescription != 'UNKNOWN'">
                  <xsl:value-of select="libraryDescription" />
                </xsl:if>
              </td>
            </tr>
          </table></center>
          </td>
        </tr>
        
        <tr height="10"><td> </td></tr>

        <xsl:variable name="episodeSortOrder" select="if ($skin-reverseEpisodeOrder='true' and /details/movie/season != -1) then 'descending' else 'ascending'" />
        <xsl:choose>
          <xsl:when test="count(files/file) = 1">
            <xsl:for-each select="files/file">
              <tr valign="top">
                <xsl:if test="//movie/season != -1">
                  <td align="right" class="normal">
                    <xsl:value-of select="@firstPart"/><xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/></xsl:if>.
                  </td>
                </xsl:if>
                <td align="center">
                  <a class="normal">
                    <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                    <xsl:attribute name="TVID">Play</xsl:attribute>
                    <xsl:attribute name="name">Play</xsl:attribute>
                    
                    <xsl:if test="@vod"><xsl:attribute name="vod"><xsl:value-of select="@vod" /></xsl:attribute></xsl:if>
                    <xsl:if test="@zcd"><xsl:attribute name="zcd"><xsl:value-of select="@zcd" /></xsl:attribute></xsl:if>
                    <xsl:if test="@rar"><xsl:attribute name="rar"><xsl:value-of select="@rar" /></xsl:attribute></xsl:if>

                    <xsl:if test="//movie/prebuf != -1">
                        <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                    </xsl:if>

                    <xsl:choose>
                      <xsl:when test="//movie/season = -1">
                        <img src="pictures/play.png" onfocussrc="pictures/play_selected.png"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="middle"/>
                        <xsl:choose>
                          <xsl:when test="@title='UNKNOWN'">
                            <xsl:choose>
                              <xsl:when test="@firstPart!=@lastPart">
                                Episodes <xsl:value-of select="@firstPart"/> - <xsl:value-of select="@lastPart"/>
                              </xsl:when>
                              <xsl:otherwise>
                                Episode <xsl:value-of select="@firstPart"/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </xsl:when>
                          <xsl:otherwise><xsl:value-of select="@title"/></xsl:otherwise>
                        </xsl:choose>
                      </xsl:otherwise>
                    </xsl:choose>
                  </a>
                </td>
              </tr>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="//movie/container = 'BDAV' and //movie/season = -1">
            <tr>
              <td>
                <center>
                  <a class="link">
                    <xsl:attribute name="href">
                      <xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" />
                    </xsl:attribute>
                    <xsl:attribute name="TVID">Play</xsl:attribute>
                    <xsl:attribute name="name">Play</xsl:attribute>
                    
                    <xsl:attribute name="vod">playlist</xsl:attribute>
                    <img src="pictures/play.png" onfocussrc="pictures/play_selected.png"/>
                  </a>
                </center>
              </td>
            </tr>
          </xsl:when>

          <xsl:otherwise>
            <tr>
              <td>
                <table>
                  <xsl:if test="($skin-playAllMovie = 'top' and /details/movie/season = -1) or ($skin-playAllTV = 'top' and /details/movie/season != -1)">
                    <tr>
                      <td>&#160;</td>
                      <td class="normal">
                        <a class="link">
                          <xsl:attribute name="href"><xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" /></xsl:attribute>
                          <xsl:attribute name="vod">playlist</xsl:attribute>
                          <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>
                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="middle"/>
                          <xsl:text>&#160;</xsl:text><xsl:value-of select="$skin-playAllText" />
                        </a>
                      </td>
                    </tr>
                  </xsl:if>
                  <xsl:for-each select="files/file">
                    <xsl:sort select="@firstPart" data-type="number" order="{$episodeSortOrder}"/>
                    <tr valign="top">
                      <td align="right" class="normal">
                        <xsl:value-of select="@firstPart"/><xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/></xsl:if>.
                      </td>
                      <td class="normal">
                        <a class="link">
                          <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>

                          <xsl:choose>
                            <xsl:when test="position() = 1">
                              <xsl:attribute name="TVID">Play</xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:attribute name="TVID"><xsl:value-of select="position()"/></xsl:attribute>
                            </xsl:otherwise>            
                          </xsl:choose> 

                          <xsl:if test="@vod"><xsl:attribute name="vod"><xsl:value-of select="@vod" /></xsl:attribute></xsl:if>
                          <xsl:if test="@zcd"><xsl:attribute name="zcd"><xsl:value-of select="@zcd" /></xsl:attribute></xsl:if>
                          <xsl:if test="@rar"><xsl:attribute name="rar"><xsl:value-of select="@rar" /></xsl:attribute></xsl:if>

                          <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>

                          <xsl:if test="position() = 1">
                            <xsl:attribute name="name">Play</xsl:attribute>
                          </xsl:if>

                          <xsl:if test="position() = 1">
                            <xsl:attribute name="class">firstMovie</xsl:attribute>
                          </xsl:if>

                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="middle"/>

                          <xsl:text>&#160;</xsl:text>

                          <xsl:choose>
                            <xsl:when test="@title='UNKNOWN'">
                              <xsl:choose>
                                <xsl:when test="@firstPart!=@lastPart">
                                  <xsl:choose>
                                    <xsl:when test="/details/movie/season != -1">Episodes </xsl:when>
                                    <xsl:otherwise>Parts </xsl:otherwise>
                                  </xsl:choose>
                                  <xsl:value-of select="@firstPart"/> - <xsl:value-of select="@lastPart"/>
                                </xsl:when>
                                <xsl:otherwise>
                                  <xsl:choose>
                                  <xsl:when test="/details/movie/season != -1">Episode </xsl:when>
                                  <xsl:otherwise>Part </xsl:otherwise>
                                  </xsl:choose>
                                  <xsl:value-of select="@firstPart"/>
                                </xsl:otherwise>
                              </xsl:choose>
                              <xsl:value-of select="@part"/>
                            </xsl:when> 
                            <xsl:otherwise><xsl:value-of select="@title"/></xsl:otherwise>
                          </xsl:choose>
                        </a>
                      </td>
                    </tr>
                  </xsl:for-each>
                  <xsl:if test="($skin-playAllMovie = 'bottom' and /details/movie/season = -1) or ($skin-playAllTV = 'bottom' and /details/movie/season != -1)">
                    <tr>
                      <td>&#160;</td>
                      <td class="normal">
                        <a class="link">
                          <xsl:attribute name="href"><xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" /></xsl:attribute>
                          <xsl:attribute name="vod">playlist</xsl:attribute>
                          <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>
                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="middle"/>
                          <xsl:text>&#160;</xsl:text><xsl:value-of select="$skin-playAllText" />
                        </a>
                      </td>
                    </tr>
                  </xsl:if>
                </table>
              </td>
            </tr>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="count(extras) != 0">
          <tr>
            <td>
              <table>
                <tr><td class="title2">Extras</td></tr>
                <xsl:for-each select="extras/extra">
                  <tr>
                    <td class="normal">
                      <a>
                        <xsl:attribute name="href"><xsl:value-of select="." /></xsl:attribute>

                        <xsl:if test="@vod"><xsl:attribute name="vod"><xsl:value-of select="@vod" /></xsl:attribute></xsl:if>
                        <xsl:if test="@zcd"><xsl:attribute name="zcd"><xsl:value-of select="@zcd" /></xsl:attribute></xsl:if>
                        <xsl:if test="@rar"><xsl:attribute name="rar"><xsl:value-of select="@rar" /></xsl:attribute></xsl:if>

                        <xsl:if test="//movie/prebuf != -1">
                          <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                        </xsl:if>
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="middle">
                          <xsl:attribute name="onmouseover">this.src='pictures/play_selected_small.png';</xsl:attribute>
                          <xsl:attribute name="onmouseout">this.src='pictures/play_small.png';</xsl:attribute>
                        </img>
                        <xsl:text>&#160;</xsl:text>
                        <xsl:value-of select="@title"/>
                      </a>
                    </td>
                  </tr>
                </xsl:for-each>
              </table>
            </td>
          </tr>
        </xsl:if>
        
      </table>
    </td>
  </tr>
</table>
</body>
</html>
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
