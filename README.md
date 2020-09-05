# TiledGraphicsGLES20

A bare-bones example of how to render a tileset to an OpenGL context in Android. The neat thing about this is that the whole image is rendered in a single `glDrawElements()` call! As opposed to a loop rendering each tile.

## What is a tileset?

A tileset (or texture atlas) is an image that is made up of rectangular or square graphic tiles that are used to build up a larger image, such as a world map. [Wikipedia](https://en.wikipedia.org/wiki/Tile-based_video_game) explains it quite well.

## How does it work?

The OpenGL rendering code calls `glDrawElements()` which is rendering a simple rectangle that fills the entire view with no 3D perspective. There are two textures enabled: 1) The tileset texture and 2) the map texture.

The map texture is a psudeo-texture that defines what tiles should go where. It is sent to the GPU as an RGBA8 (4 x 8 bit numbers per pixel) texture, but only the R and G are used. These two numbers are interpreted as an X and Y coordinate into the tileset. The fragment shader first samples into this map to determine the R and G for this screen position, and thus the X and Y of the tile to be rendered. It then determines the pixel offset into this specific tile and takes a second sample from the tileset texture to get the actual pixel color.

The map texture is generated inside the `loadMap()` function and as you can see it's all hard-coded and in need of love to make something useful.

## GOTCHAS!

There are many problems with this implementation, but the idea was to provide a bootstrap starting point that people can build from after understanding it. Here are some major problems that need to be addressed:

* The tileset has to be 256 x 256 pixels as these dimensions are hard-coded in the fragment shader. It should be trivial to extract them from the `Bitmap` object and pass them to the fragment shader as uniforms, and a good excersize for the coder to do so.
* The tile size is hard-coded when the `u_tilesetd` uniform is set in `onDrawFrame()`. Another simple excersize for the coder to fix.
* The map data is hard-coded to a size of 4 x 4 and the data can not be changed. While expanding it is simple, a more interesting thing to solve is what happens when the screen aspect ratio changes when the user rotates the phone?
* If the map data changes the texture needs to be reloaded to the GPU using `glTexSubImage2D()`. This is typically something that would happen inside a game loop in response to user input or timed events.
* If the aspect ratio changes the old map texture should be deleted using `glDeleteTextures()` and a new one created with the correct aspect ratio. This is because `glTexSubImage2D()` can only be used if the size remains the same.
* There is only 1 "layer", but with something like an RPG you probably want multiple layers: render immovable objects like land, trees, sea and buildings in one layer. Then render movable objects like players and monsters. You start to want `Layer` objects with a `render()` method. The further down this road you go the more it starts to look like a game engine... so why not just use a game engine?
* The "A" of the RGBA map texture could be used to have per-tile transparency by extracting it in the fragment shader and using it in `glFragColor()`. This would be awesome for something like shallow water - swimming players, underwater ruins, sea monsters...
* There is no "game loop" in this demo, and writing a good game loop is notoriously tricky. Some examples I have seen fire up a separate thread, but if you do that *never* reload a texture from that thread - the main thread might be halfway through rendering that texture. Make sure all your `GLES20.` calls run in the same thread.
* And no doubt many more waiting to be found...
