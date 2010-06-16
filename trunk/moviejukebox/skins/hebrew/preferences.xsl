<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- Common parameters -->

<xsl:param name="homePage"/>
<xsl:param name="rootPath"/>

<!-- Copy of the YAMJ properties. All unused properties can be removed. -->


<xsl:param name="mjb.libraryRoot"/>

<xsl:param name="mjb.jukeboxRoot"/>

<xsl:param name="mjb.detailsDirName"/>

<xsl:param name="mjb.forceXMLOverwrite"/>

<xsl:param name="mjb.forceThumbnailsOverwrite"/>

<xsl:param name="mjb.forcePostersOverwrite"/>

<xsl:param name="mjb.forceHTMLOverwrite"/>

<xsl:param name="mjb.forceFanartOverwrite"/>

<xsl:param name="mjb.forceBannersOverwrite"/>

<xsl:param name="mjb.forceVideoImagesOverwrite"/>

<xsl:param name="mjb.nmtRootPath"/>


<xsl:param name="mjb.extensions"/>

<xsl:param name="mjb.skin.dir"/>

<xsl:param name="mjb.xmlGenreFile"/>

<xsl:param name="mjb.xmlCategoryFile"/>

<xsl:param name="mjb.newdays"/>
<xsl:param name="mjb.newcount"/>

<xsl:param name="mjb.myiHome.IP"/>

<xsl:param name="mjb.playlist.IgnoreExtensions"/>

<xsl:param name="mjb.playlist.generateMultiPart"/>

<xsl:param name="mjb.forceNFOEncoding"/>

<xsl:param name="mjb.excludeMultiPartBluRay"/>

<xsl:param name="mjb.playFullBluRayDisk"/>

<xsl:param name="mjb.scanner.fanartToken"/>
<xsl:param name="mjb.scanner.bannerToken"/>
<xsl:param name="mjb.scanner.posterToken"/>
<xsl:param name="mjb.scanner.thumbnailToken"/>
<xsl:param name="mjb.scanner.videoimageToken"/>

<xsl:param name="mjb.appendDateToLogFile"/>

<xsl:param name="mjb.logTimeStamp"/>

<xsl:param name="mjb.logThreadName"/>

<xsl:param name="mjb.MaxThreadsScan"/>

<xsl:param name="mjb.MaxThreadsProcess"/>

<xsl:param name="mjb.MaxDownloadSlots"/>

<xsl:param name="filename.scanner.language.detection"/>

<xsl:param name="filename.scanner.skip.keywords"/>

<xsl:param name="filename.movie.versions.keywords"/>

<xsl:param name="filename.scanner.source.keywords"/>

<xsl:param name="filename.scanner.source.keywords.SDTV"/>
<xsl:param name="filename.scanner.source.keywords.D-THEATER"/>
<xsl:param name="filename.scanner.source.keywords.HDDVD"/>
<xsl:param name="filename.scanner.source.keywords.BluRay"/>
<xsl:param name="filename.scanner.source.keywords.DVDRip"/>

<xsl:param name="filename.nfo.directory"/>

<xsl:param name="filename.nfo.parentDirs"/>

<xsl:param name="filename.nfo.checknewer"/>

<xsl:param name="filename.extras.keywords"/>

<xsl:param name="poster.scanner.searchForExistingCoverArt"/>

<xsl:param name="poster.scanner.coverArtExtensions"/>



<xsl:param name="poster.scanner.useFolderImage"/>

<xsl:param name="poster.scanner.SearchPriority"/>

<xsl:param name="poster.scanner.Validate"/>

<xsl:param name="poster.scanner.ValidateMatch"/>

<xsl:param name="poster.scanner.ValidateAspect"/>


<xsl:param name="mjb.singleSeriesPage"/>

<xsl:param name="mjb.sets.minSetCount"/>

<xsl:param name="mjb.sets.requireAll"/>

<xsl:param name="mjb.categories.indexList"/>

