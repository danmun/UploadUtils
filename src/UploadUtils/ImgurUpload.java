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
import java.awt.Toolkit;
import java.awt.Image;
import java.io.File;
import java.net.URL;

/**
 * Utility class for uploading to Imgur.
 * @author Daniel Munkacsi
 */
public final class ImgurUpload {
    private static final String IMGUR_POST_URI = "https://api.imgur.com/3/image.json";
    private static String IMGUR_API_KEY;
    private static String imgurl;
    
    /**
     * Create a new uploader object.
     * @param API_KEY the API key used when connecting to Imgur, upload will only succeed if this is a valid key
     */
    public static void SET_API_KEY(String API_KEY){
        IMGUR_API_KEY = API_KEY;
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
        String filetype = "";
        int startindex = response.indexOf("http");
        int endindex = 0;
        if(response.contains(".png")){
            endindex = response.indexOf(".png");
            filetype = ".png";
        }else if(response.contains(".jpg")){
            endindex = response.indexOf(".jpg");
            filetype = ".jpg";
        }
        char[] url = new char[endindex - startindex];
        int index = 0;
        for(int i = startindex; i < endindex; i++){
            url[index] = response.charAt(i);
            index++;
        }
        imgurl = String.valueOf(url) + filetype;
    }
    
    /**
     * Copy image link to user's clipboard.
     * This method should only be called once the upload has finished and the image link has been verified to be a valid URL.
     */
    public static void copyToClipBoard(){
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection selection = new StringSelection(imgurl);
        clipboard.setContents(selection,null);
    }
    
    /**
     * Get the uploaded image's link.
     * This method will return a null if called before the upload finishes,
     * or if there was an error during upload (such as a connection problem).
     * If used in a SwingWorker environment, this method should be called in the 'done()' method.
     * @return the link as a standard URL
     */
    public static String getImageLink(){
        return imgurl;
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