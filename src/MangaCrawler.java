import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MangaCrawler {

    public static int TimeOutInSec = 10;

    public static void main(String[] args){
       String rawScanUrl = "http://mangahead.com/Manga-Raw-Scan";
       String mangaXML = "mangaScanDataBase.xml";
       MangaScanDataBase mangaScanDB = new MangaScanDataBase(mangaXML);

       for(String thumbnailUrl : getThumbnailUrls(rawScanUrl, mangaScanDB) ){
           System.out.println("thumbnail URL : " + thumbnailUrl);
           saveMangaScansFromThumbnailUrl(thumbnailUrl);
           //mangaScanDB.record(saveMangaScansFromThumbnailUrl);
       }
    }

    public static Boolean UseDB = true;

    public static ArrayList<String> getThumbnailUrls(String rawScanUrl, MangaScanDataBase mangaImgDB) {
        // get manga title and chapter to fetch
        ArrayList<MangaChapter> mangaChaps = new ArrayList<MangaChapter>();

        if(UseDB) {
            mangaChaps.addAll( mangaImgDB.getMangaChaptersToFetch() );
        }else {
            //mangaChaps.add(new MangaChapter("One-Piece", "784"));
            //mangaChaps.add(new MangaChapter("Naruto-Gaiden", ""));
            mangaChaps.add(new MangaChapter("World-Trigger", "101"));
            mangaChaps.add(new MangaChapter("Toriko", "321"));
            mangaChaps.add(new MangaChapter("Shokugeki-no-Soma", "116"));
            mangaChaps.add(new MangaChapter("Nisekoi", "168"));
            mangaChaps.add(new MangaChapter("Assassination-Classroom", "137"));
        }
        ArrayList<String> thumbnailUrls = new ArrayList<String>();

        System.out.println("rawScanUrl : " + rawScanUrl);

        for(MangaChapter chap: mangaChaps){
            System.out.println("chap.title : " + chap.title);
            System.out.println("chap.chapter : " + chap.chapter);
            thumbnailUrls.add( getThumbnailUrl(rawScanUrl, chap.title, chap.chapter) );
        }

        return thumbnailUrls;
    }

    public static String getThumbnailUrl(String rawScanUrl, String mangaTitle, String nextChapter){
        String thumbnailUrl = new String();

        try{
            Document jpgDoc = Jsoup.connect(rawScanUrl).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
            String regExpr = mangaTitle + "-" + nextChapter; // find a tag with href containing target manga title and next chapter id
            Elements thumbnailUrlElm = jpgDoc.select("a[href~=" + regExpr + "]");
            thumbnailUrl = thumbnailUrlElm.attr("abs:href");

            System.out.println("thumbnailUrl : " + thumbnailUrl);
            //System.in.read();

        }catch(IOException e){
            e.printStackTrace();
        }

        return thumbnailUrl;

        //return "http://mangahead.com/Manga-Raw-Scan/World-Trigger/World-Trigger-100-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Assassination-Classroom/Assassination-Classroom-136-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Toriko/Toriko-320-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Nisekoi/Nisekoi-167-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Fairy-Tail/Fairy-Tail-428-Raw-Scan";
    }

    public static void saveMangaScansFromThumbnailUrl(String thumbnailUrl){
        ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrl(thumbnailUrl);

        // create directry to save images
        String[] urlSegments = imageUrls.get(0).split("/");
        String dirName = urlSegments[urlSegments.length-2];
        URL location = MangaCrawler.class.getProtectionDomain().getCodeSource().getLocation();
        System.out.println("URL : " + location);
        String dirFullPath = location.toString() + dirName;

        if( new File(dirName).mkdir() )
            System.out.println("dir " + dirFullPath + " was created!");
        else
            System.out.println("dir " + dirFullPath + " WASN'T created!");

        // save images for each url
        for(String imageUrl : imageUrls) {
            String imgName = FilenameUtils.getName(imageUrl);
            String destinationFile = dirName + "/" + imgName;
            System.out.println("des file = " + destinationFile);

            try{
                SaveImageFromUrl.saveImage(imageUrl, destinationFile);
                System.out.println(" --> saved " + imageUrl + " from " + destinationFile);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    // collect links to images in a give url containing thumbnail images
    public static ArrayList<String> getImageUrlsFromThumbnailUrl(String thumbnailUrl){
        ArrayList<String> allImageUrls = new ArrayList<String>();
        // support multiple thumbnailUrl
        ArrayList<String> thumbnailUrls = getAllThumbnailPages(thumbnailUrl);
        System.out.println(" ---- ");
        for(String url : thumbnailUrls) {
            System.out.println(" thumbnail page = " + url);
            ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrlPerPage(url);
            allImageUrls.addAll(imageUrls);
        }
        System.out.println(" ---- ");
        return allImageUrls;
    }

    private static ArrayList<String> getAllThumbnailPages(String thumbnailUrl) {
        ArrayList<String> thumbnailUrls = new ArrayList<String>();
        thumbnailUrls.add(thumbnailUrl);
        try {
            Document doc = Jsoup.connect(thumbnailUrl).timeout(TimeOutInSec * 1000).get();
            Elements links = doc.select("a[href*=page=]");
            Set<String> hs = new HashSet<String>();

            // since there are duplicate links containing "page=", use Set to find unique links
            for(Element link : links){
                String absHref = link.attr("abs:href");
                hs.add(absHref);
            }
            for(String url : hs) {
                thumbnailUrls.add(url);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return thumbnailUrls;
    }

    // collect links to images in a give url containing thumbnail images
    public static ArrayList<String> getImageUrlsFromThumbnailUrlPerPage(String thumbnailUrl){
        ArrayList<String> imageUrls = new ArrayList<String>();
        try {
            System.out.println("thumbnailUrl = " + thumbnailUrl);
            Document doc = Jsoup.connect(thumbnailUrl).timeout(TimeOutInSec * 1000).get();
            Elements links = doc.select("a[href*=.jpg]");
            for(Element link : links){
                String absHref = link.attr("abs:href");
                Document jpgDoc = Jsoup.connect(absHref).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
                Elements jpgImgSrcs = jpgDoc.select("img[src$=.jpg]");
                for(Element imgUrl : jpgImgSrcs) {
                    System.out.println("  ==> " + imgUrl);
                    imageUrls.add(imgUrl.attr("src"));
                }
            }
            // if the page has multiple thumbnail pages, apply above to the rest of pages
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return imageUrls;
    }
}
