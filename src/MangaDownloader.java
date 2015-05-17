import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
//     * MangaChapter
//
// New Features
//   * the program keeps running and look for the latest chapters every day, if found, send notification e-mail
//     - MangaInspecter

public class MangaDownloader {

    private static int TimeOutInSec = 10;

    public static void main(String[] args) {
        String mangaXML = "mangaScanDataBase.xml";
        MangaScanDataBase mangaScanDB = new MangaScanDataBase(mangaXML);

        // get manga chapters
        ArrayList<MangaChapter> mangaChaps = new ArrayList<MangaChapter>();

        mangaChaps.addAll(mangaScanDB.getMangaChaptersToFetch());

        String rawScanUrl = "http://mangahead.com/Manga-Raw-Scan";
        String engScanUrl = "http://mangahead.com/Manga-English-Scan";

        for (MangaChapter chap : mangaChaps) {
            // go to thumbnail page from the latest page
            String scanUrl = ( chap.language.toLowerCase().equals("en") || chap.language.toLowerCase().equals("eng") || chap.language.toLowerCase().equals("english") ) ? engScanUrl : rawScanUrl;
            System.out.println(" chap.language = " + chap.language);
            System.out.println(" target URL to get thumbnail url = " + scanUrl);
            String thumbnailUrl = getThumbnailUrl(scanUrl, chap.title, chap.chapter);

            // if not found, go fetch thumbnail page from manga dedicated page
            if( thumbnailUrl.length() == 0) {
                String perMangaUrl = getPerMangaUrl(scanUrl, chap.title);
                if( perMangaUrl.length() > 0 ) {
                    System.out.println("thumbnail URL is found at the individual manga page");
                    System.out.println("  perMangaUrl = " + perMangaUrl);
                    thumbnailUrl = getThumbnailUrl(perMangaUrl, chap.title, chap.chapter);
                    if(thumbnailUrl.length() == 0)
                        System.out.println("  ==> BUT, no thumbnail urls are collected!");
                }
            }

            System.out.println("thumbnail URL : " + thumbnailUrl);
            System.out.print("The target manga to fetch --> ");
            System.out.println(chap);

            if( thumbnailUrl.length() > 0) {
                //saveMangaScansFromThumbnailUrl(thumbnailUrl);
                if (saveMangaScansFromThumbnailUrl(thumbnailUrl))
                    mangaScanDB.update(chap);
            } else {
                System.out.println("  --> URL doesn't exist! New chapter is not out yet. \n");
            }

        }
        mangaScanDB.updateDoc();
    }

    protected static String getPerMangaUrl(String topPageUrl, String title){
        String perMangaUrl = new String();
        try {
            System.out.println("topPageUrl = " + topPageUrl);
            Document doc = Jsoup.connect(topPageUrl).timeout(TimeOutInSec * 1000).get();
            System.out.println("title = " + title);
            // better to look for latest chapter as well as per-manga page
            Elements links = doc.select("a[href$="+title+"]");
            for (Element link : links) {
                System.out.println(" link = " + link);
                perMangaUrl = link.attr("abs:href");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return perMangaUrl;
    }

    protected static Boolean urlExist(String url) {
        try {
            Document doc = Jsoup.connect(url).timeout(TimeOutInSec * 1000).get();
        }catch (UnknownHostException e) {
            return false;
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    protected static String getThumbnailUrl(String rawScanUrl, String mangaTitle, String nextChapter) {
        String thumbnailUrl = new String();
        System.out.println(" --> getThumbnailUrl(" + rawScanUrl + ", " + mangaTitle + ", " + nextChapter + ")");
        try {
            Document jpgDoc = Jsoup.connect(rawScanUrl).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
            String regExpr = mangaTitle + "-" + nextChapter; // find a tag with href containing target manga title and next chapter id
            Elements thumbnailUrlElm = jpgDoc.select("a[href~=" + regExpr + "]");
            System.out.println("   name = " + thumbnailUrlElm.attr("name") );
            thumbnailUrl = thumbnailUrlElm.attr("abs:href");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return thumbnailUrl;

        //return "http://mangahead.com/Manga-Raw-Scan/World-Trigger/World-Trigger-100-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Assassination-Classroom/Assassination-Classroom-136-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Toriko/Toriko-320-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Nisekoi/Nisekoi-167-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Fairy-Tail/Fairy-Tail-428-Raw-Scan";
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

        if ( new File(mangaTitleDir + "/" + mangaTitleChapDir).mkdir() )
            System.out.println("dir " + mangaTitleDir + "/" + mangaTitleChapDir + " was created!");
        else {
            System.out.println("dir " + mangaTitleDir + "/" + mangaTitleChapDir + " already exist. Skip saving images. ");
            return false;
        }

        ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrl(thumbnailUrl);

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
    }

    // collect links to images in a give url containing thumbnail images
    protected static ArrayList<String> getImageUrlsFromThumbnailUrl(String thumbnailUrl) {
        ArrayList<String> allImageUrls = new ArrayList<String>();
        // support multiple thumbnailUrl
        ArrayList<String> thumbnailUrls = getAllThumbnailPages(thumbnailUrl);
        System.out.println(" ---- ");
        for (String url : thumbnailUrls) {
            System.out.println(" thumbnail page = " + url);
            ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrlPerPage(url);
            allImageUrls.addAll(imageUrls);
        }
        System.out.println(" ---- ");
        return allImageUrls;
    }

    protected static ArrayList<String> getAllThumbnailPages(String thumbnailUrl) {
        ArrayList<String> thumbnailUrls = new ArrayList<String>();
        thumbnailUrls.add(thumbnailUrl);
        try {
            Document doc = Jsoup.connect(thumbnailUrl).timeout(TimeOutInSec * 1000).get();
            Elements links = doc.select("a[href*=page=]");
            Set<String> hs = new HashSet<String>();

            // since there are duplicate links containing "page=", use Set to find unique links
            for (Element link : links) {
                String absHref = link.attr("abs:href");
                hs.add(absHref);
            }
            for (String url : hs) {
                thumbnailUrls.add(url);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return thumbnailUrls;
    }

    // collect links to images in a give url containing thumbnail images
    protected static ArrayList<String> getImageUrlsFromThumbnailUrlPerPage(String thumbnailUrl) {
        ArrayList<String> imageUrls = new ArrayList<String>();
        try {
            System.out.println("thumbnailUrl = " + thumbnailUrl);
            Document doc = Jsoup.connect(thumbnailUrl).timeout(TimeOutInSec * 1000).get();
            Elements links = doc.select("a[href*=.jpg]");
            for (Element link : links) {
                String absHref = link.attr("abs:href");
                Document jpgDoc = Jsoup.connect(absHref).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
                Elements jpgImgSrcs = jpgDoc.select("img[src$=.jpg]");
                for (Element imgUrl : jpgImgSrcs) {
                    System.out.println("  ==> " + imgUrl);
                    imageUrls.add(imgUrl.attr("src"));
                }
            }
            // if the page has multiple thumbnail pages, apply above to the rest of pages
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return imageUrls;
    }
}
