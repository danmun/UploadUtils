/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UploadUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Daniel Munkacsi
 */
public class UtilTester {
    public static void main(String args[]) throws IOException{
        
       // if(true) return;
        File file = new File("D:wat1.gif");
        int i = 4;
        
        // Set up Imgur uploader
        ImgurUpload.SET_API_KEY("API_KEY_HERE");
        ImgurUpload.addImagelinkListener(new ImagelinkListener() {
            @Override
            public void onImageLink(String link){
                System.out.println(link);
            }
        });
        
        
        // Set up Uguu uploader
        UguuUpload.addImagelinkListener(new ImagelinkListener() {
            @Override
            public void onImageLink(String link){
                System.out.println(link);
            }
        });     
        
        
        // Set up Uguu uploader
        PomfUpload.addImagelinkListener(new ImagelinkListener() {
            @Override
            public void onImageLink(String link){
                System.out.println(link);
                PomfUpload.copyToClipBoard(link);
            }
        });
              
        
        // Upload file to required host(s)        
        if(i == 1){
            ImgurUpload.upload(ImgurUpload.imageFromFile(file));        
        }else if(i == 2){
            UguuUpload.upload(file);
        }else if(i == 3){
            PomfUpload.upload(file); UguuUpload.upload(file); 
        }else{
            PomfUpload.upload(file);
            ImgurUpload.upload(ImgurUpload.imageFromFile(file));
        }
    }
    
    
    /**
     * Test a byte array for validity by attempting to write it to a file.
     * The file can then be checked manually to see if it represents the correct thing.
     * @param b byte array to be tested
     * @param fullPathToWriteTo full path to where the byte array should be written out (e.g. D:example.png)
     * @throws IOException 
     */
    public static void testbytes(byte[] b, String fullPathToWriteTo) throws IOException{
        FileOutputStream fos = new FileOutputStream(fullPathToWriteTo);
        fos.write(b);
        fos.close();
    }
}