<xsl:param name="mjb.categories.displayList"/>

<xsl:param name="mjb.categories.minCount"/>

<xsl:param name="mjb.sets.indexFanart"/>

<xsl:param name="mediainfo.home"/>
<xsl:param name="mediainfo.metadata.enable"/>




<xsl:param name="mjb.subtitles.ExcludeFilesWithoutExternal"/>

<xsl:param name="appletrailers.download"/>
<xsl:param name="appletrailers.max"/>
<xsl:param name="appletrailers.typesinclude"/>
<xsl:param name="appletrailers.trailertypes"/>



<xsl:param name="mjb.listing.generate"/>
<xsl:param name="mjb.listing.plugin"/>
<xsl:param name="mjb.listing.output.filename"/>
<xsl:param name="mjb.listing.types"/>
<xsl:param name="mjb.listing.GroupByType"/>
<xsl:param name="mjb.listing.clear.UNKNOWN"/>

<xsl:param name="mjb.charset.unsafeFilenameChars"/>
<xsl:param name="mjb.charset.filenameEncodingEscapeChar"/>

<xsl:param name="mjb.internet.plugin"/>

<xsl:param name="mjb.internet.tv.plugin"/>


<xsl:param name="imdb.id.search"/>

<xsl:param name="imdb.perfect.match"/>

<xsl:param name="thetvdb.language"/>
<xsl:param name="thetvdb.dvd.episodes"/>

<xsl:param name="filmweb.id.search"/>

<xsl:param name="kinopoisk.plot.maxlength"/>

<xsl:param name="kinopoisk.rating"/>

<xsl:param name="moviemeter.id.search"/>
 
<xsl:param name="filmdelta.plot.maxlength"/>

<xsl:param name="filmdelta.rating"/>

<xsl:param name="filmdelta.getcdonposter"/>

<xsl:param name="sratim.subtitle"/>

<xsl:param name="sratim.downloadOnlyHebrew"/>

<xsl:param name="sratim.username"/>

<xsl:param name="sratim.password"/>

<xsl:param name="sratim.code"/>

<xsl:param name="sratim.textMatchSimilarity"/>

<!-- API keys -->
<xsl:param name="API_KEY_TheTVDb"/>
<xsl:param name="API_KEY_TheMovieDB"/>
<xsl:param name="API_KEY_MovieMeter"/>


<!-- Skin properties -->
<xsl:param name="mjb.homePage"/>
<xsl:param name="mjb.indexFile"/>

<xsl:param name="mjb.clean.skip"/>

<xsl:param name="genres.max"/>

<xsl:param name="actors.max"/>

<xsl:param name="imdb.plot"/>

<xsl:param name="mjb.fullMovieInfoInIndexes"/>

<xsl:param name="fanart.movie.download"/>
<xsl:param name="fanart.movie.width"/>
<xsl:param name="fanart.movie.height"/>

<xsl:param name="fanart.tv.download"/>
<xsl:param name="fanart.tv.width"/>
<xsl:param name="fanart.tv.height"/>

<xsl:param name="mjb.includeEpisodePlots"/>

<xsl:param name="mjb.includeVideoImages"/>

<xsl:param name="mjb.includeWideBanners"/>

<xsl:param name="mjb.onlySeriesBanners"/>

<xsl:param name="mjb.cycleSeriesBanners"/>

<xsl:param name="mjb.nbThumbnailsPerPage"/>

<xsl:param name="mjb.nbThumbnailsPerLine"/>

<xsl:param name="mjb.nbTvThumbnailsPerPage"/>

<xsl:param name="mjb.nbTvThumbnailsPerLine"/>

<xsl:param name="mjb.filter.genres"/>

<xsl:param name="thumbnails.format"/>

<xsl:param name="thumbnails.width"/>
<xsl:param name="thumbnails.height"/>

<xsl:param name="thumbnails.logoHD"/>

