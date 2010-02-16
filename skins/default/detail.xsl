<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:include href="preferences.xsl"/>

<xsl:include href="skin-options.xsl"/>

<xsl:include href="../functions.xsl"/>

<xsl:template match="/">
<xsl:for-each select="details/movie">
<html>
<head>
  <link rel="StyleSheet" type="text/css" href="exportdetails_item_popcorn.css"></link>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <title><xsl:value-of select="title"/></title>
<script type="text/javascript">
var baseFilename = "<xsl:value-of select="/details/movie/baseFilename"/>";
//<![CDATA[
  var title = 1;
  var lnk = 1;
  function bind() {
    if ( title == 1 ) title = document.getElementById('title').firstChild;
    if ( lnk == 1 ) lnk = document.getElementById('playLink');
  }
  function show(x) {
    bind();
    title.nodeValue = document.getElementById('title'+x).firstChild.nodeValue;
    if(lnk)lnk.setAttribute('HREF', baseFilename + '.playlist' + x + '.jsp');
  }
  function hide() {
    bind();
    title.nodeValue = "-";
    if(lnk)lnk.setAttribute('HREF', '');
  }
//]]>
</script>
</head>

<xsl:variable name="star_rating">true</xsl:variable>
<xsl:variable name="full_rating">true</xsl:variable>

<body bgproperties="fixed" background="pictures/background.jpg" onloadset="Play">
<!-- xsl:attribute name="onloadset"><xsl:value-of select="//index[@current='true']/@name"/></xsl:attribute-->

<xsl:choose>
  <xsl:when test="$use-fanart='true'">
    <xsl:attribute name="background"><xsl:value-of select="fanartFile"/></xsl:attribute>
  </xsl:when>
  <xsl:otherwise>
    <xsl:attribute name="background">pictures/background.jpg</xsl:attribute>
  </xsl:otherwise>
</xsl:choose>

