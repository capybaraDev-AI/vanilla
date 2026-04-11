# Vanilla Music - Progreso de actualización

## Fase 1 - Scoped Storage ✅ COMPLETA
- AndroidManifest.xml: eliminado requestLegacyExternalStorage
- MediaLibrary.java: añadido getPathFromUri() + imports
- MediaScanner.java: rpcNativeVerify() compatible con API 29+
- MediaUtils.java: sanitizeMediaPath() simplificada
- CoverCache.java: Downloads dir lazy, shadowBase privado
- app/build.gradle: applicationId cambiado a .dev para coexistir con original

## Fase 2 - Pendiente
- AudioFocus API moderna (AudioFocusRequest)
- AsyncTask → reemplazar en AudioSearchActivity y AudioPickerActivity
- AudioPickerActivity: query por MediaColumns.DATA

## Fase 3 - Pendiente
- SlidingTabLayout → TabLayout de Material
- Theming: migrar a Material 3

## Fase 4 - Pendiente
- Material You (colores dinámicos del wallpaper)
