# GeoImage2DXF Pro

**GeoImage2DXF Pro** هو تطبيق أندرويد هندسي متقدم ومستقل 100% يعمل بالكامل دون الحاجة لاتصال بالإنترنت (Offline Engine)، متخصص في تحويل الصور الجوية والمساحية والمخططات المعمارية (Satellite, Drone, Site Plans, Cadastral Maps) إلى رسومات متجهة نظيفة (Vector CAD Geometry) وتصديرها بصيغة DXF قياسية متوافقة مع برامج الهندسة العالمية مثل AutoCAD و Civil 3D و QGIS و LibreCAD.

---

## الميزات الرئيسية

- **100% Offline Processing:** جميع عمليات المعالجة البصرية، واكتشاف الحدود، وتبسيط المتجهات، وتوليد ملفات DXF تتم محلياً على الجهاز بدون أي خوادم خارجية أو واجهات سحابية.
- **Image Processing Pipeline:**
  - استيراد آمن مع إدارة الذاكرة وتفادي `OutOfMemory`.
  - تحويل إلى التدرج الرمادي (Grayscale) وضبط التباين التكيفي.
  - إزالة التشويه عبر فلتر جاوس 5×5.
  - كشف الحواف عبر خوارزميات `Adaptive Gaussian Thresholding` و `Canny Edges` و `Otsu Global`.
  - عمليات مورفولوجية (Morphological Closing) لربط فواصل الجدران والشوارع.
- **Auto Vectorization & Shape Detection:**
  - زر `AUTO VECTORIZE` لتحليل واستخراج العناصر الهندسية تلقائياً.
  - تبسيط المسارات عبر خوارزمية Douglas-Peucker.
  - تسوية زوايا المباني هندسياً (Orthogonal 90° Snapping).
  - اكتشاف الدوائر والمستديرات بدقة رياضية.
- **Vector Viewer & CAD Canvas:**
  - عرض المخطط المتجهي، أو الصورة الأصلية، أو التطابق مع شفافية متغيرة، أو وضع المقارنة التفاعلي (Split View).
  - تحريك وتكبير وتصغير (Pan & Zoom & Fit).
  - نظام التقاط النقاط (Snap Engine) للنهايات والمنتصف والمركز والتقاطعات.
- **CAD Layers Management:**
  - طبقات قياسية: `BUILDINGS`, `ROADS`, `BOUNDARIES`, `PARCELS`, `VEGETATION`, `0`.
  - دعم ألوان AutoCAD Color Index (ACI).
- **Scale Calibration & Georeferencing:**
  - معايرة المقياس عبر تحديد نقطتين وإدخال المسافة الحقيقية (`m`, `cm`, `mm`, `ft`, `km`).
  - إسناد جغرافي يدعم التحويل التوافقي لـ Helmert (2-Point) والتحويل التآلفي Affine (3-Point).
- **AutoCAD DXF Exporter & Validator:**
  - تصدير ملفات DXF متوافقة مع إصدارات AC1015 / R12 تحتوي على أقسام `HEADER`, `TABLES`, `ENTITIES` (`LINE`, `LWPOLYLINE`, `CIRCLE`, `ARC`, `POINT`).
  - فحص مسبق لصحة الملف وتفادي القيم الشاذة.
- **Project Persistence:**
  - حفظ المشاريع بصيغة `.geo2dxf` وقاعدة بيانات محلية سريعة (Room Database).

---

## بناء المشروع عبر GitHub Actions CI/CD

يحتوي المستودع على ملف سير العمل (Workflow) في المسار:
`.github/workflows/android_build.yml`

يقوم سير العمل تلقائياً بما يلي عند كل `push` أو `pull_request`:
1. إعداد بيئة Java 17 و Gradle Caching.
2. تهيئة ملفات البيئة `.env` ومفاتيح التوقيع Debug Keystore.
3. تشغيل جميع الاختبارات الهندسية (Unit Tests & Robolectric Tests) والتأكد من نجاحها 100%.
4. بناء حزمة التطبيق `assembleDebug` (APK).
5. رفع ملف الـ APK النهائي والتقارير في صفحة Artifacts للتحميل المباشر.

### البناء اليدوي محلياً

```bash
# تشغيل الاختبارات
./gradlew testDebugUnitTest

# بناء ملف APK
./gradlew assembleDebug
```
