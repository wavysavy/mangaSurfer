import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by hakiba on 4/23/2015.
 */
public class MangaScanDataBase {

    static String MangaScanDataXML = "manga_scan_data.xml";

    private org.w3c.dom.Document doc;

    public MangaScanDataBase(String xmlFile){
        // load XML file
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            File fXmlFile = new File(MangaScanDataXML);
            doc = dBuilder.parse(fXmlFile);
            System.out.println("doc :" + doc);
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        } catch (ParserConfigurationException e){
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // find all the manga title and latest chapter fetched
/*
    // Example XML:
    public ArrayList<MangaChapter> getMangaChaptersToFetch(){
        NodeList nList = doc.getElementsByTagName("Manga");
    }
    */
}
