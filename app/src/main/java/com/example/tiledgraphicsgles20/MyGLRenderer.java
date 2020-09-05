package com.example.tiledgraphicsgles20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";

    private int program_id;
    private int vertex_id;
    private int uv_id;
    private int index_id;
    private int tileset_texture_id;
    private int map_texture_id;

    private int surface_width;
    private int surface_height;

    private int map_width = 4;
    private int map_height = 4;

    private Bitmap tileset_bitmap;

    static float[] verts = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f,  1.0f, 0.0f
    };

    static float[] uvs = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private short[] indicies = {0, 1, 2, 1, 2, 3};

    private final String vertexShaderCode =
            "#version 100\n\n" +
            "attribute vec3 b_vertex;\n" +
            "attribute vec2 b_uv;\n" +
            "varying vec2 v_uv;\n\n" +
            "void main(void) {\n" +
            "    v_uv = b_uv;\n" +
            "    gl_Position = vec4(b_vertex, 1.0);\n" +
            "}\n";

    private final String fragmentShaderCode =
            "#version 100\n\n" +
            "varying vec2 v_uv;\n\n" +
            "uniform sampler2D s_map;\n" +
            "uniform sampler2D s_tileset;\n\n" +
            "uniform vec2 u_mapd;\n" +
            "uniform vec2 u_tilesetd;\n\n" +
            "void main(void) {\n" +
            "    vec4 tmp = texture2D(s_map, v_uv);\n" +
            "    vec2 tile_spec = vec2(tmp.x * 256.0, tmp.y * 256.0);\n\n" +
            "    vec2 screen_tile_base = vec2(\n" +
            "        floor(v_uv.x * u_mapd.x) / u_mapd.x,\n" +
            "        floor(v_uv.y * u_mapd.y) / u_mapd.y\n" +
            "    );\n\n" +
            "    vec2 screen_tile_diff = vec2(\n" +
            "        v_uv.x - screen_tile_base.x,\n" +
            "        v_uv.y - screen_tile_base.y\n" +
            "    );\n\n" +
            "    vec2 pct_in_tile = vec2(\n" +
            "        screen_tile_diff.x / (1.0 / u_mapd.x),\n" +
            "        screen_tile_diff.y / (1.0 / u_mapd.y)\n" +
            "    );\n\n" +
            "    vec2 tileset_sample = vec2(\n" +
            "        (tile_spec.x / u_tilesetd.x) + (pct_in_tile.x / u_tilesetd.x),\n" +
            "        (tile_spec.y / u_tilesetd.y) + (pct_in_tile.y / u_tilesetd.y)\n" +
            "    );\n\n" +
            "    gl_FragColor = texture2D(s_tileset, tileset_sample);\n" +
            "}\n";

    public MyGLRenderer(Bitmap tileset) {
        super();
        tileset_bitmap = tileset;
    }

    private static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        program_id = generateShader(vertexShaderCode, fragmentShaderCode);
        loadBuffers();
        loadTileSet();
        loadMap();
        checkGlError("generateShader");
    }

    private void loadBuffers() {
        ByteBuffer vb = ByteBuffer.allocateDirect(verts.length * 4);
        vb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = vb.asFloatBuffer();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);
        vertex_id = loadArrayBuffer(vertexBuffer, verts.length * 4);

        ByteBuffer ub = ByteBuffer.allocateDirect(uvs.length * 4);
        ub.order(ByteOrder.nativeOrder());
        FloatBuffer uvBuffer = ub.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);
        uv_id = loadArrayBuffer(uvBuffer, uvs.length * 4);

        ByteBuffer ib = ByteBuffer.allocateDirect(indicies.length * 2);
        ib.order(ByteOrder.nativeOrder());
        ShortBuffer indexBuffer = ib.asShortBuffer();
        indexBuffer.put(indicies);
        indexBuffer.position(0);
        index_id = loadElementBuffer(indexBuffer, indicies.length * 2);
    }

    private void loadTileSet() {
        final int[] id = {0};
        GLES20.glGenTextures(1, IntBuffer.wrap(id));
        GLES20.glBindTexture(GL_TEXTURE_2D, id[0]);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tileset_bitmap, 0);
        checkGlError("realize");
        tileset_texture_id = id[0];
    }

    private void loadMap() {
        byte[] data = new byte[4 * 4 * 4];
        //int oi = 0;
        int ni = 0;
        for (int y = 0; y < map_height; y++) {
            for (int x = 0; x < map_width; x++) {
                data[ni] = 3; ni++; // oi++;
                data[ni] = 1; ni++; // oi++;
                data[ni] = 0; ni++;
                data[ni] = (byte) 255; ni++;
            }
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 4 * 4);
        buffer.put(data);
        buffer.position(0);
        final int[] id = {0};
        GLES20.glGenTextures(1, IntBuffer.wrap(id));
        GLES20.glBindTexture(GL_TEXTURE_2D, id[0]);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, map_width, map_height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        checkGlError("realize");
        map_texture_id = id[0];
    }

    static int loadArrayBuffer(FloatBuffer buffer, int size) {
        final int[] id = {0};
        GLES20.glGenBuffers(1, IntBuffer.wrap(id));
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GL_ARRAY_BUFFER, size, buffer, GL_STATIC_DRAW);
        checkGlError("loadArrayBuffer");
        if (id[0] == 0) {
            Log.i(TAG, "loadArrayBuffer: returned 0");
        }
        return id[0];
    }

    static int loadElementBuffer(ShortBuffer buffer, int size) {
        final int[] id = {0};
        GLES20.glGenBuffers(1, IntBuffer.wrap(id));
        GLES20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GL_ELEMENT_ARRAY_BUFFER, size, buffer, GL_STATIC_DRAW);
        checkGlError("loadElementBuffer");
        if (id[0] == 0) {
            Log.i(TAG, "loadElementBuffer: returned 0");
        }
        return id[0];
    }

    public void onDrawFrame(GL10 unused) {
        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, surface_width, surface_height);

        GLES20.glUseProgram(program_id);
        checkGlError("use program");

        GLES20.glBindBuffer(GL_ARRAY_BUFFER, vertex_id);
        int b_vertex = GLES20.glGetAttribLocation(program_id, "b_vertex");
        GLES20.glVertexAttribPointer(b_vertex, 3, GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(b_vertex);
        checkGlError("b_vertex");

        GLES20.glBindBuffer(GL_ARRAY_BUFFER, uv_id);
        int b_uv = GLES20.glGetAttribLocation(program_id, "b_uv");
        GLES20.glVertexAttribPointer(b_uv, 2, GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(b_uv);
        checkGlError("b_uv");

        int s_map = GLES20.glGetUniformLocation(program_id, "s_map");
        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, map_texture_id);
        GLES20.glUniform1i(s_map, 0);
        checkGlError("s_map");

        int s_tileset = GLES20.glGetUniformLocation(program_id, "s_tileset");
        GLES20.glActiveTexture(GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, tileset_texture_id);
        GLES20.glUniform1i(s_tileset, 1);
        checkGlError("s_tileset");

        int u_mapd = GLES20.glGetUniformLocation(program_id, "u_mapd");
        GLES20.glUniform2f(u_mapd, map_width, map_height);
        checkGlError("u_mapd");

        int u_tilesetd = GLES20.glGetUniformLocation(program_id, "u_tilesetd");
        GLES20.glUniform2f(u_tilesetd, 16, 16);
        checkGlError("u_tilesetd");

        GLES20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_id);
        GLES20.glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
        checkGlError("draw");

        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        checkGlError("unbind");
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        surface_width = width;
        surface_height = height;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static int generateShader(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        int mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
        return mProgram;
    }
}
