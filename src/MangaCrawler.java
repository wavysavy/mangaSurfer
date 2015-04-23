import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MangaCrawler {

    public static void main(String[] args) {
        String rawScanUrl = "http://mangahead.com/Manga-Raw-Scan";

        String thumbnailUrl = getThumbnailUrl(rawScanUrl);

        saveMangaScansFromThumbnailUrl(thumbnailUrl);
    }

    public static String getThumbnailUrl(String rawScanUrl){
        // read XML
        // for each manga
        //    find the next chapter
        //    search for thumbnailUrl in the rawScanUrl
        //    return it when it's found

        // focus on one manga for now
        // given a manga raw page, download unread chapter. That is, save a read chapter in XML as below
        // <Manga title="World-Trigger">
        //    <Chapter id='99' fetched='true'/>
        //return "http://mangahead.com/Manga-Raw-Scan/World-Trigger/World-Trigger-100-Raw-Scan";
        //return "http://mangahead.com/Manga-Raw-Scan/Assassination-Classroom/Assassination-Classroom-136-Raw-Scan";
        return "http://mangahead.com/Manga-Raw-Scan/Toriko/Toriko-320-Raw-Scan";
    }

    public static void saveMangaScansFromThumbnailUrl(String thumbnailUrl){
        ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrl(thumbnailUrl);

        // save images for each url
        for(String imageUrl : imageUrls) {
            String[] urlSegments = imageUrl.split("/");
            String imgName = urlSegments[urlSegments.length-1];
            String dirName = urlSegments[urlSegments.length-2];
            String destinationFile = dirName + "/" + imgName;
            System.out.println("des file = " + destinationFile);

            try{
                URL location = MangaCrawler.class.getProtectionDomain().getCodeSource().getLocation();
                System.out.println("URL : " + location);
                String dirFullPath = location.toString() + dirName;
                if( new File(dirName).mkdir() )
                    System.out.println("dir " + dirFullPath + " was created!");
                else
                    System.out.println("dir " + dirFullPath + " wasn't created!");
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
            Document doc = Jsoup.connect(thumbnailUrl).timeout(10 * 1000).get();
            Elements links = doc.select("a[href*=.jpg]");
            for(Element link : links){
                String absHref = link.attr("abs:href");
                System.out.println(absHref);

                Document jpgDoc = Jsoup.connect(absHref).timeout(10 * 1000).get(); // 10 sec timeout
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
