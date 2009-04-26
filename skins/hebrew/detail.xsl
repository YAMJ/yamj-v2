<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" omit-xml-declaration="yes"/>

<xsl:template match="/">
<xsl:for-each select="details/movie">
<html>
<head>
  <link rel="StyleSheet" type="text/css" href="exportdetails_item_popcorn.css"></link>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <title><xsl:value-of select="title"/></title>
<script type="text/javascript">
//<![CDATA[
  var title = 1;
  function show(x)
  {
    if ( title == 1 )
      title = document.getElementById('title').firstChild;
    title.nodeValue = document.getElementById('title'+x).firstChild.nodeValue;
  }
  function hide()
  {
    if ( title == 1 )
      title = document.getElementById('title').firstChild;
    title.nodeValue = "-";
  }
//]]>
</script>
</head>

<xsl:variable name="star_rating">true</xsl:variable>
<xsl:variable name="full_rating">true</xsl:variable>

<body bgproperties="fixed" background="pictures/background.jpg">
<!-- xsl:attribute name="onloadset"><xsl:value-of select="//index[@current='true']/@name"/></xsl:attribute-->

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <tr height="30">
    <td height="50" align="center" colspan="2">
      <!-- Navigation using remote keys: Home, PageUP/PageDown (First/Last), Prev/Next & Left/Right (Previous/Next) -->
      <a>
        <xsl:attribute name="TVID">HOME</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="//preferences/homePage" /></xsl:attribute>
      </a>

      <a>
        <xsl:attribute name="TVID">PGUP</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="first" />.html</xsl:attribute>
      </a>
      <a>
        <xsl:attribute name="TVID">PGDN</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="last" />.html</xsl:attribute>
      </a>

      <a name="goright" onfocusload="">
        <xsl:if test="count(files/file) = 1">
          <xsl:attribute name="TVID">RIGHT</xsl:attribute>
        </xsl:if>
        <xsl:attribute name="href">
          <xsl:choose><xsl:when test="contains(next,'UNKNOWN')"><xsl:value-of select="first"/>.html</xsl:when><xsl:otherwise><xsl:value-of select="next"/>.html</xsl:otherwise></xsl:choose>
        </xsl:attribute>
      </a>
      <a name="goleft" onfocusload="">
        <xsl:if test="count(files/file) = 1">
          <xsl:attribute name="TVID">LEFT</xsl:attribute>
        </xsl:if>
        <xsl:attribute name="href">
          <xsl:choose><xsl:when test="contains(previous,'UNKNOWN')"><xsl:value-of select="//preferences/homePage"/></xsl:when><xsl:otherwise><xsl:value-of select="last"/>.html</xsl:otherwise></xsl:choose>
        </xsl:attribute>
      </a>

      <a>
        <xsl:attribute name="TVID">PREV</xsl:attribute>
        <xsl:attribute name="href"><xsl:choose><xsl:when
          test="contains(next,'UNKNOWN')"><xsl:value-of select="first" />.html</xsl:when><xsl:otherwise><xsl:value-of
          select="next" />.html</xsl:otherwise></xsl:choose></xsl:attribute>
      </a>
      <a>
        <xsl:attribute name="TVID">NEXT</xsl:attribute>
        <xsl:attribute name="href"><xsl:choose><xsl:when
          test="contains(previous,'UNKNOWN')"><xsl:value-of select="//preferences/homePage" /></xsl:when><xsl:otherwise><xsl:value-of
          select="last" />.html</xsl:otherwise></xsl:choose></xsl:attribute>
      </a>
    </td>
  </tr>

  <tr align="left" valign="top">
    <td width="420px">
       <img width="400"><xsl:attribute name="src"><xsl:value-of select="detailPosterFile" /></xsl:attribute></img>
    </td>
    
    <td>
      <table border="0" width="95%" align="right">
        <tr>
          <td class="title1" valign="top" colspan="4" align="right">
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
            <xsl:if test="season != -1">
              <xsl:value-of select="season" /> הנוע 
            </xsl:if>
            <xsl:value-of select="title"/>
          </td>
        </tr>
        <xsl:if test="originalTitle != title">
          <tr>
            <td class="title1sub" valign="top" colspan="4" align="right">
              <xsl:value-of select="originalTitle"/>
            </td>
          </tr>
        </xsl:if>
        <tr>
          <td class="title2" valign="top" align="right">
            <xsl:if test="top250 != -1">
              <xsl:text>&#160;&#160;</xsl:text>#<xsl:value-of select="top250" /> :ץלמומ 
            </xsl:if>
            <xsl:if test="rating != -1">
              <xsl:if test="$full_rating = 'true'">
                <xsl:if test="$star_rating = 'true'">
                  (<xsl:value-of select="rating div 10" />/10)
                </xsl:if>
                <xsl:if test="$star_rating = 'false'">
                  IMDB Rating: <xsl:value-of select="rating div 10" />/10
                </xsl:if>
              </xsl:if>
              <xsl:if test="$star_rating = 'true'">
                <img><xsl:attribute name="src">pictures/rating_<xsl:value-of select="round(rating div 10)*10" />.png</xsl:attribute></img>
              </xsl:if>
            </xsl:if>
          </td>
        </tr>
        <tr>
          <td class="title2" valign="top" colspan="4" align="right"> 
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
            <xsl:if test="company != 'UNKNOWN'">
              <xsl:value-of select="company" />
              <xsl:if test="director != 'UNKNOWN'"> ,</xsl:if>
            </xsl:if>
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
          </td>
        </tr>

        <xsl:if test="count(cast/actor) != 0">
          <tr>
            <td class="title2" colspan="4" align="right"> 
              <xsl:for-each select="cast/actor[position() &lt;= //preferences/actors.max]">
                <xsl:if test="position()!=1"> ,</xsl:if>
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
              </xsl:for-each> םע
            </td>
          </tr>
        </xsl:if>

        <tr>
          <td class="title2" valign="top" colspan="3" align="right">
            <xsl:if test="language != 'UNKNOWN'">
              <xsl:value-of select="language" />
              <xsl:if test="count(genres) != 0 or runtime != 'UNKNOWN' or certification != 'UNKNOWN'"> ,</xsl:if>
            </xsl:if>
            <xsl:if test="certification != 'UNKNOWN'">
              <xsl:value-of select="certification" />
              <xsl:if test="count(genres) != 0 or runtime != 'UNKNOWN'"> ,</xsl:if>
            </xsl:if>
            <xsl:if test="runtime != 'UNKNOWN'">
              <xsl:value-of select="runtime" />
              <xsl:if test="count(genres) != 0"> ,</xsl:if>
            </xsl:if>
            <xsl:if test="count(genres) != 0">
              <xsl:for-each select="genres/genre[position() &lt;= //preferences/genres.max]">
                <xsl:if test="position()!= 1"> ,</xsl:if>
                <xsl:value-of select="." />
              </xsl:for-each>
            </xsl:if>
          </td>
        </tr>

        <tr class="divider"><td><xsl:text> </xsl:text></td></tr>

        <tr>
          <td width="95%" class="normal" colspan="4" align="right">
            <xsl:if test="plot != 'UNKNOWN'">
              <xsl:value-of disable-output-escaping="yes" select='translate(plot,"{}","&lt;&gt;")' />
            </xsl:if>
          </td>
        </tr>

        <tr class="spacer"><td><xsl:text> </xsl:text></td></tr>

        <tr>
          <td colspan="4"><center><table width="95%">
            <tr>
              <td class="title3info" width="5%">Source</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="videoSource" /></td>
              <td class="title3info" width="5%">Subtitles</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="subtitles" /></td>
            </tr>
            <tr>
              <td class="title3info" width="5%">System</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="container" /></td>
              <td class="title3info" width="5%">Dimension</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="resolution" /></td>
            </tr>
            <tr>
              <td class="title3info" width="5%">Video</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="videoCodec" /></td>
              <td class="title3info" width="5%">Output</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="videoOutput" /></td>
            </tr>
            <tr>
              <td class="title3info" width="5%">Audio</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="audioCodec" /></td>
              <td class="title3info" width="5%">FPS</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="fps" /></td>
            </tr>
            <tr>
              <td class="title3info" width="5%">Channels</td>
              <td class="normalinfo" width="45%"><xsl:value-of select="audioChannels" /></td>
              <td class="title3info" width="5%"></td>
              <td class="normalinfo" width="45%"></td>
            </tr>
          </table></center></td>
        </tr>
        
        <tr class="spacer" colspan="2"><td> </td></tr>

        <xsl:choose>
          <xsl:when test="count(files/file) = 1">
            <xsl:for-each select="files/file">
              <tr valign="top">
                <td align="center">
                  <a class="link">
                    <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                    <xsl:attribute name="TVID">Play</xsl:attribute>
                     
                    <xsl:call-template name="zcd">
                      <xsl:with-param name="url" select="fileURL"/>
                      <xsl:with-param name="container" select="//movie/container"/>
                    </xsl:call-template>

                    <xsl:attribute name="vod"/>

                    <xsl:if test="//movie/prebuf != -1">
                      <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                    </xsl:if>

                    <xsl:choose>
                      <xsl:when test="//movie/season = -1">
                        <img src="pictures/play.png" onfocussrc="pictures/play_selected.png"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:choose>
                          <xsl:when test="@title='UNKNOWN'">
                            <xsl:choose>
                              <xsl:when test="@firstPart!=@lastPart">
                                          קרפ <xsl:value-of select="@firstPart"/> - <xsl:value-of select="@lastPart"/>
                              </xsl:when>
                              <xsl:otherwise>
                                קרפ <xsl:value-of select="@firstPart"/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </xsl:when>
                          <xsl:otherwise><xsl:value-of select="@title"/></xsl:otherwise>
                        </xsl:choose>
                        <xsl:if test="//movie/season != -1">
                          .<xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/></xsl:if>
                          <xsl:value-of select="@firstPart"/>
                        </xsl:if>
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
                      </xsl:otherwise>
                    </xsl:choose>
                  </a>
                </td>
              </tr>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="//movie/container = 'BDAV'">
            <tr>
              <td align="center">
                <a class="link">
                  <xsl:attribute name="href">
                    <xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" />
                  </xsl:attribute>
                  <xsl:attribute name="TVID">Play</xsl:attribute>

                  <xsl:attribute name="vod">playlist</xsl:attribute>
                  <img src="pictures/play.png" onfocussrc="pictures/play_selected.png"/>
                </a>
              </td>
            </tr>
          </xsl:when>
          <xsl:otherwise>
            <tr>
              <xsl:variable name="episodeSortOrder" select="if (/details/preferences/skin.reverseEpisodeOrder='true' and /details/movie/season != -1) then 'descending' else 'ascending'" />
              <td>
                <table align="right">
                  <tr align="right" valign="top">
                    <td class="normal">
                      <xsl:for-each select="files/file">
                        <xsl:sort select="@firstPart" data-type="number" order="{$episodeSortOrder}"/>
                        <xsl:if test="position() = 1">
                          <a tvid="Play">
                            <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                          </a>
                        </xsl:if>
                        <a class="link">
                          <xsl:if test="position() = 1">
                            <xsl:attribute name="class">firstMovie</xsl:attribute>
                          </xsl:if>

                          &#160;<xsl:value-of select="@firstPart"/>
                          <xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/>
                          </xsl:if> קרפ
                        </a>
                        <a>
                          <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                          <xsl:attribute name="TVID"><xsl:value-of select="@firstPart"/></xsl:attribute>
                          <xsl:attribute name="name">e<xsl:value-of select="position()"/></xsl:attribute>
                          <xsl:attribute name="onkeyleftset">e<xsl:value-of select="position()-1"/></xsl:attribute>
                          <xsl:attribute name="onkeyrightset">e<xsl:value-of select="position()+1"/></xsl:attribute>
                          <xsl:attribute name="onfocus">show(<xsl:value-of select="@firstPart"/>)</xsl:attribute>
                          <xsl:attribute name="onblur">hide()</xsl:attribute>

                          <xsl:call-template name="zcd">
                            <xsl:with-param name="url" select="fileURL"/>
                            <xsl:with-param name="container" select="//movie/container"/>
                          </xsl:call-template>

                          <xsl:attribute name="vod"/>

                          <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>

                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top">
                            <xsl:attribute name="onmouseover">show(<xsl:value-of select="@firstPart"/>);</xsl:attribute>
                            <xsl:attribute name="onmouseout">hide();</xsl:attribute>
                          </img>
                        </a>
                      </xsl:for-each>
                    </td>
                  </tr>
                  <tr align="right">
                    <td class="normal">
                      <div id="title">&#160; </div>
                      <a class="link">
                        <xsl:attribute name="onfocus">hide()</xsl:attribute>
                        <xsl:attribute name="href"><xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" /></xsl:attribute>
                        <xsl:attribute name="vod">playlist</xsl:attribute>
                        <xsl:if test="//movie/prebuf != -1">
                          <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                        </xsl:if>
                        <xsl:text>&#160;</xsl:text>לכה ןגנ
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
                      </a>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="count(trailers) != 0">
          <tr>
            <td>
              <table align="right">
                <tr><td class="title2" align="right">םירליירט</td></tr>
                <xsl:for-each select="trailers/trailer">
                  <tr>
                    <td class="normal">
                      <a>
                        <xsl:attribute name="href"><xsl:value-of select="." /></xsl:attribute>

                        <xsl:call-template name="zcd">
                          <xsl:with-param name="url" select="."/>
                        </xsl:call-template>

                        <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                        </xsl:if>

                        <xsl:attribute name="vod"/>
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
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
 
      <xsl:for-each select="files/file">
        <div class="title">
          <xsl:attribute name="id">title<xsl:value-of select="@firstPart" /></xsl:attribute>
          <xsl:choose>
            <xsl:when test="@title='UNKNOWN'">
              <xsl:choose>
                <xsl:when test="@firstPart!=@lastPart">
                  <xsl:value-of select="@firstPart" /> - <xsl:value-of select="@lastPart" />
                  <xsl:choose>
                    <xsl:when test="/details/movie/season != -1"> םיקרפ</xsl:when>
                    <xsl:otherwise> םיקלח</xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@firstPart" />
                  <xsl:choose>
                    <xsl:when test="/details/movie/season != -1"> קרפ</xsl:when>
                    <xsl:otherwise> קלח</xsl:otherwise>
                  </xsl:choose>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:value-of select="@part" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@title" />
              <xsl:if test="@firstPart!=@lastPart"> -	<xsl:value-of select="@lastPart" /></xsl:if>.
              <xsl:value-of select="@firstPart" />
            </xsl:otherwise>
          </xsl:choose>
        </div>
      </xsl:for-each>
    </td>
  </tr>
</table>
</body>
</html>
</xsl:for-each>
</xsl:template>

<xsl:template name="zcd">
  <xsl:param name="url"/>
  <xsl:param name="container"/>

  <xsl:if test="$container = 'ISO' or ends-with($url, '.ISO') or ends-with($url, '.iso')">
    <xsl:attribute name="zcd">2</xsl:attribute>
  </xsl:if>

  <xsl:if test="$container = 'IMG' or ends-with($url, '.IMG') or ends-with($url, '.img')">
    <xsl:attribute name="zcd">2</xsl:attribute>
  </xsl:if>

  <xsl:if test="ends-with($url, 'VIDEO_TS')">
    <xsl:attribute name="zcd">2</xsl:attribute>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
