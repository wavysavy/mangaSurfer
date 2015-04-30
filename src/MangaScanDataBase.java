import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.Element;
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
        } catch (ParserConfigurationException e){
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // find all the manga title and latest chapter fetched

    // Example XML:
    public ArrayList<MangaChapter> getMangaChaptersToFetch(){
        ArrayList<MangaChapter> mangaChapList = new ArrayList<MangaChapter>();
        NodeList mangaNodeList = doc.getElementsByTagName("Manga");

        for (int i = 0; i < mangaNodeList.getLength(); ++i)
        {
            Node n = mangaNodeList.item(i);
            NamedNodeMap atts = n.getAttributes();
            String title = atts.getNamedItem("title").getNodeValue();
            String latestChapterFetched = atts.getNamedItem("latestChapterFetched").getNodeValue();
            String nextChapter = Integer.toString( Integer.parseInt(latestChapterFetched) + 1);
            MangaChapter mangaChap = new MangaChapter( title, nextChapter );
            System.out.println(mangaChap);
            mangaChapList.add(mangaChap);
        }
        return mangaChapList;
    }


}
