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

/**
 * Utility class for uploading to Uguu
 * @author Daniel Munkacsi
 */
public final class UguuUpload {
    
    private static final String boundary = "---------------------------" + System.currentTimeMillis();
    private static final String UGUU_URI = "http://uguu.se/api.php?d=upload";
    private static final String tmpfiletype = "file/";
    private static String extension;
    private static String filename;
    private static String uguurl;

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
        getResponse(connection);


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
            byte[] buff = new byte[2048];
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
     * Connect to Uguu.
     * @throws java.IOException while attempting to connect, can also be of type ProtocolException if given protocol is not supported
     */
    private static HttpURLConnection connect() throws IOException{
        HttpURLConnection conn = null;
        URL url = null;
        try {
            url = new URL(UGUU_URI);
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
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
	
        return conn;
    }
    
    /**
     * Send the file to Uguu.
     * @param b the contents of the file in a byte array
     * @param conn the connection to use
     * @throws java.IOException during writing to output stream
     */
    private static void sendFile(byte[] b, HttpURLConnection conn) throws IOException{
        String first = String.format("Content-Type: multipart/form-data; boundary=" + boundary +"\"\r\nContent-Length: 30721\r\n");
        String second = String.format("Content-Disposition: form-data; name=\"MAX_FILE_SIZE\"\r\n\r\n" + "100000000\r\n");
        String data = String.format("Content-Disposition: form-data; name=\"file\";filename=\"" + filename + "." + extension +"\"\r\nContent-type:" + tmpfiletype + "\r\n");
        String last = String.format("Content-Disposition: form-data; name=\"name\"");
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataOutputStream outstream;
        try {
            outstream = new DataOutputStream(conn.getOutputStream());
            outstream.writeBytes(first);
            outstream.writeBytes("\r\n");
            outstream.writeBytes("\r\n");
            
            outstream.writeBytes("--" + boundary);
            outstream.writeBytes(second);
            outstream.writeBytes("--" + boundary + "\r\n");
            
            outstream.writeBytes(data);
            outstream.writeBytes("\r\n");
            
            int i;
            while ((i = bais.read()) > -1){
                outstream.write(i);
            }
            bais.close();
            outstream.writeBytes("\r\n");
            outstream.writeBytes("--" + boundary + "\r\n");
            outstream.writeBytes(last);
            outstream.writeBytes("\r\n");
            outstream.writeBytes("\r\n");
            outstream.writeBytes("\r\n");
            outstream.writeBytes( "--" + boundary + "--");
            outstream.flush();
            outstream.close();
        } catch (IOException ex) {
            throw ex;
        }
    }
    
    /**
     * Get a response from Uguu.
     * @param conn the connection to use to listen to response.
     * @throws IOException during reading GZip response
     */
    private static void getResponse(HttpURLConnection conn) throws IOException{
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
        uguurl = response;
    }

    /**
     * Copy image link to user's clipboard.
     * This method should only be called once the upload has finished and the image link has been verified to be a valid URL.
     */
    public static void copyToClipBoard(){
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection selection = new StringSelection(uguurl);
        clipboard.setContents(selection,null);
    }
    
    /**
     * Get the uploaded file's link.
     * This method will return a null if called before the upload finishes,
     * or if there was an error during upload (such as a connection problem).
     * If used in a SwingWorker environment, this method should be called in the 'done()' method.
     * @return the link as a standard URL
     */
    public static String getFileLink(){
        return uguurl;
    }
}