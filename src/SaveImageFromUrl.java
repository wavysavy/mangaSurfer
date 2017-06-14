/**
 * Created by hakiba on 4/15/2015.
 */


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SaveImageFromUrl {

    public static void saveImage(String imageUrl, String destinationFile) throws IOException {
        //System.out.println(imageUrl);
        URL url = new URL(imageUrl);

        //InputStream is = url.openStream();  // results in java.io.IOException: Server returned HTTP response code: 403 for URL:
        HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
        InputStream is = httpcon.getInputStream();

        OutputStream os = new FileOutputStream(destinationFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();
    }

}
