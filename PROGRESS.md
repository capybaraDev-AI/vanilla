# Vanilla Music - Progreso de actualización

## Fase 1 - Scoped Storage ✅ COMPLETA
- AndroidManifest.xml: eliminado requestLegacyExternalStorage
- MediaLibrary.java: añadido getPathFromUri() + imports
- MediaScanner.java: rpcNativeVerify() compatible con API 29+
- MediaUtils.java: sanitizeMediaPath() simplificada
- CoverCache.java: Downloads dir lazy, shadowBase privado
- app/build.gradle: applicationId cambiado a .dev para coexistir con original

## Fase 2 - APIs modernas ✅ COMPLETA
- PlaybackService.java: AudioFocus migrado a AudioFocusRequest (API 26+)
- PlaybackService.java: abandonAudioFocus() agregado en onDestroy()
- AudioSearchActivity.java: AsyncTask reemplazado por Thread + runOnUiThread
- AudioPickerActivity.java: AsyncTask reemplazado por Thread + AtomicBoolean

## Fase 3 - Pendiente
- SlidingTabLayout → TabLayout de Material
- Theming: migrar a Material 3

## Fase 4 - Pendiente
- Material You (colores dinámicos del wallpaper)
