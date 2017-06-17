import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.*;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static java.lang.System.exit;

// ---
// Program Flow
// ---
// Input: raw scan url
// 1. get what to look for (from XML)
// 2. For each target manga chapter
//   3. find thumbnail url
//   4. indicate that it's not uploaded yet, if thumbnail url not found
//   5. if images don't exist in local drive
//      6. download images at manga title dir
//
// Classes
//   * MangaDownloader - for each manga, find thumbnail URL, and save each images there
//     * MangaScanDataBase
//     * MangaMetaData
//
// New Features
//   * the program keeps running and look for the latest chapters every day, if found, send notification e-mail
//     - MangaInspecter
// Problem:
//   *
// ToDo:
//   * if chapter doesn't exist, try fetching next, if 3 attempt failed. Consider that as latest not out (define broken link as 3 consecutive chapters)
//   * run MangDownloader as deamon, send notification once a manga is available (new feature)
//
public class MangaDownloader {

    private static int TimeOutInSec = 20;
    private static boolean DownloadDirectlyFromThumbnailUrl = true;

    public static void main(String[] args) {
        //download_manga_from_mangahead();
        download_manga_from_mangamura("http://matome.manga-free-online.com/?cat=1260");
    }

    // For each manga link from 作品一覧 page such as http://matome.manga-free-online.com/?cat=3674 for Btoom
    //    For each chapter/volume link x ( find by <a href=x> under  <div class="* article_title"> )
    //       For each page link  (find by <a href = 'paged=*')
    //           For each jpg image link (find by *[1-9][0-9].jpg*)
    //              download image

    public static Boolean CreateDir(String dirName) {
        if ( new File(dirName).mkdir() ) {
            System.out.println("dir " + dirName + " was created!");
            return true;
        }else {
            System.out.println("dir " + dirName + " WASN'T created!");
            return false;
        }
    };

