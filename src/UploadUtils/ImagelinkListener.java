/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UploadUtils;

/**
 *
 * @author Daniel Munkacsi
 */
public interface ImagelinkListener {
    /**
     * Called when an image link has been received from the image host.
     * Enables the developer to handle the link as required and be notified as soon as the link is available.
     * @param link the received link
     */
    public void onImageLink(String link);
}
