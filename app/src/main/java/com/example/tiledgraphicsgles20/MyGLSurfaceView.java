package com.example.tiledgraphicsgles20;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer renderer;

    public MyGLSurfaceView(Context context, Bitmap tileset){
        super(context);
        setEGLContextClientVersion(2);
        renderer = new MyGLRenderer(tileset);
        setRenderer(renderer);
    }
}