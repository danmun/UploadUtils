/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UploadUtils;

import org.apache.commons.codec.binary.Base64;
import java.awt.datatransfer.StringSelection;
import java.io.UnsupportedEncodingException;
import java.awt.datatransfer.Clipboard;
import java.net.MalformedURLException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.awt.Toolkit;
import java.awt.Image;
import java.util.List;
import java.io.File;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for uploading to Imgur.
 * @author Daniel Munkacsi
 */
public final class ImgurUpload {
    private static final String IMGUR_POST_URI = "https://api.imgur.com/3/image.json";
    private static final List<ImagelinkListener> listeners = new ArrayList<>();
    private static String IMGUR_API_KEY;
        
    /**
     * Set a KEY you will use to identify yourself to Imgur.
     * @param API_KEY the API key used when connecting to Imgur, upload will only succeed if this is a valid key
     */
    public static void SET_API_KEY(String API_KEY){
        IMGUR_API_KEY = API_KEY;
    }
    
    /**
     * Add an image link listener.
     * @param ll the link listener to be added
     */
    public static void addImagelinkListener(ImagelinkListener ll){
        listeners.add(ll);
    }
    
    /**
     * Upload image.
     * Only images can be uploaded to Imgur, so this method taking a File argument would be rather pointless and error-prone.
     * @param imgToUpload the image to upload
     * @throws java.io.IOException thrown during file handling and connection problems (can be of type super, UnsupportedEncodingException and MalformedURLException)
     */
    public static void upload(BufferedImage imgToUpload) throws IOException {
        ByteArrayOutputStream baos = writeImage(imgToUpload);
        String dataToSend = encodeImage(baos);
        URLConnection connection = connect();
        sendImage(connection,dataToSend);
        String response = getResponse(connection);
        parseResponse(response);
    }
    
    /**
     * Write image to bytes.
     * @param imgToUpload the image to be written to bytes
     * @return the output stream containing image data
     * @throws java.IOException while operating on the supplied BufferedImage
     */
    private static ByteArrayOutputStream writeImage(BufferedImage imgToUpload) throws IOException{
        // Creates Byte Array from picture
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(imgToUpload, "png", baos);
        } catch (IOException ex) {
            throw ex;
        }
        return baos;
    }
    
    /**
     * Encode the byte array stream for upload.
     * @param bs the stream to encode
     * @return the encoded data, ready to be sent
     * @throws java.UnsupportedEncodingException if encoding is not supported
     */
    private static String encodeImage(ByteArrayOutputStream bs) throws UnsupportedEncodingException{
        String data = "";
        try {
            //encodes picture with Base64 and inserts api key
            data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(Base64.encodeBase64String(bs.toByteArray()), "UTF-8");
            data += "&" + URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(IMGUR_API_KEY, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw ex;
        }
        return data;
    }
    
    /**
     * Connect to image host.
     * @return the connection
     * @throws java.MalformedURLExceptionMalformedURLException if the URL can't be constructed successfully
     * @throws java.IOException if failed to open connection to the newly constructed URL
     */
    private static URLConnection connect() throws MalformedURLException, IOException{
        URLConnection conn = null;
        try {
            // opens connection and sends data
            URL url = new URL(IMGUR_POST_URI);
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Authorization", "Client-ID " + IMGUR_API_KEY);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
        } catch (MalformedURLException ex) {
            throw ex;
        } catch(IOException ex){
            throw ex;
        }
        return conn;
    }
    
    /**
     * Send the data.
     * @param cn    the connection used to send the image
     * @param data  the encoded image data to send
     * @throws java.IOException while attempting to write to output stream
     */
    private static void sendImage(URLConnection cn, String data) throws IOException{
        try{
            OutputStreamWriter wr = new OutputStreamWriter(cn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
        }catch(IOException ex){
            throw ex;
        }
    }
    
    /**
     * Get a response from the image hoster.
     * @param cn the connection to receive a response from
     * @return the response
     * @throws java.IOException while reading into input stream
     */
    private static String getResponse(URLConnection cn) throws IOException{
        String response = "";
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(cn.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                response = line.replaceAll("\\\\", "");
            }
            in.close();
        }catch(IOException ex){
            throw ex;
        }
        return response;
    }
    
    /**
     * Parse the response to get the image link.
     * @param response the image link resulting from the upload
     */
    private static void parseResponse(String response){
        JSONObject jsn = new JSONObject(response);
        JSONObject data = (JSONObject) jsn.get("data");
        String imgurl = (String) data.getString("link");
        for(ImagelinkListener ll : listeners){
            ll.onImageLink(imgurl);
        }
    }
    
    /**
     * Copy image link to user's clipboard.
     * This method could be called in onImageLink(String link) to copy the link to the user's clipboard.
     * @param imgurl the string to copy to the clipboard
     */
    public static void copyToClipBoard(String imgurl){
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection selection = new StringSelection(imgurl);
        clipboard.setContents(selection,null);
    }
    
    /**
     * Create a BufferedImage from the supplied File.
     * @param f the file to create the image from
     * @return the resulting image
     * @throws java.io.IOException while operating on supplied file
     */
    public static BufferedImage imageFromFile(File f) throws IOException{
        Image img = null;
        try {
            img = ImageIO.read(f);
        } catch (IOException ex) {
            throw ex;
        } 
        return (BufferedImage) img;
    }
}