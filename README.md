# MyLiveApp
[![](https://jitpack.io/v/gswzm/MyLiveApp.svg)](https://jitpack.io/#gswzm/MyLiveApp)

##### 在主程序`builde.gradle`中添加

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
##### 在app的`builde.gralde`中添加依赖

```
dependencies {
    compile 'com.github.gswzm:MyLiveApp:1.0'
}
```

##### 用法：
[手机摄像头推流](https://github.com/gswzm/MyLiveApp/blob/master/app/src/main/java/com/wzm/myliveapp/PhoneVideoActivity.java "手机摄像头推流")

[外置摄像头推流](https://github.com/gswzm/MyLiveApp/blob/master/app/src/main/java/com/wzm/myliveapp/ExternalVideoActivity.java "外置摄像头推流")
