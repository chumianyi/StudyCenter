#!/bin/bash
# 学习中心 构建脚本 - 纯源码编译，零混淆
# 在 GitHub Actions runner 上执行：aapt2 -> javac -> d8 -> zipalign -> apksigner
set -e
set -o pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_DIR="$PROJECT_DIR/output"
APP_NAME="app-release"

echo "============================================"
echo "  学习中心 构建 (零混淆 / 纯源码)"
echo "============================================"

# Android SDK 路径：CI 上由 workflow 设置 ANDROID_HOME
ANDROID_PLATFORM="30"
ANDROID_JAR="$ANDROID_HOME/platforms/android-30/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/30.0.3"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERROR: android.jar not found at $ANDROID_JAR"
    exit 1
fi
if [ ! -x "$BUILD_TOOLS/aapt2" ]; then
    echo "ERROR: aapt2 not found at $BUILD_TOOLS/aapt2"
    exit 1
fi
echo "Using android-$ANDROID_PLATFORM, build-tools 30.0.3"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$OUTPUT_DIR" "$BUILD_DIR/classes" "$BUILD_DIR/gen" "$BUILD_DIR/compiled_res"

echo ""
echo "=== 1. aapt2 编译资源 ==="
# 编译 res 下所有 xml
find "$PROJECT_DIR/res" -type f \( -name "*.xml" -o -name "*.png" -o -name "*.jpg" \) | while read -r f; do
    "$BUILD_TOOLS/aapt2" compile "$f" -o "$BUILD_DIR/compiled_res/"
done

echo ""
echo "=== 2. aapt2 link 生成 R.java + 基础 APK ==="
"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    -R "$BUILD_DIR/compiled_res/"*.flat \
    -A "$PROJECT_DIR/assets" \
    --min-sdk-version 17 \
    --target-sdk-version 30 \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay \
    -o "$BUILD_DIR/app.apk"

echo ""
echo "=== 3. javac 编译 Java 源码 ==="
find "$PROJECT_DIR/src" -name "*.java" > "$BUILD_DIR/sources.txt"
find "$BUILD_DIR/gen" -name "*.java" >> "$BUILD_DIR/sources.txt"
echo "源文件数: $(wc -l < "$BUILD_DIR/sources.txt")"
cat "$BUILD_DIR/sources.txt"

javac -source 1.8 -target 1.8 \
    -bootclasspath "$ANDROID_JAR" \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.txt"

echo "class 文件数: $(find "$BUILD_DIR/classes" -name '*.class' | wc -l)"

echo ""
echo "=== 4. d8 转 dex (--min-api 17) ==="
DEX_INPUTS=$(find "$BUILD_DIR/classes" -name "*.class")
"$BUILD_TOOLS/d8" --min-api 17 --output "$BUILD_DIR" $DEX_INPUTS

ls -la "$BUILD_DIR/classes.dex"

echo ""
echo "=== 5. 把 dex 加入 APK ==="
cd "$BUILD_DIR"
cp app.apk app_unsigned.apk
zip -j app_unsigned.apk classes.dex

echo ""
echo "=== 6. zipalign ==="
"$BUILD_TOOLS/zipalign" -f 4 app_unsigned.apk app_aligned.apk

echo ""
echo "=== 7. 生成 keystore 并签名 (v1+v2) ==="
KEYSTORE="$BUILD_DIR/release.keystore"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" \
        -alias studycenter -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass studycenter -keypass studycenter \
        -dname "CN=StudyCenter,O=chumianyi,C=CN"
fi

"$BUILD_TOOLS/apksigner" sign \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --ks "$KEYSTORE" \
    --ks-key-alias studycenter \
    --ks-pass pass:studycenter \
    --key-pass pass:studycenter \
    --out "$OUTPUT_DIR/${APP_NAME}.apk" \
    app_aligned.apk

echo ""
echo "=== 验证签名 ==="
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT_DIR/${APP_NAME}.apk"

echo ""
echo "=== 构建完成 ==="
ls -la "$OUTPUT_DIR/"
