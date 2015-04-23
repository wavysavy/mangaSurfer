import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

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

       for(String thumbnailUrl : getThumbnailUrls(rawScanUrl) ){
           System.out.println("thumbnail URL : " + thumbnailUrl);
           saveMangaScansFromThumbnailUrl(thumbnailUrl);
       }
    }

    public static class MangaChapter{
        public String title;
        public String chapter;
        public MangaChapter(String title, String chapter){
            this.title = title;
            this.chapter = chapter;
        }
    }

    public static ArrayList<String> getThumbnailUrls(String rawScanUrl) {
        // get manga title and chapter from XML
        ArrayList<MangaChapter> manChaps = new ArrayList<MangaChapter>();
        MangaChapter manChap = new MangaChapter("Gintama", "538");
        manChaps.add( manChap );
        manChaps.add( new MangaChapter("Toriko", "320") );

        ArrayList<String> thumbnailUrls = new ArrayList<String>();

        for(MangaChapter chap: manChaps){
            thumbnailUrls.add( getThumbnailUrl(rawScanUrl, chap.title, chap.chapter) );
        }

        return thumbnailUrls;
    }

    public static String getThumbnailUrl(String rawScanUrl, String mangaTitle, String nextChapter){
        // focus on one manga for now
        // given a manga raw page, download unread chapter. That is, save a read chapter in XML as below
        // <Manga title="World-Trigger">
        //    <Chapter id='99' fetched='true'/>

        // read XML
        // for each manga
        //    find the next chapter
        //    search for thumbnailUrl in the rawScanUrl
        //    return it when it's found
        //String mangaTitle = "Gintama";
        //String nextChapter = "538";
        //String nextChapter = "538";
        String thumbnailUrl = new String();

        try{
            Document jpgDoc = Jsoup.connect(rawScanUrl).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
            //Elements thumbnailUrlElm = jpgDoc.select("a[href*=" + mangaTitle + "]"); // find a tag with href containing target manga title and next chapter id
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

    // collect links to images in a give url
    public static ArrayList<String> getImageUrlsFromThumbnailUrl(String thumbnailUrl){
        ArrayList<String> imageUrls = new ArrayList<String>();
        try {
            System.out.println("thumbnailUrl = " + thumbnailUrl);
            Document doc = Jsoup.connect(thumbnailUrl).timeout(TimeOutInSec * 1000).get();
            Elements links = doc.select("a[href*=.jpg]");
            for(Element link : links){
                String absHref = link.attr("abs:href");
                System.out.println(absHref);

                Document jpgDoc = Jsoup.connect(absHref).timeout(TimeOutInSec * 1000).get(); // 10 sec timeout
                Elements jpgImgSrcs = jpgDoc.select("img[src$=.jpg]");
                for(Element imgUrl : jpgImgSrcs) {
                   System.out.println("  ==> " + imgUrl);
                   imageUrls.add(imgUrl.attr("src"));
                }

            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return imageUrls;
    }

}
