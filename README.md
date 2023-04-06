# Doodle
[![Maven Central](https://img.shields.io/maven-central/v/io.github.billywei01/doodle)](https://search.maven.org/artifact/io.github.billywei01/doodle)｜[中文文档](README_CN.md)

Doodle is a lightweight, efficient and powerful image loading library for Android.

Doodle's aar package is only 94K, much smaller than Glide/Fresco.

Doodle is not depend on third-party libraries, not require annotations , no need to configure proguard obfuscation.<br>
In brief, It's easy to use.

The functions implemented by Doodle include but are not limited to the following list:

- Support loading images with File, Uri, Resource(raw/drawable), assets, http, etc.
- Support static images, animated images, and taking video frames.
- Support the acceleration of loading media thumbnails.
- Support custom data loading.
- Support custom decoding.
- Support applying result to custom view.
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
implementation 'io.github.billywei01:doodle:2.1.1'
```

### Global Config
```kotlin
Doodle.config()
    .setLogger(Logger)
    .setExecutor(IOExecutor)
    .setHttpSourceFetcher(OkHttpSourceFetcher)
    .addAnimatedDecoders(GifDecoder)
```

Any option in the global configuration is optional

### Image Loading

1. Load image to View (ImageView or custom View)

```java
Doodle.load(path).into(view)
```

2. Get the result by callback

```java
Doodle.load(path).into(result -> {
    if (result instanceof Bitmap) {
        // handle bitmap
    } else if (result instanceof Drawable) {
        // handle drawable
    } else { 
        // handle result with other type or null
    }
});
```

3. Get bitmap

```java
Bitmap bitmap = Doodle.load(path).get()
```

4. Preload

```java
Doodle.load(path).preload()
```

When loading images with Doodle，start from "load"，end with "into", "get" or "preload".<br/>
After the "load" method, you could apply more options, check [API](API.md) for more details.


## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.
