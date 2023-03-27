# Doodle

Doodle是一个轻量，高效，功能丰富的图片加载库。<br/>

Doodle依赖包仅93K，远小于Glide/Fresco等其他加载库。<br/>
Doodle不依赖第三方库，不需要注解和配置混淆，开箱即用。<br/>

Doodle实现的功能包括但不限于以下列表：

- 支持加载File, Uri, Resource(raw/drawable), assets, http等形式的文件；
- 支持静态图片，动态图片，视频缩略图；
- 支持加载媒体文件缩略图的加速（读取系统的thumbnail)；
- 支持自定义数据加载；
- 支持自定义解码实现；
- 支持自定义图片变换（内置圆形和圆角剪裁）；
- 支持监听生命周期，并做对应处理（如页面结束时取消加载）；
- 支持暂停/恢复加载；
- 支持磁盘缓存（包括缓存原文件和解码结果）；
- 支持内存缓存（包含LRU缓存和弱引用缓存）；
- 支持占位图，动画；
- 支持降采样/上采样，剪裁……


## 使用方法

### 下载
```gradle
implementation 'io.github.billywei01:doodle:2.0.2'
```

### 全局配置
```kotlin
Doodle.config()
    .setLogger(Logger)
    .setExecutor(IOExecutor)
    .setHttpSourceFetcher(OkHttpSourceFetcher)
    .addDrawableDecoders(GifDecoder)
```
全局配置中的各个选项都是可选的（可以不设置）。

### 图片加载
Doodle加载图片的API和Picasso/Glide相似。

基本用法，加载图片到ImageView:
```java
Doodle.load(path).into(imageView)
```

或者，如果target不是ImageView, 可以通过接口回调result，在将结果应用到所需要的target。<br/>
Doodle的result目前只有三种情况，Bitmap，Drawable或者null。
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

或者直接获取bitmap:
```java
Bitmap bitmap = Doodle.load(path).get()
```

或者仅预加载：
```java
Doodle.load(path).preload()
```

Doodle加载图片，从load()方法开始，到into(), get() 或者 preload()方法结束。<br/>
在into()/get()/preload()之前，可以添加更多的参数。<br/>
具体可以查看[API](API_CN.md)获取更多详情。


## 相关链接

https://juejin.cn/post/6844903695902064653

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.
