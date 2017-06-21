import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;


/**
 * Created by hakiba on 6/17/2017.
 */

public class MangaScanDataBaseForMM extends MangaScanDataBase
{
    public MangaScanDataBaseForMM(String xmlFile){
        loadXML(xmlFile);
    }

    public ArrayList<String> getTitleURLs(){
        ArrayList<String> titleURLs = new ArrayList<String>();
        NodeList mangaNodeList = doc.getElementsByTagName("Manga");

        for (int i = 0; i < mangaNodeList.getLength(); ++i)
        {
            Node n = mangaNodeList.item(i);
            NamedNodeMap atts = n.getAttributes();
            String title = atts.getNamedItem("url").getNodeValue();
            titleURLs.add(title);
        }

        return titleURLs;
    }
}