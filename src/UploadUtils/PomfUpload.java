/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UploadUtils;

import java.awt.datatransfer.StringSelection;
import org.apache.commons.io.FilenameUtils;
import java.awt.datatransfer.Clipboard;
import java.net.MalformedURLException;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.IOException;
import java.awt.Toolkit;
import java.io.Reader;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for uploading to Uguu
 * @author Daniel Munkacsi
 */
public final class PomfUpload {
    
    private static final String boundary = "WebKitFormBoundaryZtcg4u85ATPRDWba";
    private static final List<ImagelinkListener> listeners = new ArrayList<>();
    private static final String POMF_URI = "https://pomf.cat/upload.php";
    private static final String POMF_FRONT_END_URI = "https://a.pomf.cat/";
    private static final String tmpfiletype = "file/";
    private static String extension;
    private static String filename;

    /**
     * Add an image link listener.
     * @param ll the link listener to be added
     */
    public static void addImagelinkListener(ImagelinkListener ll){
        listeners.add(ll);
    }
    
    /**
     * Upload file.
     * @param f the file to upload
     * @throws java.io.IOException (types could be ProtocolException, FileNotFoundException)
     */
    public static void upload(File f) throws IOException{
        String fullname = f.getName();
        extension = FilenameUtils.getExtension(fullname);
        filename = FilenameUtils.getBaseName(fullname);
        byte[] bytes = fileToBytes(f); 
        HttpURLConnection connection = connect();
        sendFile(bytes,connection);
        String response = getResponse(connection);
        parseResponse(response);
    }
    
    /**
     * Convert the file to byte array.
     * @param f the file to be written to a byte array
     * @return byte array containing file's data
     * @throws jave.IOException during operating on passed file and creating output stream
     */ 
    private static byte[] fileToBytes(File f) throws IOException{
        byte[] filebytes = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            int read;
            byte[] buff = new byte[1024];
            while ((read = in.read(buff)) > 0){
                out.write(buff, 0, read);
            }   filebytes = out.toByteArray();
            out.flush();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        }
        return filebytes;
    }
    
    /**
     * Connect to Pomf.
     * @throws java.IOException while attempting to connect, can also be of type ProtocolException if given protocol is not supported
     */
    private static HttpURLConnection connect() throws IOException{
        HttpURLConnection conn = null;
        URL url = null;
        try {
            url = new URL(POMF_URI);
        } catch (MalformedURLException ex) {
            Logger.getLogger(UguuUpload.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            throw ex;
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException ex) {
            throw ex;
        }
//      conn.setRequestProperty("Host","pomf.cat");
        conn.setRequestProperty("Connection", "keep-alive");
//        conn.setRequestProperty("Content-Length", "3423");
//        conn.setRequestProperty("Origin", "https://pomf.cat");  
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
        conn.setRequestProperty("Content-Type","multipart/form-data; boundary=----" + boundary);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//        conn.setRequestProperty("Referer", "https://pomf.cat/");  
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
//        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//      conn.setRequestProperty("Cookie", "__cfduid=d46abf9b3f58e7a7344d431ad0230f29b1439947653");
        return conn;
    }
    
    public static void p(String s){
        System.out.println(s);
    }
    
    /**
     * Send the file to Uguu.
     * @param b the contents of the file in a byte array
     * @param conn the connection to use
     * @throws java.IOException during writing to output stream
     */
    private static void sendFile(byte[] b, HttpURLConnection conn) throws IOException{        
        System.out.println("entered sendfile");
        String introline = "------"+boundary;
        String padder = String.format("Content-Disposition: form-data; name=\"files[]\"; filename=\"" + filename + "." + extension +"\"\r\nContent-type: " + tmpfiletype + "\r\n");
        String outroline = "------"+boundary+"--";
               
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataOutputStream outstream;
        try {
            outstream = new DataOutputStream(conn.getOutputStream());
            outstream.writeBytes(introline);
            outstream.writeBytes("\r\n");
            outstream.writeBytes(padder);
            outstream.writeBytes("\r\n");
            
            int i;
            while ((i = bais.read()) > -1){
                outstream.write(i);
                
            }
            bais.close();
            
            outstream.writeBytes("\r\n");
            outstream.writeBytes("\r\n");
            outstream.writeBytes(outroline);
            outstream.flush();
            outstream.close();
        }catch(IOException ex){
            if(ex instanceof SSLHandshakeException){
                ex.printStackTrace();
            }else{
                throw ex;
            }
        }
    }
    
    /**
     * Get a response from Uguu.
     * @param conn the connection to use to listen to response.
     * @throws IOException during reading GZip response
     */
    private static String getResponse(HttpURLConnection conn) throws IOException{
        String charset = "UTF-8";
        InputStream gzippedResponse = conn.getInputStream();
        InputStream ungzippedResponse = new GZIPInputStream(gzippedResponse);
        Reader reader = new InputStreamReader(ungzippedResponse, charset);
        StringWriter writer = new StringWriter();
        char[] buffer = new char[10240];
        for (int length = 0; (length = reader.read(buffer)) > 0;) {
            writer.write(buffer, 0, length);
        }
        String response = writer.toString();        
        writer.close();
        reader.close();
        reader.close();
        for(ImagelinkListener ll: listeners){
            ll.onImageLink(response);
        }
        return response;
    }

    /**
     * Parse the response to get the image link.
     * @param response the image link resulting from the upload
     */
    private static void parseResponse(String response){
        JSONObject outerjson = new JSONObject(response);
        JSONArray jsnarray = (JSONArray) outerjson.get("files");
        JSONObject innerjson = (JSONObject) jsnarray.get(0);
        String url = POMF_FRONT_END_URI + innerjson.get("url").toString();
        for(ImagelinkListener ll : listeners){
            ll.onImageLink(url);
        }
    }    
    
    
    /**
     * Copy image link to user's clipboard.
     * This method could be called in onImageLink(String link) to copy the link to the user's clipboard.
     * @param link the string to copy to the clipboard
     */
    public static void copyToClipBoard(String link){
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection selection = new StringSelection(link);
        clipboard.setContents(selection,null);
    }
}