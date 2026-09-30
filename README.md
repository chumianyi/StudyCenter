# 学习中心 (StudyCenter)

一个纯 Java 编写的 Android 学习辅助应用，minSdk 17（Android 4.2+），无 AndroidX、无 Material Design、无 Kotlin、无 Gradle。

## 功能模块
- 英语语法学习（内置 SQLite，17+ 知识点，含讲解/例句/选择题）
- 语文拼音查询（内置 SQLite，500+ 常用汉字，含声调/部首/笔画/释义）
- 英语查询（Free Dictionary API 联网查询，带历史记录）
- 英语点读（TextToSpeech，语速/音调可调）
- 设置（科目、主题、清除记录、检查更新）

## 构建
在 GitHub Actions 上自动构建，产出 `app-release.apk`。本地如需构建：
```bash
export ANDROID_HOME=/path/to/android/sdk
./build.sh
```

## 包名
`com.chumianyi.studycenter`
