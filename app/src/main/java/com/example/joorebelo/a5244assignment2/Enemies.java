package com.example.joorebelo.a5244assignment2;

import android.content.Context;

public class Enemies extends Sprite {

    private boolean visible;
    private String color;

    public Enemies(Context context, int x, int y, int imageName, int sizeX, int sizeY, boolean visible, String color) {
        super(context, x, y, imageName, sizeX, sizeY);
        this.visible = visible;
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getColor() {return color;}

    public void setColor(String color) {this.color = color;}
}
