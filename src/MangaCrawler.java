import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MangaCrawler {
    public static void main(String[] args) {
        String thumbnailUrl = "http://mangahead.com/Manga-Raw-Scan/World-Trigger/World-Trigger-99-Raw-Scan";

        ArrayList<String> imageUrls = getImageUrlsFromThumbnailUrl(thumbnailUrl);

        // save images for each url
        for(String imageUrl : imageUrls) {
            String destinationFile = FilenameUtils.getName(imageUrl);
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