<xsl:param name="thumbnails.logoTV"/>

<xsl:param name="thumbnails.logoSet"/>

<xsl:param name="thumbnails.language"/>

<xsl:param name="thumbnails.normalize"/>

<xsl:param name="thumbnails.reflection"/>

<xsl:param name="thumbnails.reflectionHeight"/>

<xsl:param name="thumbnails.reflectionStart"/>

<xsl:param name="thumbnails.reflectionEnd"/>

<xsl:param name="thumbnails.opacityStart"/>

<xsl:param name="thumbnails.opacityEnd"/>

<xsl:param name="thumbnails.perspective"/>

<xsl:param name="thumbnails.perspectiveTop"/>
<xsl:param name="thumbnails.perspectiveBottom"/>

<xsl:param name="thumbnails.perspectiveDirection"/>

<xsl:param name="posters.format"/>
<xsl:param name="posters.width"/>
<xsl:param name="posters.height"/>
<xsl:param name="posters.normalize"/>
<xsl:param name="posters.reflection"/>
<xsl:param name="posters.logoHD"/>
<xsl:param name="posters.logoTV"/>
<xsl:param name="posters.language"/>
<xsl:param name="posters.reflectionHeight"/>
<xsl:param name="posters.reflectionStart"/>
<xsl:param name="posters.reflectionEnd"/>
<xsl:param name="posters.opacityStart"/>
<xsl:param name="posters.opacityEnd"/>
<xsl:param name="posters.perspective"/>
<xsl:param name="posters.perspectiveTop"/>
<xsl:param name="posters.perspectiveBottom"/>
<xsl:param name="posters.perspectiveDirection"/>

<xsl:param name="banners.format"/>
<xsl:param name="banners.width"/>
<xsl:param name="banners.height"/>
<xsl:param name="banners.normalize"/>
<xsl:param name="banners.reflection"/>
<xsl:param name="banners.logoHD"/>
<xsl:param name="banners.logoTV"/>
<xsl:param name="banners.language"/>
<xsl:param name="banners.reflectionHeight"/>
<xsl:param name="banners.reflectionStart"/>
<xsl:param name="banners.reflectionEnd"/>
<xsl:param name="banners.opacityStart"/>
<xsl:param name="banners.opacityEnd"/>
<xsl:param name="banners.perspective"/>
<xsl:param name="banners.perspectiveTop"/>
<xsl:param name="banners.perspectiveBottom"/>
<xsl:param name="banners.perspectiveDirection"/>

<xsl:param name="videoimages.format"/>
<xsl:param name="videoimages.width"/>
<xsl:param name="videoimages.height"/>
<xsl:param name="videoimages.normalize"/>
<xsl:param name="videoimages.reflection"/>
<xsl:param name="videoimages.logoHD"/>
<xsl:param name="videoimages.logoTV"/>
<xsl:param name="videoimages.language"/>
<xsl:param name="videoimages.reflectionHeight"/>
<xsl:param name="videoimages.reflectionStart"/>
<xsl:param name="videoimages.reflectionEnd"/>
<xsl:param name="videoimages.opacityStart"/>
<xsl:param name="videoimages.opacityEnd"/>
<xsl:param name="videoimages.perspective"/>
<xsl:param name="videoimages.perspectiveTop"/>
<xsl:param name="videoimages.perspectiveBottom"/>
<xsl:param name="videoimages.perspectiveDirection"/>

<xsl:param name="mjb.image.plugin"/>
<xsl:param name="mjb.background.plugin"/>

<xsl:param name="sorting.strip.prefixes"/>

<xsl:param name="certification.ordering"/>

<xsl:param name="indexing.character.replacement"/>

<xsl:param name="indexing.character.groupEnglish"/>

<xsl:param name="highdef.differentiate"/>
<xsl:param name="highdef.720.width"/>
<xsl:param name="highdef.1080.width"/>


</xsl:stylesheet>