<table class="main" align="center" border="0" cellpadding="0" cellspacing="0">
  <tr height="30">
    <td height="50" align="center" colspan="2">
      <!-- Navigation using remote keys: Home, PageUP/PageDown (Previous/Next) -->
      <a>
        <xsl:attribute name="TVID">HOME</xsl:attribute>
        <xsl:attribute name="href"><xsl:value-of select="$homePage" /></xsl:attribute>
      </a>
      <xsl:if test="$parental-control-on != 'true'">
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
      </xsl:if>
    </td>
  </tr>

  <tr align="left" valign="top">
    <td width="420px">
      <img width="400"><xsl:attribute name="src"><xsl:value-of select="detailPosterFile" /></xsl:attribute></img>
    </td>

    <td>
      <table border="0" width="85%">
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
          <td class="title2" valign="top" colspan="2">
            <xsl:if test="rating != -1">
              <xsl:if test="$star_rating = 'true'">
                <img><xsl:attribute name="src">pictures/rating_<xsl:value-of select="round(rating div 10)*10" />.png</xsl:attribute></img>
              </xsl:if>
              <xsl:if test="$full_rating = 'true'">
                <xsl:if test="$star_rating = 'true'">
                   (<xsl:value-of select="rating div 10" />/10)
                </xsl:if>
                <xsl:if test="$star_rating = 'false'">
                  IMDB Rating: <xsl:value-of select="rating div 10" />/10
                </xsl:if>
              </xsl:if>
            </xsl:if>
            <xsl:if test="top250 != -1">
              <xsl:text>&#160;&#160;</xsl:text>Top 250: #<xsl:value-of select="top250" />
            </xsl:if>
          </td>
        </tr>
        <tr>
          <td class="title2" valign="top" colspan="2">By 
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
              <xsl:text> (</xsl:text>
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
              <xsl:text>) </xsl:text>
            </xsl:if>
          </td>
        </tr>

        <xsl:if test="count(cast/actor) != 0">
          <tr>
            <td class="title2" colspan="2">With 
              <xsl:for-each select="cast/actor[position() &lt;= $actors.max]">
                <xsl:if test="position()!=1">, </xsl:if>
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
            </td>
          </tr>
        </xsl:if>

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

        <tr class="divider"><td colspan="2"><xsl:text> </xsl:text></td></tr>

        <xsl:if test="plot != 'UNKNOWN'">
          <tr>
            <td width="85%" colspan="2">
              <xsl:attribute name="class">
                <xsl:if test="string-length(plot) >= 700">x</xsl:if>
                <xsl:if test="string-length(plot) >= 250">large-</xsl:if>plot</xsl:attribute>

 			  <xsl:variable name="plotLinebreakPreserved">
                <xsl:call-template name="PreserveLineBreaks">
                  <xsl:with-param name="text" select="plot"/>
                </xsl:call-template>
              </xsl:variable>
              
    			<xsl:call-template name="string-replace-plot-BR">
      				<xsl:with-param name="text" select='$plotLinebreakPreserved' />
    			</xsl:call-template>
              
            </td>
          </tr>
        </xsl:if>

        <tr class="spacer"><td><xsl:text> </xsl:text></td></tr>

        <tr>
          <td colspan="2">
          <center><table width="85%">
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
        
        <tr class="spacer" colspan="2"><td> </td></tr>

        <xsl:choose>
          <xsl:when test="count(files/file) = 1">
            <xsl:for-each select="files/file">
              <tr valign="top">
                <xsl:if test="//movie/season != -1">
                  <td align="right" class="normal">
                    <xsl:value-of select="@firstPart"/>
                    <xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/></xsl:if>.
                  </td>
                </xsl:if>
                <td align="center">
                  <a class="link">
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
                        <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
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
              <td align="center">
                <a class="link">
                  <xsl:attribute name="href">
                    <xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" />
                  </xsl:attribute>
                  <xsl:attribute name="TVID">Play</xsl:attribute>
                  <xsl:attribute name="name">Play</xsl:attribute>

                  <xsl:attribute name="vod">playlist</xsl:attribute>
                  <img src="pictures/play.png" onfocussrc="pictures/play_selected.png"/>
                </a>
              </td>
            </tr>
          </xsl:when>
          <xsl:otherwise>
            <tr>
              <xsl:variable name="episodeSortOrder">
                <xsl:choose>
                  <xsl:when test="$skin-reverseEpisodeOrder='true' and //movie/season != -1">
                    <xsl:text>descending</xsl:text>
                  </xsl:when>
                  <xsl:otherwise>ascending</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
              <td>
                <table>
                  <xsl:if test="($skin-playAllMovie = 'top' and //movie/season = -1) or ($skin-playAllTV = 'top' and //movie/season != -1)">
                    <tr>
                      <td class="normal">
                        <div id="title">&#160; </div>
                        <a class="link">
                          <xsl:attribute name="onfocus">hide()</xsl:attribute>
                          <xsl:attribute name="href"><xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" /></xsl:attribute>
                          <xsl:attribute name="vod">playlist</xsl:attribute>
                          <xsl:if test="//movie/prebuf != -1">
                             <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>
                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
                          <xsl:text>&#160;</xsl:text><xsl:value-of select="$skin-playAllText" />
                        </a>
                      </td>
                    </tr>
                  </xsl:if>
                  <tr valign="top">
                    <td class="normal">
                      <xsl:for-each select="files/file">
                        <xsl:sort select="@firstPart" data-type="number" order="{$episodeSortOrder}"/>
                        <xsl:if test="position() = 1">
                          <a id="playLink" tvid="Play" vod="playlist">
                            <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                          </a>
                        </xsl:if>
                        <a>
                          <xsl:attribute name="href"><xsl:value-of select="fileURL" /></xsl:attribute>
                          <xsl:attribute name="TVID"><xsl:value-of select="@firstPart"/></xsl:attribute>
                          <xsl:attribute name="name">e<xsl:value-of select="position()"/></xsl:attribute>
                          <xsl:attribute name="onkeyleftset">e<xsl:value-of select="position()-1"/></xsl:attribute>
                          <xsl:attribute name="onkeyrightset">e<xsl:value-of select="position()+1"/></xsl:attribute>
                          <xsl:attribute name="onfocus">show(<xsl:value-of select="@firstPart"/>)</xsl:attribute>
                          <xsl:attribute name="onblur">hide()</xsl:attribute>
                          
                          <xsl:if test="@vod"><xsl:attribute name="vod"><xsl:value-of select="@vod" /></xsl:attribute></xsl:if>
                          <xsl:if test="@zcd"><xsl:attribute name="zcd"><xsl:value-of select="@zcd" /></xsl:attribute></xsl:if>
                          <xsl:if test="@rar"><xsl:attribute name="rar"><xsl:value-of select="@rar" /></xsl:attribute></xsl:if>

                          <xsl:if test="//movie/prebuf != -1">
                            <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>

                          <xsl:if test="position() = 1">
                            <xsl:attribute name="name">Play</xsl:attribute>
                          </xsl:if>

                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top">
                            <xsl:attribute name="onmouseover">show(<xsl:value-of select="@firstPart"/>);</xsl:attribute>
                            <xsl:attribute name="onmouseout">hide();</xsl:attribute>
                          </img>
                        </a>
                        <a class="link">
                          <xsl:if test="position() = 1"> 
                            <xsl:attribute name="class">firstMovie</xsl:attribute> 
                          </xsl:if>

                          <xsl:value-of select="@firstPart"/>
                          <xsl:if test="@firstPart!=@lastPart">-<xsl:value-of select="@lastPart"/></xsl:if>
                          <xsl:text>&#160; </xsl:text>
                        </a>
                      </xsl:for-each>
                    </td>
                  </tr>
                  <xsl:if test="($skin-playAllMovie = 'bottom' and //movie/season = -1) or ($skin-playAllTV = 'bottom' and //movie/season != -1)">
                    <tr>
                      <td class="normal">
                        <div id="title">&#160; </div>
                        <a class="link">
                          <xsl:attribute name="onfocus">hide()</xsl:attribute>
                          <xsl:attribute name="href"><xsl:value-of select="concat(/details/movie/baseFilename,'.playlist.jsp')" /></xsl:attribute>
                          <xsl:attribute name="vod">playlist</xsl:attribute>
                          <xsl:if test="//movie/prebuf != -1">
                             <xsl:attribute name="prebuf"><xsl:value-of select="//movie/prebuf" /></xsl:attribute>
                          </xsl:if>
                          <img src="pictures/play_small.png" onfocussrc="pictures/play_selected_small.png" align="top"/>
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
          <xsl:attribute name="id">title<xsl:value-of	select="@firstPart" /></xsl:attribute>
          <xsl:choose>
            <xsl:when test="@title='UNKNOWN'">
              <xsl:choose>
                <xsl:when test="@firstPart!=@lastPart">
                  <xsl:choose>
                    <xsl:when test="//movie/season != -1">Episodes </xsl:when>
                    <xsl:otherwise>Parts </xsl:otherwise>
                  </xsl:choose>
                  <xsl:value-of select="@firstPart" /> - <xsl:value-of select="@lastPart" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:choose>
                    <xsl:when test="//movie/season != -1">Episode </xsl:when>
                    <xsl:otherwise>Part </xsl:otherwise>
                  </xsl:choose>
                  <xsl:value-of select="@firstPart" />
                </xsl:otherwise>
              </xsl:choose>
              <xsl:value-of select="@part" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@firstPart" />
              <xsl:if test="@firstPart!=@lastPart"> -	<xsl:value-of select="@lastPart" /></xsl:if>. 
              <xsl:value-of select="@title" />
            </xsl:otherwise>
          </xsl:choose>
        </div>
      </xsl:for-each>
    </td>
  </tr>
</table>
<!--  
<xsl:if test="count(sets/set) > 0">
  <div align="center">
    <table width="80%">
      <tr>
        <td class="title2" align="center" width="30%">
          <xsl:attribute name="colspan"><xsl:value-of select="count(sets/set)" /></xsl:attribute>
          <img src="pictures/set.png" align="middle" />Sets
        </td>
        <xsl:for-each select="sets/set">
          <td class="normal">
            <a>
            <xsl:attribute name="href"><xsl:value-of select="@index" />.html</xsl:attribute>
              <img align="top">
                <xsl:attribute name="src"><xsl:value-of select="@index" />_small.png</xsl:attribute>
              </img>
            </a>
          </td>
        </xsl:for-each>
      </tr>
    </table>
  </div>
</xsl:if>
-->
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
