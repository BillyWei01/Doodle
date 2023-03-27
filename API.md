
## Doodle (Entrance)
Method | Description
---|---
Config config()  | Return global config object.
Request load(String) | Get Request by path.
Request load(File) | Get Request by file.
Request load(int) | Get Request by resource id.
Request load(Uri) | Get Request by uri.
File downloadOnly(String) | Download file(no decoding), don't call this method in UI thread.
File getCacheFile(String) | Get cached File, return null when no cache.
void cacheBitmap(String,Bitmap,Boolean) |  Cache bitmap to memory cache.
Bitmap getCacheBitmap(String): Bitmap? | Get bitmap from memory cache.
void pauseRequests() | Pause requests.
void resumeRequests() | Resume requests.
void notifyPause(Object) | Notify pause event of host.
void notifyResume(Object) | Notify pause event of host.
void notifyDestroy(Object) | Notify pause event of host.
void trimMemory(int) | Trim memory of LruCache.
void clearMemory() | Remove all bitmap from LruCache.


## Config (Global Configuration)

Method | Description
---|---
setExecutor(Executor executor) |Config executor.<br/>If each component in the APP creates its own thread pool and keeps the core threads alive, then there will be a lot of alive threads in the entire APP, which will easily lead to OOM. Therefore, when writing the framework, it will be better to provide an interface for user to pass in an Executor. In that way, the APP can manage threads uniformly, and the framework can reuse the thread pool of the APP. 
setLogger(DLogger logger) | Set up Logger.
setCachePath(String) | Set the storage path of the result cache.
setResultMaxCount(int) | Set the maximum number of result caches.
setResultCapacity(long) | Set the capacity of the result cache.
setSourceMaxCount(int) | Set the maximum number of source caches.
setSourceCapacity(long) | Set the capacity of the source cache.
setMemoryCacheCapacity(long) | Set the capacity of memory cache, the default is 1/6 of maxMemory.
setCompressFormat(Bitmap.CompressFormat) | Sets the compression format for the result cache. <br/>If the default compression format is not set, Doodle will decide which compression format to use according to the decoding format (RGB_8888/RGB_565), file type, and system version.
setHttpSourceFetcher(HttpSourceFetcher) | Doodle has a built-in downloader, which is implemented with the HttpURLConnection. <br/> If you need to use your own downloader, implement HttpSourceFetcher and inject with this method.
addDataParser(DataParser) | Add DataParser for custom data fetching.
addDrawableDecoders(DrawableDecoder) | Add custom DrawableDecoder.
addBitmapDecoders(BitmapDecoder) | Add custom BitmapDecoder.


## Request (Single Request)

Method | Description
---|---
sourceKey(String) | Set the key of the data source. <br/> By default, the path is part of the CacheKey, Doodle will use 'sourceKey' instead of 'path' as part of CacheKey if it is not empty.
override(int, int) | Assigned width and height of target.
scaleType(ImageView.ScaleType) | Set scale type。 <br/> If not set, and the target is ImageView, it will be automatically extract from ImageView.
clipType(ClipType) | ClipType ClipType is an enumeration of custom zoom types, most definitions overlap with ScaleType, with a few additions and deletions.
enableUpscale() | By default, Doodle decodes image files with down-sampling strategy. Call this method if you need up-sampling strategy.
memoryCacheStrategy(MemoryCacheStrategy) | Set the memory cache strategy，LRU strategy by default。
diskCacheStrategy(DiskCacheStrategy) | Set disk caching strategy.
noCache() | No to cache, including disk cache and memory cache.
onlyIfCached(boolean) | Specifies whether network requests are only read from the cache (source cache).
decodeFormat(DecodeFormat) | Set the decoding format, ARGB_8888 by default.
transform(Transformation) | Set the decoded image transformation (circular clipping, rounded corners, grayscale, blur, etc.), which can be called continuously (will be executed in order).
keepOriginalDrawable() | By default, the Drawable of ImageView will be cleared before loading. <br/> Use this method, the Drawable will stay until the loading finish.
placeholder(int) | Set placeholder  with drawable id.
placeholder(Drawable) | Set placeholder with Drawable.
error(int) | Set error drawable.
error(Drawable) | Set error drawable.
animation(int) | Set animation.
animation(Animation) | Set animation.
fadeIn(int) | Set fade in animation.
crossFade(int) | Enable cross fade transition.
alwaysAnimation(Boolean) | By default, the animation is only performed when the image is loaded from the disk or the network. This method can be used to set the animation to always be performed.
asBitmap() | When GIF Decoder is set, by default, if the picture is a GIF picture, it will be decoded with GIF Decoder. <br/>Doodle will use BitmapFactory to decode and return a bitmap as result when setting 'asBitmap'.
observeHost(Object) | Pass in the host (Activity/Fragment/View) to observe its lifecycle.
addOption(String, String) | Options currently have two effects:<br/>1. Passing parameters to a custom Decoder.<br/>2. As part of CacheKey to distinguish different requests.
setBitmapDecoder(BitmapDecoder) | Add BitmapDecoder for single request. <br/>This Decoder only works on the current Request, and will take precedence over other custom Decoders.
enableThumbnailDecoder() | This option is used to speed up the display of album thumbnails, and it only handle the album medias (which path starts with "content://media/").<br/>This option only effect on the current Request.
listen(CompleteListener) | Observe if there is result when loading complete.