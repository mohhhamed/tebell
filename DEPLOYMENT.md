# دليل النشر - Bell App

## 🚀 رفع المشروع على GitHub

### 1. إنشاء مستودع GitHub

#### الخطوات:
1. اذهب إلى [GitHub.com](https://github.com)
2. انقر على "New repository"
3. املأ البيانات:
   - **Repository name**: `Bell-Teacher-App`
   - **Description**: `تطبيق الجرس الذكي للمدرسين - Smart Bell App for Teachers`
   - **Visibility**: Public (أو Private حسب الرغبة)
   - ✅ Add a README file
   - ✅ Add .gitignore (Android)
   - ✅ Choose a license (MIT)

### 2. تهيئة Git محلياً

```bash
# الانتقال لمجلد المشروع
cd Bell

# تهيئة Git
git init

# إضافة remote origin
git remote add origin https://github.com/username/Bell-Teacher-App.git

# إضافة جميع الملفات
git add .

# أول commit
git commit -m "🎉 Initial commit: Bell Teacher App v1.0.0

✨ Features:
- Complete Android app for teachers
- Schedule management with JSON/QR import
- Sound notifications system
- Location-based activation
- Background service
- Material Design 3 UI
- Arabic RTL support
- Dark/Light theme

🛠️ Tech Stack:
- Kotlin 100%
- Room Database
- Hilt DI
- Coroutines
- Google Maps
- Material Design 3"

# رفع للـ main branch
git branch -M main
git push -u origin main
```

### 3. إضافة الأصوات

```bash
# إضافة ملفات الأصوات (بعد تحميلها)
git add app/src/main/res/raw/
git commit -m "🔊 Add sound files for bell notifications

- bell_classic.mp3: Traditional school bell
- bell_modern.wav: Modern chime sound  
- chime_soft.mp3: Soft notification sound
- chime_loud.wav: Loud alert sound

All sounds are royalty-free and optimized for mobile"

git push origin main
```

## ☁️ البناء في السحابة - GitHub Actions

### الملفات المضافة:
- `.github/workflows/android-build.yml` - البناء التلقائي
- `.github/workflows/release.yml` - إنشاء الإصدارات

### ميزات البناء التلقائي:
✅ **البناء عند كل Push**
✅ **اختبار الكود تلقائياً**
✅ **إنشاء APK للتحميل**
✅ **Cache للتسريع**
✅ **تقارير الأخطاء**

## 🔐 إعداد التوقيع للإصدارات

### 1. إنشاء مفتاح التوقيع:

```bash
# إنشاء keystore جديد
keytool -genkey -v -keystore bell-release-key.keystore \
  -alias bell-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# معلومات المطلوبة:
# - كلمة مرور المتجر
# - الاسم الأول والأخير
# - اسم المنظمة
# - المدينة والولاية
# - رمز البلد (SA للسعودية)
```

### 2. تحويل إلى Base64:

```bash
# تحويل keystore إلى base64
base64 bell-release-key.keystore > keystore.base64

# نسخ المحتوى
cat keystore.base64
```

### 3. إضافة Secrets في GitHub:

اذهب إلى: `Settings > Secrets and variables > Actions`

أضف المتغيرات التالية:
```
SIGNING_KEY          # محتوى keystore.base64
ALIAS               # bell-key
KEY_STORE_PASSWORD  # كلمة مرور المتجر
KEY_PASSWORD        # كلمة مرور المفتاح
```

## 🏷️ إنشاء الإصدارات

### 1. إنشاء Tag للإصدار:

```bash
# إصدار أولي
git tag -a v1.0.0 -m "🎉 Bell App v1.0.0 - Initial Release

🚀 First stable release of Bell Teacher App

✨ Features:
- Complete schedule management
- Smart notifications
- Location-based activation
- Background service
- Modern Material Design UI
- Arabic language support

📱 Requirements:
- Android 7.0+ (API 24)
- 2GB RAM
- 50MB storage space

🔧 Technical:
- Kotlin 100%
- Room Database
- Hilt DI
- Google Maps integration"

# رفع Tag
git push origin v1.0.0
```

### 2. إصدارات تالية:

```bash
# إصدار تحديث
git tag -a v1.0.1 -m "🔧 Bell App v1.0.1 - Bug Fixes

🐛 Bug Fixes:
- Fixed notification sound issues
- Improved location accuracy
- Better battery optimization

🔊 Sound Updates:
- Added new bell sounds
- Improved audio quality
- Fixed volume control"

git push origin v1.0.1
```

## 📦 تحميل APK المبني

### من GitHub Actions:
1. اذهب إلى `Actions` tab
2. اختر آخر build ناجح
3. حمل `bell-app-debug` من Artifacts

### من Releases:
1. اذهب إلى `Releases` page
2. حمل `Bell-App-v1.0.0.apk`

## 🔄 سير العمل المقترح

### التطوير:
```bash
# إنشاء فرع جديد للميزة
git checkout -b feature/new-sounds
# تطوير الميزة
git add .
git commit -m "Add new notification sounds"
git push origin feature/new-sounds
# إنشاء Pull Request
```

### المراجعة والدمج:
1. **Pull Request** على GitHub
2. **مراجعة الكود** من الفريق
3. **اختبار تلقائي** بواسطة Actions
4. **دمج** في main branch

### الإصدار:
```bash
# تحديث رقم الإصدار في build.gradle
# إنشاء tag جديد
git tag -a v1.0.2 -m "Release notes"
git push origin v1.0.2
# بناء تلقائي وإنشاء Release
```

## 📊 مراقبة المشروع

### GitHub Insights:
- **Traffic**: زوار المستودع
- **Clones**: عدد النسخ
- **Forks**: عدد التفريعات
- **Stars**: تقييم المجتمع

### Actions Monitoring:
- **Build Status**: حالة البناء
- **Test Results**: نتائج الاختبارات
- **Deployment**: حالة النشر

## 🎯 نصائح للنجاح

### 1. التوثيق:
- ✅ README شامل
- ✅ CHANGELOG محدث
- ✅ API documentation
- ✅ Contributing guidelines

### 2. الجودة:
- ✅ اختبارات تلقائية
- ✅ Code review
- ✅ Continuous integration
- ✅ Security scanning

### 3. المجتمع:
- ✅ Issues templates
- ✅ Discussion forums
- ✅ Release notes
- ✅ User feedback

## 🔗 روابط مفيدة

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Signing Guide](https://developer.android.com/studio/publish/app-signing)
- [Semantic Versioning](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)

---

**🎊 مبروك! مشروعك جاهز للنشر على GitHub! 🎊**

