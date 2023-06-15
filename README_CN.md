# Doodle

Doodle是一个轻量，高效，功能丰富的图片加载库。<br/>

Doodle依赖包仅94K，远小于Glide/Fresco等其他加载库。<br/>
Doodle不依赖第三方库，不需要注解和配置混淆，开箱即用。<br/>

Doodle实现的功能包括但不限于以下列表：

- 支持加载File, Uri, Resource(raw/drawable), assets, http等形式的文件；
- 支持静态图片，动态图片，视频缩略图；
- 支持加载媒体文件缩略图的加速（读取系统的thumbnail)；
- 支持自定义数据加载；
- 支持自定义解码实现；
- 支持加载到自定义的View;
- 支持自定义图片变换（内置圆形和圆角剪裁）；
- 支持监听生命周期，并做对应处理（如页面结束时取消加载）；
- 支持暂停/恢复加载；
- 支持磁盘缓存（包括缓存原文件和解码结果）；
- 支持内存缓存（包含LRU缓存和弱引用缓存）；
- 支持占位图，动画；
- 支持降采样/上采样，剪裁……

BitmapFactory本身可以解码JPG, PNG, WEBP,静态GIF等图片格式，高版本Android还支持HEIF格式。<br/>
通过自定义解码，可以实现处理任意格式的文件。<br/>
本项目的测试用例中实现了动态GIF, SVG, PAG以及动态WEBP等格式的解码。

## 使用方法

### 下载
```gradle
implementation 'io.github.billywei01:doodle:2.1.3'
```

### 全局配置
```kotlin
Doodle.config()
    .setLogger(Logger)
    .setExecutor(IOExecutor)
    .setHttpSourceFetcher(OkHttpSourceFetcher)
    .addAnimatedDecoders(GifDecoder)
```
全局配置中的各个选项都是可选的（可以不设置）。

### 图片加载

1. 加载图片到View (ImageView或者自定义的View)
```java
Doodle.load(path).into(view)
```

2. 通过接口回调result
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

3. 直接获取bitmap
```java
Bitmap bitmap = Doodle.load(path).get()
```

4. 预加载
```java
Doodle.load(path).preload()
```

Doodle加载图片，从load()方法开始，到into(), get() 或者 preload()方法结束。<br/>
在into()/get()/preload()之前，可以添加更多的参数。<br/>
具体可以查看[API](API_CN.md)获取更多详情。


## 实现原理

https://juejin.cn/post/6844903695902064653

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.
