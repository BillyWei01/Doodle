# Doodle
[![Maven Central](https://img.shields.io/maven-central/v/io.github.billywei01/doodle)](https://search.maven.org/artifact/io.github.billywei01/doodle)｜[中文文档](README_CN.md)

Doodle is a lightweight, efficient and powerful image loading library for Android.

Doodle's aar package is only 93K, much smaller than Glide/Fresco.

Doodle is not depend on third-party libraries, not require annotations and confusion configuration,
 it's very easy to use.

The functions implemented by Doodle include but are not limited to the following list:

- Support loading images with File, Uri, Resource(raw/drawable), assets, http, etc.
- Support static iamges, animated images, and taking video frames.
- Support the acceleration of loading media thumbnails.
- Support custom data loading.
- Support custom decoding.
- Support custom transformation.
- Support observing lifecycle of activity to take actions (For example: Canceling the loading when the activity destroyed).
- Support pause/resume loading.
- Support disk cache (including source cache and result cache).
- Support memory cache (including LRU cache and weak reference cache).
- Support setting placeholder and animations.
- Supports down-sampling/up-sampling, clipping.


## Getting Start

### Download
```gradle
implementation 'io.github.billywei01:doodle:2.0.3'
```

### Global Config
```kotlin
Doodle.config()
    .setLogger(Logger)
    .setExecutor(IOExecutor)
    .setHttpSourceFetcher(OkHttpSourceFetcher)
    .addDrawableDecoders(GifDecoder)
```

Any option in the global configuration is optional

### Image Loading
Doodle's API for loading images is similar to Picasso/Glide.

Basic usage, load image to ImageView:

```java
Doodle.load(path).into(imageView)
```

If the target is not an ImageView, you can get the result by callback, and apply the result to your target.
The result of Doodle currently can be only three cases: Bitmap, Drawable or null.

```java
Doodle.load(path).into(result -> {
    if (result instanceof Bitmap) {
        // handle bitmap
    } else if (result instanceof Drawable) {
        // handle drawable
    } else { 
        // handle null
    }
});
```

Get bitmap directly:

```java
Bitmap bitmap = Doodle.load(path).get()
```

Preload:

```java
Doodle.load(path).preload()
```

Loading image with Doodle ，start from 'load()'，end with 'into()', 'get()' or 'preload()'.<br/>
After the 'load()' method, more parameters can be added.
You can check [API](API.md) for more detail.


## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.
