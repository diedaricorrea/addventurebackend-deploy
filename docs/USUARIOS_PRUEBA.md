# 👥 Usuarios de Prueba - AddVenture

## 🔐 Credenciales de Acceso

### Usuario Administrador
- **Email:** `admin@addventure.com`
- **Contraseña:** `admin123`
- **Nombre:** Carlos Administrador
- **Ciudad:** Bogotá, Colombia
- **Estado:** Verificado ✅
- **Descripción:** Usuario administrador para pruebas del sistema

### Usuario Regular
- **Email:** `maria@addventure.com`
- **Contraseña:** `maria123`
- **Nombre:** María Viajera
- **Ciudad:** Medellín, Colombia
- **Estado:** Verificado ✅
- **Descripción:** Apasionada por los viajes de aventura y la fotografía

## 🚀 Uso

1. **Inicia la aplicación:**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Los usuarios se crean automáticamente** al iniciar la aplicación

3. **Accede con cualquiera de las credenciales** en `/auth/login`

## ⚠️ IMPORTANTE

- **ELIMINAR ANTES DE PRODUCCIÓN**
- Estos usuarios se crean automáticamente en el `DataLoader`
- Para eliminar, comentar la llamada `cargarUsuariosDePrueba()` en `DataLoader.java`
- O borrar todo el método y la sección marcada con comentarios

## 🧪 Casos de Prueba Sugeridos

### Con el Usuario Admin (Carlos)
- Crear grupos de viaje
- Gestionar miembros
- Probar permisos de líder
- Acceder al chat como creador

### Con el Usuario Regular (María)
- Unirse a grupos creados por Carlos
- Probar permisos de miembro
- Enviar mensajes en el chat
- Probar solicitudes de unión

## 🗂️ Ubicación del Código

Los usuarios se definen en:
```
src/main/java/com/add/venture/config/DataLoader.java
```

Busca la sección:
```java
// ==========================================
// USUARIOS DE PRUEBA - COMENTAR/ELIMINAR EN PRODUCCIÓN
// ==========================================
``` 