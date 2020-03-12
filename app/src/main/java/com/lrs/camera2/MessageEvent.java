package com.lrs.camera2;

import android.graphics.Bitmap;

/**
 * @description 作用:
 * @date: 2020/1/3
 * @author: 卢融霜
 */
public class MessageEvent {
    private int id;
    private String Message;
    private Bitmap bitmap;

    public MessageEvent(int id, String message) {
        setId(id);
        setMessage(message);
    }

    public MessageEvent(int id, Bitmap bitmap) {
        setId(id);
        setBitmap(bitmap);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return Message;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setMessage(String message) {
        Message = message;
    }
}
