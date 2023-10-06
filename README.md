# WebView Template For Valor

### Использование

- В ```AndroidManifest.xml``` добавить следующие разрешения:

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
```
```xml
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
        </intent>
    </queries>
```

- В ```AndroidManifest.xml``` в тег activity, в котором выполняется вебвью, добавить атрибут ```android:launchMode="singleTop"```. Внутри тега добавить следующий intent-filter:

```xml
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="valor" />
    </intent-filter>
```
- Примеры кода на Java и Kotlin для внедрения в компонент WebVew находятся в директории ```com.example.myapplication``` в соответствующих активити. 