# TouchPC - Estado del Deploy a Google Play Store

**Fecha:** 21 de febrero de 2026
**Estado:** EN PROGRESO - Subiendo a Pruebas Internas

---

## Lo que está COMPLETADO

### Código
- [x] App Android completa (30+ archivos Kotlin, MVVM, Hilt, Navigation)
- [x] PC Server completo (Python, WebSocket, pynput)
- [x] Build debug exitoso (`app-debug.apk` - 7.28 MB)
- [x] Build release exitoso (`app-release.aab` - 3.63 MB)
- [x] compileSdk y targetSdk actualizados a 35 (requisito Play Store 2026)
- [x] versionCode = 2, versionName = "1.0.1"
- [x] Keystore de release generado (`android-app/release.keystore`, password: `touchpc2026`, alias: `touchpc`)
- [x] ProGuard/R8 configurado y funcionando

### Repositorio
- [x] GitHub repo: https://github.com/patruzz/touchpc
- [x] Commits al día (4 commits en master)
- [x] GitHub Release v1.0.0 con AAB adjunto: https://github.com/patruzz/touchpc/releases/tag/v1.0.0
- [x] `gh` CLI instalado y autenticado como `patruzz`

### Play Store - Assets
- [x] Privacy Policy publicada: https://patruzz.github.io/touchpc/privacy-policy.html
- [x] Email de contacto: patriciomartinmendez@gmail.com
- [x] Icono 512x512: `docs/assets/icon-512x512.png`
- [x] Feature Graphic 1024x500: `docs/assets/feature-graphic-1024x500.png`
- [x] Screenshots teléfono (5): `docs/assets/screenshots/phone-*.png`
- [x] Screenshots tablet 7" (2): `docs/assets/screenshots/tablet7-*.png`
- [x] Screenshots tablet 10" (2): `docs/assets/screenshots/tablet10-*.png`
- [x] Listing español: `docs/playstore/listing-es.md`
- [x] Listing inglés: `docs/playstore/listing-en.md`
- [x] Release checklist: `docs/playstore/release-checklist.md`

### Play Store Console
- [x] Cuenta Google Play Developer activa
- [x] App creada en Play Console (com.touchpc.remotecontrol)
- [x] Se subió un primer AAB con versionCode 1 (targetSdk 34 - rechazado)
- [x] Tester agregado: patriciomartinmendez@gmail.com

---

## Lo que FALTA hacer

### En Google Play Console (manual)

1. **Subir el AAB actualizado** (versionCode 2, targetSdk 35)
   - Archivo: `android-app/app/build/outputs/bundle/release/app-release.aab`
   - Ir a Pruebas internas > Crear nueva version > Subir AAB
   - IMPORTANTE: Borrar el AAB viejo (versionCode 1) antes de subir el nuevo

2. **Completar la ficha de Play Store** (si no se ha hecho):
   - Nombre: "TouchPC - Control Remoto"
   - Descripción breve y completa (copiar de `docs/playstore/listing-es.md`)
   - Subir icono 512x512, feature graphic, screenshots
   - Categoría: Tools
   - URL Privacy Policy: `https://patruzz.github.io/touchpc/privacy-policy.html`

3. **Completar secciones obligatorias**:
   - Data Safety → "No data collected", sin ads
   - Content Rating → Responder cuestionario (resultado esperado: Everyone)
   - Target Audience → 13+ o All ages
   - App Access → Indicar que requiere PC server companion

4. **Publicar la versión de pruebas internas**:
   - Agregar notas de versión
   - Revisar y lanzar

5. **Instalar en tu teléfono**:
   - Copiar el enlace de participación de testers
   - Abrir en tu Android con tu cuenta Google
   - Aceptar ser tester e instalar
   - Nota: Puede tardar hasta 1 hora en estar disponible

---

## Comandos útiles para reconstruir

```bash
# Variables de entorno necesarias
export JAVA_HOME="C:\\dev\\java\\jdk-21.0.10+7"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="C:\\Users\\patri\\AppData\\Local\\Android\\Sdk"

# Reconstruir AAB
cd android-app
./gradlew bundleRelease

# El AAB queda en:
# app/build/outputs/bundle/release/app-release.aab

# Reconstruir APK debug (para testing directo)
./gradlew assembleDebug
# adb install app/build/outputs/apk/debug/app-debug.apk
```

## Archivos sensibles (NO commitear)
- `android-app/release.keystore` - Keystore de firma (HACER BACKUP SEGURO)
- `android-app/keystore.properties` - Credenciales del keystore
- `android-app/local.properties` - Ruta al Android SDK local

---

## Estructura del proyecto

```
keyboard and mouse from mobile/
├── android-app/          # App Android (Kotlin)
│   ├── app/src/main/java/com/touchpc/remotecontrol/
│   │   ├── app/          # TouchPCApp, MainActivity
│   │   ├── di/           # Hilt DI Module
│   │   ├── data/         # PreferencesManager
│   │   ├── protocol/     # Command, Serializer, Constants
│   │   ├── transport/    # WebSocket transport
│   │   ├── discovery/    # mDNS + Manual discovery
│   │   ├── gesture/      # Multi-touch gesture interpreter
│   │   ├── service/      # Foreground ConnectionService
│   │   └── ui/           # Fragments + ViewModels (5 screens)
│   ├── release.keystore  # NO COMMITEAR
│   └── keystore.properties # NO COMMITEAR
├── pc-server/            # Servidor Python
└── docs/
    ├── plans/            # Plan de implementación
    ├── playstore/        # Listings, privacy policy, checklist
    └── assets/           # Icono, feature graphic, screenshots
```