    protected static void download_manga_from_mangamura(String mangaTitleURL) {

        class UrlNamePair{
            String url;
            String name;
            UrlNamePair(String url, String name){ this.url = url; this.name = name; }
        };

        String imgExt = ".jpg";

        Document doc = getDocFromURL(mangaTitleURL);

        // find manga title string from manga title page
        String mangaTitleDir = doc.select("h2[class*=post_title]").first().text();
        CreateDir(mangaTitleDir);

        // find chapter/volume list URLs by finding a tag under div tag which contains article_title as a value of class atrributes
        ArrayList<UrlNamePair> chapVolURLs = new ArrayList<UrlNamePair>();
        for(Element e : doc.select("div[class*=article_title]")) {
            Element hrefIncludedElm = e.child(0);
            chapVolURLs.add( new UrlNamePair(hrefIncludedElm.attr("abs:href"), hrefIncludedElm.text() ) );
        }

        for(UrlNamePair chapVolURLNamePair : chapVolURLs )
        {
            String chapVolDirName = chapVolURLNamePair.name;

            // create chapter or volume dir
            if( CreateDir(mangaTitleDir + "/" + chapVolDirName) == false ) continue;

            System.out.println(chapVolURLNamePair.url);
            for (String pageURL : fetchLinksByPattern(chapVolURLNamePair.url, "a[href*=paged=]"))
            {
                //System.out.println(" -> pageURL: " + pageURL);

                for (String imgURL : fetchLinksByPattern(pageURL, "a[href*= " + imgExt + "]"))
                {
                    String filenamePrefix = FilenameUtils.getName(imgURL); // extract string after the last '/'
                    String imgFilename = filenamePrefix.substring(0, filenamePrefix.indexOf(".")); // remove string after "."
                    String outImgPath = mangaTitleDir + "/" + chapVolDirName + "/" + imgFilename + imgExt;
                    try {
                        SaveImageFromUrl.saveImage(imgURL, outImgPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    // --
    // Example: In order to get the URL "/?p=101512" in the following html, search is done in 2 steps.
    // That is, find <div with class value containing "article_title", then, find "<a" with non-empty href value.
    //
    //   <div class="col-sm-12 col-xs-12 article_title">  <a href="/?p=101512"> Volume 5 </a>  </div>
    // --
//    protected static ArrayList<String> fetchLinksByPatternTwoLevels(String inURL, String pattern1, String pattern2) {
//        Document doc = getDocFromURL(inURL);
//        Elements elms = doc.select(pattern1);
//        for(Element elm : elms){
//            Elements  = elm.select(pattern2);
//        }
//        ArrayList<String> outURLs = new ArrayList<String>();
//        return outURLs;
//    }

    protected static ArrayList<String> fetchLinksByPattern(String inURL, String pattern) {
        System.out.println("inURL @ fetchLinksByPattern = " + inURL);

        ArrayList<String> outURLs = new ArrayList<String>();

        Document doc = getDocFromURL(inURL);
        Elements links = doc.select(pattern);

        // remove duplicate links
        Set<String> hs = new HashSet<String>();
        for (Element link : links) {
            String absHref = link.attr("abs:href");
            hs.add(absHref);
        }

        for (String url : hs)
            outURLs.add(url);

        return outURLs;
    }

    protected static boolean download_manga_from_mangahead() {
        // Enhancement List:
        // 1. when manga download stalled due to connection issue, delete current dir
        // 2. download as long as it exists

        String mangaXML = "mangaScanDataBase.xml";
        String urlBase = "http://mangahead.me/Manga-Raw-Scan/title/title-1-Raw-Scan";
        MangaScanDataBase mangaScanDB = new MangaScanDataBase(mangaXML);

        if (DownloadDirectlyFromThumbnailUrl) {
            // single URL download
            //saveMangaScansFromThumbnailUrl("http://mangahead.com/Manga-Collections/Special-Collections/Naruto-Gaiden-VIII-Special-Raw-Scan");

            // get manga chapters
            ArrayList<MangaMetaData> mangaChaps = new ArrayList<MangaMetaData>();
            mangaChaps.addAll(mangaScanDB.getMangaMetaDataToFetch());

            for (MangaMetaData chap : mangaChaps){

                boolean downloadSucceded = true;

                //String targetUrlBase = chap.url; //"http://mangahead.com/Manga-Raw-Scan/One-Piece/One-Piece-799-Raw-Scan"; // get it from XML
                String targetUrlBase = urlBase.replaceAll("title", chap.title);

                // multiple URL download
                while(true){
                    // change chapter name : scan from the back, find the chapter number string, cast it to int, increment by 1, cast to string, replace that with original
                    // String.valueOf(Integer.parseInt(s) + 1); // int 2 string
                    Pattern p = Pattern.compile("[0-9]+");
                    Matcher m = p.matcher(targetUrlBase);
                    String oldChapterNumber = new String();
                    if (m.find()) {
                        oldChapterNumber = m.group(m.groupCount());
                    }
                    String newChapterNumber =  chap.chapter;
                    System.out.println("oldChapterNumber = " + oldChapterNumber + ",  newChapterNumber = " + newChapterNumber);
                    String targetUrl = targetUrlBase.replace(oldChapterNumber, newChapterNumber);
                    System.out.println("newChapterNumber = " + newChapterNumber);
                    System.out.println("targetUrl = " + targetUrl);
                    downloadSucceded = saveMangaScansFromThumbnailUrl(targetUrl);
                    if(!downloadSucceded){
                        System.out.println("no image in " + targetUrl);
                        break;
                    }else{
                        System.out.println(chap);
                        mangaScanDB.update(chap);
                        chap.incrementChapterNumber();
                    }
                    targetUrlBase = targetUrl;
                }
                mangaScanDB.updateDoc();
            }

        } else {
            while (download_one_chapter_per_manga(mangaScanDB)) {
            }
        }
        return true;
    }

    protected static boolean saveMangaScansFromThumbnailUrl(String thumbnailUrl) {
        // create directory to save images
        String[] urlSegments = thumbnailUrl.split("/");
        String mangaTitleChapDir = urlSegments[urlSegments.length - 1];
        String mangaTitleDir = urlSegments[urlSegments.length - 2];
        URL location = MangaDownloader.class.getProtectionDomain().getCodeSource().getLocation();
        String dirFullPath = location.toString() + mangaTitleDir + "/" + mangaTitleChapDir;

        // create tile dir and chapter dir inside it
        if ( new File(mangaTitleDir).mkdir() )
            System.out.println("dir " + mangaTitleDir + " was created!");
        else
            System.out.println("dir " + mangaTitleDir + " WASN'T created!");

        // this takes some time
        ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrl(thumbnailUrl);

        if( imageUrls.size() > 0 ) {

            // create chapter dir
            File perChapDirFile = new File(mangaTitleDir + "/" + mangaTitleChapDir);
            Boolean perChapDirCreated = perChapDirFile.mkdir();
            if ( perChapDirCreated )
                System.out.println("dir " + mangaTitleDir + "/" + mangaTitleChapDir + " was created!");
            else {
                System.out.println("dir " + mangaTitleDir + "/" + mangaTitleChapDir + " already exist.");
                Boolean completeChapExistsInDir = perChapDirFile.listFiles().length > 18 ? true : false;
                if ( completeChapExistsInDir ) return false;
            }

            // save images for each url
            for (String imageUrl : imageUrls) {
                String imgName = FilenameUtils.getName(imageUrl);
                String destinationFile = mangaTitleDir + "/" + mangaTitleChapDir + "/" + imgName;
                System.out.println("destination file = " + destinationFile);

                try {
                    SaveImageFromUrl.saveImage(imageUrl, destinationFile);
                    System.out.println(" --> saved " + imageUrl + " from " + destinationFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }else{
            return false;
        }
    }

    protected static Document getDocFromURL(String url){
        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(TimeOutInSec * 1000).userAgent("Mozilla").get();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return doc;
    }


    // collect links to images in a give url containing thumbnail images
    protected static ArrayList<String> getImageUrlsFromThumbnailUrl(String thumbnailUrl) {
        ArrayList<String> allImageUrls = new ArrayList<String>();
        // support multiple thumbnailUrl
        ArrayList<String> thumbnailUrls = getAllThumbnailPages(thumbnailUrl, "a[href*=page=]");
        System.out.println(" ---- ");
        for (String url : thumbnailUrls) {
            System.out.println(" thumbnail page = " + url);
            ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrlPerPage(url);
            allImageUrls.addAll(imageUrls);
        }
        System.out.println(" ---- ");
        return allImageUrls;
    }

    protected static ArrayList<String> getAllThumbnailPages(String thumbnailUrl, String pattern) {
        ArrayList<String> thumbnailUrls = new ArrayList<String>();
        thumbnailUrls.add(thumbnailUrl);
        System.out.println("thumbnailUrl = " + thumbnailUrl);

        Document doc = getDocFromURL(thumbnailUrl);
        Elements links = doc.select(pattern); // for mangahead
        //Elements links = doc.select(); // for mangamura
        Set<String> hs = new HashSet<String>();

        // since there are duplicate links containing "page=", use Set to find unique links
        for (Element link : links) {
            String absHref = link.attr("abs:href");
            hs.add(absHref);
        }
        for (String url : hs) {
            thumbnailUrls.add(url);
        }
        return thumbnailUrls;
    }

    // collect links to images in a give url containing thumbnail images
    // parse a Thumbnail page, find a link to individual image page, then go there and find the final link to the .jpg file
    protected static ArrayList<String> getImageUrlsFromThumbnailUrlPerPage(String thumbnailUrl) {
        ArrayList<String> imageUrls = new ArrayList<String>();
        System.out.println("thumbnailUrl = " + thumbnailUrl);
        Document doc = getDocFromURL(thumbnailUrl);
        Elements links = doc.select("a[href*=.jpg]"); // href value that contains .jpg
        for (Element link : links) {
            String absHref = link.attr("abs:href");
            Document jpgDoc = getDocFromURL(absHref);
            Elements jpgImgSrcs = jpgDoc.select("img[src$=.jpg]"); // src value that ends with .jpg
            for (Element imgUrl : jpgImgSrcs) {
                System.out.println("  ==> " + imgUrl);
                imageUrls.add(imgUrl.attr("src"));
            }
        }
        // if the page has multiple thumbnail pages, apply above to the rest of pages
        return imageUrls;
    }


    protected static boolean download_one_chapter_per_manga(MangaScanDataBase mangaScanDB){

        Boolean manga_downloaded = false;

        // get manga chapters
        ArrayList<MangaMetaData> listOfManga = new ArrayList<MangaMetaData>();

        listOfManga.addAll(mangaScanDB.getMangaMetaDataToFetch());

        String rawScanUrl = "http://mangahead.com/Manga-Raw-Scan";
        String engScanUrl = "http://mangahead.com/Manga-English-Scan";

        for (MangaMetaData meta : listOfManga) {
            // go to thumbnail page from the latest page
            String scanUrl = (meta.language.toLowerCase().equals("en") || meta.language.toLowerCase().equals("eng") || meta.language.toLowerCase().equals("english")) ? engScanUrl : rawScanUrl;
            System.out.println(" meta.language = " + meta.language);
            System.out.println(" target URL to get thumbnail url = " + scanUrl);
            String thumbnailUrl = getThumbnailUrl(scanUrl, meta.title, meta.chapter);

            // if not found, go fetch thumbnail page from manga dedicated page
            if (thumbnailUrl.length() == 0) {
                String perMangaUrl = getPerMangaUrl(scanUrl, meta.title);
                if (perMangaUrl.length() > 0) {
                    System.out.println("thumbnail URL is found at the individual manga page");
                    System.out.println("  perMangaUrl = " + perMangaUrl);

                    // try finding consecutive 3 chapters since 1 or 2 chapters may not be uploaded sometimes
                    for(int tryCount=0; tryCount < 2; tryCount++) {
                        thumbnailUrl = getThumbnailUrl(perMangaUrl, meta.title, meta.chapter);
                        if (thumbnailUrl.length() != 0) break;
                        else System.out.println("  " + meta.chapter + "is not available.");
                    }
                }
            }

            System.out.println("thumbnail URL : " + thumbnailUrl);
            System.out.print("The target manga to fetch --> ");
            System.out.println(meta);

            if (thumbnailUrl.length() > 0) {
                //saveMangaScansFromThumbnailUrl(thumbnailUrl);
                if (saveMangaScansFromThumbnailUrl(thumbnailUrl)) {
                    mangaScanDB.update(meta);
                    manga_downloaded = true;
                }
            } else {
                System.out.println("  --> URL doesn't exist! New chapter is not out yet or this chapter is never uploaded. \n");
            }

        }
        mangaScanDB.updateDoc();
        return manga_downloaded;
    }

    protected static String getPerMangaUrl(String topPageUrl, String title){
        String perMangaUrl = new String();
        System.out.println("topPageUrl = " + topPageUrl);
        Document doc = getDocFromURL(topPageUrl);
        System.out.println("title = " + title);
        // better to look for latest chapter as well as per-manga page
        Elements links = doc.select("a[href$="+title+"]");
        for (Element link : links) {
            System.out.println(" link = " + link);
            perMangaUrl = link.attr("abs:href");
        }
        return perMangaUrl;
    }

    protected static String getThumbnailUrl(String rawScanUrl, String mangaTitle, String nextChapter) {
        String thumbnailUrl = new String();
        System.out.println(" --> getThumbnailUrl(" + rawScanUrl + ", " + mangaTitle + ", " + nextChapter + ")");
        Document jpgDoc = getDocFromURL(rawScanUrl); // 10 sec timeout
        String regExpr = mangaTitle + "-" + nextChapter + "-Raw"; // find a tag with href containing target manga title and next chapter id
        Elements thumbnailUrlElm = jpgDoc.select("a[href~=" + regExpr + "]");
        System.out.println("   name = " + thumbnailUrlElm.attr("name") );
        thumbnailUrl = thumbnailUrlElm.attr("abs:href");

        return thumbnailUrl;

        //return "http://mangahead.com/Manga-Raw-Scan/World-Trigger/World-Trigger-100-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Assassination-Classroom/Assassination-Classroom-136-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Toriko/Toriko-320-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Nisekoi/Nisekoi-167-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Fairy-Tail/Fairy-Tail-428-Raw-Scan";
    }
}
