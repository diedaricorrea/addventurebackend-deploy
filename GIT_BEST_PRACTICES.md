# Git Best Practices - ¿Qué subir y qué no?

## ✅ SÍ debes subir a Git

### Archivos de configuración con valores por defecto

1. **`application.properties`** ✅
   - Contiene configuración de **desarrollo**
   - URLs: `http://localhost:8080`, `http://localhost:4200`
   - Base de datos local: `localhost:3306`
   - Otros desarrolladores lo necesitan para trabajar
   
2. **`application-prod.properties`** ✅
   - Plantilla para producción
   - Usa placeholders: `${DB_PASSWORD}`, `${JWT_SECRET}`
   - NO contiene secretos reales

3. **`environment.ts` (Angular)** ✅
   - URLs de desarrollo: `http://localhost:8080/api`
   - Necesario para compilar el proyecto

4. **`environment.prod.ts` (Angular)** ✅
   - Plantilla con URLs de ejemplo
   - Se modificará en el servidor de producción

### Código fuente
- Todos los archivos `.java`, `.ts`, `.html`, `.css`
- Archivos de configuración del proyecto: `pom.xml`, `package.json`
- Documentación: `README.md`, `*.md`

---

## ❌ NO debes subir a Git

### Archivos con secretos REALES

1. **`.env` o `.env.local`** ❌
   - Si creas archivos con variables de entorno locales
   - Ejemplo:
   ```env
   DB_PASSWORD=mi-contraseña-real
   JWT_SECRET=mi-secreto-super-seguro
   MAIL_PASSWORD=mi-password-de-gmail
   ```

2. **Archivos de configuración personalizados** ❌
   - `application-local.properties` (si lo creas)
   - Cualquier archivo con tus credenciales personales

### Archivos generados
- `/target/` (Java)
- `/node_modules/` (Node.js)
- `/dist/` (Angular compilado)
- `/uploads/` (archivos subidos por usuarios)
- `*.log` (logs de la aplicación)

---

## 📝 Ejemplo: ¿Cómo manejar secretos?

### ❌ MAL - Guardar secretos en el archivo
```properties
# application.properties - NO HACER ESTO
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
spring.mail.password=qhrqavxjsoblzwlx
```

### ✅ BIEN - Usar variables de entorno en producción

**En `application-prod.properties`:**
```properties
jwt.secret=${JWT_SECRET}
spring.mail.password=${MAIL_PASSWORD}
```

**En el servidor de producción:**
```bash
export JWT_SECRET=tu-secreto-generado-con-openssl
export MAIL_PASSWORD=tu-password-real
```

---

## 🔐 Generar secretos seguros

Para JWT o cualquier secreto:

```bash
# Linux/Mac
openssl rand -base64 64

# PowerShell (Windows)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

---

## 📋 Checklist antes de hacer commit

- [ ] ¿Estoy subiendo contraseñas reales? → **NO**
- [ ] ¿Estoy subiendo tokens/API keys reales? → **NO**
- [ ] ¿Estoy subiendo archivos de configuración con valores por defecto? → **SÍ**
- [ ] ¿Estoy subiendo código fuente? → **SÍ**
- [ ] ¿El `.gitignore` está bien configurado? → **SÍ**

---

## 🚀 Configuración actual del proyecto

### Ya configurado correctamente ✅

**Backend:**
- ✅ `application.properties` tiene valores de desarrollo (localhost)
- ✅ `application-prod.properties` usa variables de entorno
- ✅ `.gitignore` ignora `/uploads/`, `.env`, logs

**Frontend:**
- ✅ `environment.ts` tiene `http://localhost:4200`
- ✅ `environment.prod.ts` tiene placeholders para cambiar
- ✅ `.gitignore` ignora `/node_modules/`, `/dist/`

### Lo que debes cambiar en producción

1. **Servidor de producción**: Configurar variables de entorno
   ```bash
   export CORS_ALLOWED_ORIGINS=https://tuapp.com
   export JWT_SECRET=$(openssl rand -base64 64)
   export DB_PASSWORD=tu-password-real
   ```

2. **Build del frontend**: Editar `environment.prod.ts` antes de compilar
   ```typescript
   apiUrl: 'https://api.tuapp.com/api'
   ```

---

## 🎯 Resumen

| Archivo | ¿Subir a Git? | ¿Por qué? |
|---------|---------------|-----------|
| `application.properties` | ✅ SÍ | Valores por defecto (localhost) |
| `application-prod.properties` | ✅ SÍ | Plantilla con placeholders |
| `.env` con secretos reales | ❌ NO | Contiene contraseñas |
| `environment.ts` | ✅ SÍ | Configuración de desarrollo |
| `environment.prod.ts` | ✅ SÍ | Plantilla para producción |
| `/uploads/` | ❌ NO | Archivos de usuarios |
| `/target/` | ❌ NO | Archivos compilados |

**Tu proyecto ya está configurado correctamente.** Solo sube los archivos que ya están versionados y usa variables de entorno en producción. 🎉
