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
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
    public ArrayList<MangaMetaData> getMangaMetaDataToFetch(){
        ArrayList<MangaMetaData> mangaChapList = new ArrayList<MangaMetaData>();
        NodeList mangaNodeList = doc.getElementsByTagName("Manga");

        for (int i = 0; i < mangaNodeList.getLength(); ++i)
        {
            Node n = mangaNodeList.item(i);
            NamedNodeMap atts = n.getAttributes();
            String language = atts.getNamedItem("language")==null ? "" : atts.getNamedItem("language").getNodeValue();
            String title = atts.getNamedItem("title").getNodeValue();
            String[] savedChaptersInStr = atts.getNamedItem("savedChapters").getNodeValue().split("\\s+");
            List<Integer> chapters = new ArrayList<Integer>();
            Integer latestChapter = 0;
            for(String chapStr : savedChaptersInStr) {
                Integer chapInt = Integer.parseInt(chapStr);
                chapters.add(chapInt);
                if(chapInt > latestChapter) latestChapter = chapInt;
            }

            String nextChapter = Integer.toString( latestChapter + 1);
            MangaMetaData mangaChap = new MangaMetaData( language, title, nextChapter );
            mangaChapList.add(mangaChap);
        }
        return mangaChapList;
    }

    public void update(MangaMetaData mangaChapter){
        NodeList mangaNodeList = doc.getElementsByTagName("Manga");

        for (int i = 0; i < mangaNodeList.getLength(); ++i) {
            Node n = mangaNodeList.item(i);
            NamedNodeMap atts = n.getAttributes();
            String title = atts.getNamedItem("title").getNodeValue();
            if(title==mangaChapter.title) {
                Node savedChaptersNode = atts.getNamedItem("savedChapters");
                String savedChaptersValue = savedChaptersNode.getNodeValue();
                savedChaptersValue += " " + mangaChapter.chapter;
                savedChaptersNode.setNodeValue(savedChaptersValue);
            }
        }
    }

    public void updateDoc(){
        // write the DOM object to the file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File(MangaScanDataXML));
            try {
                transformer.transform(domSource, streamResult);
            }catch(TransformerException e){
                e.printStackTrace();
            }
        }catch (TransformerConfigurationException e){
            e.printStackTrace();
        }
    }
}
