[![](https://jitpack.io/v/xuqiqiang/EasyCamera2.svg)](https://jitpack.io/#xuqiqiang/EasyCamera2)

# EasyCamera2
基于camera2的相机框架

本项目基于Google官方的Camera2Basic项目，并进行了大量的优化和功能扩展。将所有的功能封装成一个library，方便开发者引入，轻松实现项目中的camera功能。

Camera2是Google在Android 5.0中全新设计的框架，相机模块是和硬件紧密相关的，Camera2中引入很多的特性，厂商的支持情况各有差异，所以Google定义了硬件兼容级别，方便开发者参考。
   * 硬件兼容性：LEGACY < LIMITED < FULL < LEVEL_3。

本项目根据设备Camera2的硬件兼容情况，兼容性为LEGACY以下的默认为camera api 1版本；其他默认为camera api 2版本。本项目的所以功能都基于camera api 1和api 2有相同的实现，可以随意切换api。

本项目在Google官方的项目基础上实现了以下扩展和优化：

- 解决了开启闪光灯拍照暗的问题
- 解决了自动闪光灯无法触发闪光的问题
- 解决了部分机型预览和拍照暗的问题
- 增加了照片视频方向修正
- 实现了手动对焦
- 实现了手动缩放
- 实现了预览界面的实时滤镜调节
- 实现了二维码、条形码实时识别
- 可以手动调节焦距（仅api2，部分机型支持）
- 可以同时打开前后摄像头（仅api2，部分机型支持）
- 可以获取yuv格式的图片



## Gradle dependency

```
allprojects {
        repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
	implementation 'com.github.xuqiqiang:EasyCamera2:1.0.0'
}
```


## Usage

[Simple demo](https://github.com/xuqiqiang/EasyCamera2/blob/master/app/src/main/java/com/xuqiqiang/camera2/demo/DemoActivity.java)

## License

[Apache License](https://github.com/xuqiqiang/EasyCamera2/blob/master/LICENSE)