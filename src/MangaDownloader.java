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
// Usage
// ---
// $java -jar mmFetcher.jar targetTitles.xml
//  where xml file contains list of m title URLs
// ---
public class MangaDownloader {

    public static void main(String[] args) {
        download_manga("mmTarget.xml");
    }

    // given a list of m title page URLs, download images
    protected static void download_manga(String mmTargetXML) {
        System.out.println("mmTargetXML : " + mmTargetXML);
        MangaScanDataBaseForMM mangaScanDB = new MangaScanDataBaseForMM(mmTargetXML);

        for( String url : mangaScanDB.getTitleURLs() ) {
            download_one_manga_title(url);
        }
    }

    // ---
    //   Download images from mm given a manga title page URL
    //    For each chapter/volume link x ( find by <a href=x> under  <div class="* article_title"> )
    //       For each page link  (find by <a href = 'paged=*')
    //           For each jpg image link (find by *[1-9][0-9].jpg*)
    //              download image
    // ---
    protected static void download_one_manga_title(String mangaTitleURL) {

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

    // ---
    // Utilities
    // ---
    private static int TimeOutInSec = 20;

    protected static Document getDocFromURL(String url){
        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(TimeOutInSec * 1000).userAgent("Mozilla").get();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return doc;
    }

    protected static Boolean CreateDir(String dirName) {
        if ( new File(dirName).mkdir() ) {
            System.out.println("dir " + dirName + " was created!");
            return true;
        }else {
            System.out.println("dir " + dirName + " WASN'T created!");
            return false;
        }
    };

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

}
