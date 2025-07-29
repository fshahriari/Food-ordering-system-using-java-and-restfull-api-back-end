package com.snappfood.controller;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class GenerallController {

    public static boolean isValidImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return false;
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Encodes a byte array into a Base64 string.
     *
     * @param imageBytes The byte array to encode.
     * @return The Base64 encoded string, or null if the input is null or empty.
     */
    public static String toBase64(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static boolean isValidImage(String imageBase64) {
        if (imageBase64 == null || imageBase64.length() == 0) {
            return false;
        }
        byte [] imageBytes = Base64.getDecoder().decode(imageBase64);
        return isValidImage(imageBytes);
    }

    public static byte[] toByteArray(String profileImageBase64) {
    if (profileImageBase64 == null || profileImageBase64.isEmpty()) {
        return null;
    }
    try {
        return Base64.getDecoder().decode(profileImageBase64);
    } catch (IllegalArgumentException e) {
        return null;
    }
}
}