
# 一、Doodle（框架入口）

方法 | 描述
---|---
Config config()  | 返回全局配置。
Request load(String) | 根据路径返回Request。
Request load(File) | 根据文件返回Request。
Request load(int) | 根据资源id返回Request。
Request load(Uri) | 根据Uri返回Request。
File downloadOnly(String) | 下载文件（不解码器）。注意不要在主线程调用此方法。
File getCacheFile(String) | 获取缓存好的文件，没有则返回null。
void cacheBitmap(String,Bitmap,Boolean) |  保存bitmap到缓存。
Bitmap getCacheBitmap(String): Bitmap? | 从缓存中取bitmap, 无则返回null。
void pauseRequests() | 暂停请求。
void resumeRequests() | 恢复请求。
void notifyPause(Object) | 发送pause事件。
void notifyResume(Object) | 发送resume事件。
void notifyDestroy(Object) | 发送destroy事件。
void trimMemory(int) | 缩减内存缓存。
void clearMemory() | 清除LruCache中的所有bitmap。
float getScale(...) | 以原图宽高和解码参数计算缩放因子。

# 二、Config（全局配置）

方法 | 描述
---|---
setExecutor(Executor executor) | 设置Executor。<br/>APP中如果每个组件都创建自己的线程池并且保持核心线程存活，那整个APP的存活线程就很多了，容易导致OOM。<br/>故此，Doodle提供一个接口给调用者传入Executor, 这样APP可以统一管理线程，框架可以复用APP的线程池。<br/>注：Doodle内部会套队列来控制任务的并发量。
setLogger(DLogger logger) | 设置Logger。通过Log可以观察一些运行情况，输出错误日志等。
setCachePath(String) | 设置结果缓存的存储路径。如果不设定，会默认在内部目录的cache目录下创建子目录。
setResultMaxCount(int) | 设置果缓存最大数量，默认8192。
setResultCapacity(long) | 设置结果缓存的容量，默认128M。
setSourceMaxCount(int) | 设置原图缓存最大数量，默认4096。
setSourceCapacity(long) | 设置原图缓存容量，默认256M。
setMemoryCacheCapacity(long) | 设置内存缓存的容量，默认为maxMemory的1/6。
setCompressFormat(Bitmap.CompressFormat) | 设置结果缓存的压缩格式。<br/>如果不设定默认压缩格式，Doodle会根据解码格式(RGB_8888/RGB_565），文件类型，以及系统版本决定用哪一种压缩格式。
setHttpSourceFetcher(HttpSourceFetcher) | Doodle内置了下载http文件的代码，用SDK自带的HttpURLConnection实现。<br/> 如果需要用自己的下载方法，实现HttpSourceFetcher并调此方法注入即可。
addDataParser(DataParser) | 添加DataParser，用于自定义数据获取。
addDrawableDecoders(DrawableDecoder) | 添加自定义DrawableDecoder。
addBitmapDecoders(BitmapDecoder) | 添加自定义BitmapDecoder。


# 三、Request（加载请求）

方法 | 描述
---|---
sourceKey(String) | 设置数据源的key。<br/> path默认情况下作为CacheKey的一部分. 如果sourceKey不为空，Doodle会用sourceKey替换path作为CacheKey的一部分。
override(int, int) | 指定目标尺寸。
scaleType(ImageView.ScaleType) | 指定缩放类型。 <br/> 如果未设定，且target为ImageView，则会自动从ImageView获取。
clipType(ClipType) | ClipType 是自定义缩放类型枚举，大部分定义和ScaleType重叠，有少量增删。
enableUpscale() | 默认情况下，解码图片文件基于降采样的方式。<br/>例如：<br/>目标宽高是200x200，目标scaleType='centerCrop'。<br/>1、文件分辨率是400x400，最终会解码出200x200的bitmap;<br/>2、文件分辨率是100x100，最终会解码出100x100的bitmap。<br/>第2种情况，由于源文件本身的分辨率只有100x100, 当ImageView的scaleType是centerCrop时，正常解码100x100和上采样成200x200的显示结果是一样的，后者并不会比前者清晰。<br/>但有的情况就是要不管源文件分辨率，要求最终解码出的bitmap是目标宽高，这时候可以调用此方法。
memoryCacheStrategy(MemoryCacheStrategy) | 设置内存缓存策略，默认LRU策略。
diskCacheStrategy(DiskCacheStrategy) | 设置磁盘缓存策略，默认ALL。
noCache() | 不做任何缓存，包括磁盘缓存和内存缓存。
onlyIfCached(boolean) | 指定网络请求是否只从缓存读取（原图缓存）。
decodeFormat(DecodeFormat) | 设置解码格式，默认ARGB_8888。
transform(Transformation) | 设置解码后的图片变换（圆形剪裁，圆角，灰度，模糊等），可以连续调用（会按顺序执行）。<br/>Doodle内置了圆形剪裁和圆角两种Transformation。
keepOriginalDrawable() | 默认情况下请求开始会先清空ImageView之前的Drawable, 调用此方法后会保留之前的Drawable，直到加载结束。
placeholder(int) | 设置占位图，在结果加载完成之前会显示此drawable。
placeholder(Drawable) | 同上。
error(int) | 设置加载失败后的占位图。
error(Drawable) | 同上。
animation(int) | 设置加载成功后的过渡动画。
animation(Animation) | 同上。
fadeIn(int) | 加载成功后显示淡入动画。
crossFade(int) | 这个动画效果是“原图”从透明度100到0， bitmap从0到100。<br/>当设置placeholder时，placeholder为“原图”。<br/>如果没有设置placeholder,  效果和fadeIn差不多。<br/>需要注意的是，这个动画在原图和bitmap宽高不相等时，动画结束时图片会变形。<br/>因此，慎用crossFade。<br/>
alwaysAnimation(Boolean) | 默认情况下仅在图片是从磁盘或者网络加载出来时才做动画，可通过此方法设置总是做动画。
asBitmap() | 当设置了GIF Decoder时，默认情况下只要图片是GIF图片，则用GIF Decoder解码。<br/>调用此方法后，不走GIF解码器，直接用BitmapFactory解码，并返回bitmap。
observeHost(Object) | 传入宿主(Activity/Fragment/View), 以观察其生命周期。
addOption(String, String) | options目前有两个作用：<br/> 1. 传递参数给自定义Decoder;<br/>  2. 参与计算CacheKey, 以区分不同请求。
setBitmapDecoder(BitmapDecoder) | 添加针对单个请求的BitmapDecoder。<br/>此Decoder仅作用于当前Request, 并且会优先于其他自定义Decoder。
enableThumbnailDecoder() | 这个选项是用于加速相册缩略图显示的，只对相册媒体（路径开头为"content://media/"）有效。<br/>相册中的媒体文件通常伴有生成好的缩略图文件，读取缩略图文件要比读取原文件要快很多。<br/>缩率图文件分辨率较低，用于自定义相册的列表显示足够了。<br/>开启此选项，会优先尝试读取缩略图，如果读取不到则访问原文件。<br/>此选项仅作用于当前Request。
listen(CompleteListener) | 监听加载任务结束时有没有取到结果(bitmap/drawable)。
