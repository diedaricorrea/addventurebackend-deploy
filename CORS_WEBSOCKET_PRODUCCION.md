# Configuración de CORS y WebSocket para Desarrollo y Producción

## ¿Por qué necesitamos esto?

El **CORS (Cross-Origin Resource Sharing)** es una medida de seguridad del navegador que impide que sitios web maliciosos accedan a tus APIs. 

Los **WebSockets** también necesitan configuración de orígenes permitidos para evitar ataques.

---

## Configuración por Ambiente

### 🛠️ Desarrollo (Actual)

**Archivo:** `application.properties`

```properties
cors.allowed-origins=http://localhost:4200
websocket.allowed-origins=http://localhost:4200
```

✅ **Funciona para:** Angular en `http://localhost:4200`

---

### 🚀 Producción

Cuando despliegues tu aplicación, tendrás URLs reales. Por ejemplo:

- **Frontend:** `https://addventure.com`
- **Backend:** `https://api.addventure.com`

#### Opción 1: Usar `application-prod.properties`

**Archivo:** `application-prod.properties` (ya creado)

```properties
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://addventure.com}
websocket.allowed-origins=${WEBSOCKET_ALLOWED_ORIGINS:https://addventure.com}
```

**Ejecutar en producción:**
```bash
java -jar addventure.jar --spring.profiles.active=prod
```

#### Opción 2: Variables de Entorno (Recomendado)

**En tu servidor (Linux/Docker):**
```bash
export CORS_ALLOWED_ORIGINS=https://addventure.com,https://www.addventure.com
export WEBSOCKET_ALLOWED_ORIGINS=https://addventure.com,https://www.addventure.com
```

**Con Docker:**
```yaml
environment:
  - CORS_ALLOWED_ORIGINS=https://addventure.com
  - WEBSOCKET_ALLOWED_ORIGINS=https://addventure.com
```

---

## Múltiples Dominios

Si tienes varios dominios (ej: con y sin `www`, app móvil, etc.):

```properties
cors.allowed-origins=https://addventure.com,https://www.addventure.com,https://app.addventure.com
websocket.allowed-origins=https://addventure.com,https://www.addventure.com,https://app.addventure.com
```

---

## Frontend en Producción

También debes actualizar las URLs del frontend:

### Crear `environment.prod.ts`

**Archivo:** `src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.addventure.com',
  wsUrl: 'https://api.addventure.com/ws'
};
```

**Luego en `websocket.service.ts`:**
```typescript
import { environment } from '../environments/environment';

// En lugar de:
const socket = new SockJS('http://localhost:8080/ws');

// Usar:
const socket = new SockJS(environment.wsUrl);
```

---

## Checklist para Producción

- [ ] Cambiar `cors.allowed-origins` a tu dominio real
- [ ] Cambiar `websocket.allowed-origins` a tu dominio real
- [ ] Usar HTTPS (no HTTP) en producción
- [ ] Configurar certificado SSL/TLS
- [ ] Cambiar `jwt.secret` a un valor aleatorio seguro (mínimo 256 bits)
- [ ] Usar variables de entorno para credenciales sensibles
- [ ] Cambiar `spring.jpa.hibernate.ddl-auto=validate` (no `update`)
- [ ] Desactivar `spring.jpa.show-sql=false`

---

## Ejemplo de Despliegue Completo

### Backend (Spring Boot)
```bash
# Variables de entorno en servidor
export CORS_ALLOWED_ORIGINS=https://addventure.com
export WEBSOCKET_ALLOWED_ORIGINS=https://addventure.com
export JWT_SECRET=tu-secreto-muy-largo-y-aleatorio-generado-con-openssl
export DB_USERNAME=addventure_user
export DB_PASSWORD=contraseña-segura

# Ejecutar aplicación
java -jar addventure.jar --spring.profiles.active=prod
```

### Frontend (Angular)
```bash
# Build para producción
ng build --configuration production

# Los archivos se generan en dist/
# Subir a servidor web (Nginx, Apache, etc.)
```

---

## ¿Tu proyecto ya está preparado?

✅ **SÍ** - Solo necesitas:
1. Cambiar las URLs en `application-prod.properties` cuando tengas tu dominio
2. Usar variables de entorno para valores sensibles
3. Activar el perfil `prod` al desplegar

El código está listo y es seguro para producción. 🚀
