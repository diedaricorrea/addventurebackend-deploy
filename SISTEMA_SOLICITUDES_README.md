# Sistema de Solicitudes de Unión a Grupos

## Descripción General

Este sistema permite a los usuarios solicitar unirse a grupos de viaje, con un flujo de aprobación por parte del líder del grupo y notificaciones en tiempo real.

## Arquitectura

### Backend (Spring Boot)

#### Endpoints REST

##### 1. Solicitar unirse a un grupo
```
POST /api/grupos/{id}/unirse
```

**Validaciones:**
- Usuario autenticado
- No es el creador del grupo
- El grupo tiene capacidad disponible (participantesAceptados + 1 < maxParticipantes)
- Manejo de estados:
  - Si ya es ACEPTADO → Error "Ya eres miembro"
  - Si está PENDIENTE → Error "Ya tienes solicitud pendiente"
  - Si fue RECHAZADO:
    - Verifica intentos < 3
    - Permite reintentar incrementando contador
    - Error si alcanzó el límite de 3 intentos

**Flujo:**
1. Valida usuario y grupo
2. Verifica capacidad
3. Busca solicitud existente (ParticipanteGrupo)
4. Si es nueva solicitud:
   - Crea ParticipanteGrupo con estado PENDIENTE e intentos=1
   - Guarda en BD
5. Si es reintento:
   - Actualiza estado a PENDIENTE
   - Incrementa intentosSolicitud
6. Envía notificación WebSocket al líder del grupo
7. Retorna mensaje de éxito

**Response:**
```json
{
  "success": true,
  "message": "Solicitud enviada exitosamente al líder del grupo"
}
```

##### 2. Obtener solicitudes pendientes (solo líder)
```
GET /api/grupos/{id}/solicitudes-pendientes
```

**Autorización:** Solo el creador del grupo puede acceder

**Response:**
```json
{
  "solicitudes": [
    {
      "idUsuario": 5,
      "nombreCompleto": "Juan Pérez",
      "email": "juan@example.com",
      "fotoPerfil": "profile.jpg",
      "iniciales": "JP",
      "fechaSolicitud": "2024-01-15T10:30:00",
      "intentos": 1
    }
  ],
  "total": 1
}
```

##### 3. Aceptar solicitud
```
POST /api/grupos/{id}/solicitudes/{idUsuario}/aceptar
```

**Validaciones:**
- Usuario autenticado es el líder
- Solicitud existe y está PENDIENTE
- Hay capacidad en el grupo

**Flujo:**
1. Actualiza ParticipanteGrupo.estadoSolicitud = ACEPTADO
2. Asigna rol MIEMBRO vía PermisosService
3. Envía notificación al solicitante: "Tu solicitud ha sido aceptada"
4. Retorna éxito

##### 4. Rechazar solicitud
```
POST /api/grupos/{id}/solicitudes/{idUsuario}/rechazar
```

**Validaciones:**
- Usuario autenticado es el líder
- Solicitud existe y está PENDIENTE

**Flujo:**
1. Actualiza ParticipanteGrupo.estadoSolicitud = RECHAZADO
2. Remueve cualquier rol existente vía PermisosService
3. Envía notificación con contador de intentos:
   - Si intentos < 3: "Te quedan X intentos más"
   - Si intentos >= 3: "Has alcanzado el límite máximo de 3 intentos"
4. Retorna éxito

#### Notificaciones REST

##### 5. Obtener todas las notificaciones del usuario
```
GET /api/notificaciones
```

**Response:**
```json
{
  "notificaciones": [...],
  "total": 15,
  "noLeidas": 3
}
```

##### 6. Obtener solo no leídas
```
GET /api/notificaciones/no-leidas
```

##### 7. Contar no leídas
```
GET /api/notificaciones/contador
```

**Response:**
```json
{
  "contador": 3
}
```

##### 8. Marcar como leída
```
PUT /api/notificaciones/{id}/leer
```

##### 9. Marcar todas como leídas
```
PUT /api/notificaciones/leer-todas
```

##### 10. Eliminar notificación
```
DELETE /api/notificaciones/{id}
```

##### 11. Eliminar todas
```
DELETE /api/notificaciones
```

#### WebSocket

**Topic para notificaciones de usuario:**
```
/queue/notificaciones/{userId}
```

**Tipos de notificaciones:**
- `SOLICITUD_UNION`: Cuando alguien solicita unirse
- `SOLICITUD_ACEPTADA`: Cuando el líder acepta
- `SOLICITUD_RECHAZADA`: Cuando el líder rechaza

**Estructura del mensaje WebSocket:**
```json
{
  "idNotificacion": 123,
  "tipo": "SOLICITUD_UNION",
  "contenido": "Juan Pérez quiere unirse al grupo \"Viaje a París\"",
  "leido": false,
  "fecha": "2024-01-15T10:30:00",
  "grupo": {
    "idGrupo": 5,
    "nombreViaje": "Viaje a París"
  },
  "solicitante": {
    "idUsuario": 10,
    "nombreCompleto": "Juan Pérez",
    "fotoPerfil": "profile.jpg"
  }
}
```

### Base de Datos

#### Modelo ParticipanteGrupo

```java
@Entity
@Table(name = "ParticipanteGrupo")
@IdClass(ParticipanteGrupoId.class)
public class ParticipanteGrupo {
    @Id
    @ManyToOne
    private Usuario usuario;

    @Id
    @ManyToOne
    private GrupoViaje grupo;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estadoSolicitud; // PENDIENTE, ACEPTADO, RECHAZADO

    private Integer intentosSolicitud; // Contador de intentos (máximo 3)

    private String rolParticipante; // "MIEMBRO"

    private LocalDateTime fechaUnion;

    public enum EstadoSolicitud {
        PENDIENTE,
        ACEPTADO,
        RECHAZADO
    }
}
```

#### Modelo Notificacion

```java
@Entity
@Table(name = "Notificacion")
public class Notificacion {
    @Id
    @GeneratedValue
    private Long idNotificacion;

    private String tipo; // SOLICITUD_UNION, SOLICITUD_ACEPTADA, SOLICITUD_RECHAZADA

    private String contenido; // Mensaje legible

    private Boolean leido; // false por defecto

    private LocalDateTime fecha;

    private LocalDateTime fechaLectura;

    @ManyToOne
    private Usuario usuario; // Destinatario

    @ManyToOne
    private GrupoViaje grupo; // Grupo relacionado (opcional)

    @ManyToOne
    private Usuario solicitante; // Usuario que solicita (opcional)
}
```

### Frontend (Angular)

#### Servicios

**SolicitudService:**
```typescript
- unirseGrupo(idGrupo): Observable<any>
- obtenerSolicitudesPendientes(idGrupo): Observable<SolicitudesResponse>
- aceptarSolicitud(idGrupo, idUsuario): Observable<any>
- rechazarSolicitud(idGrupo, idUsuario): Observable<any>
```

**NotificacionService:**
```typescript
- obtenerNotificaciones(): Observable<NotificacionesResponse>
- obtenerNoLeidas(): Observable<NotificacionesResponse>
- contarNoLeidas(): Observable<{contador: number}>
- marcarComoLeida(id): Observable<any>
- marcarTodasComoLeidas(): Observable<any>
- eliminarNotificacion(id): Observable<any>
- eliminarTodas(): Observable<any>
```

**WebSocketService:**
```typescript
- connectNotifications(userId): void
- subscribeToNotifications(userId): Observable<Notificacion>
- disconnect(): void
```

#### Componentes

**GrupoDetalleComponent:**
- Botón "Unirse al grupo" (solo si `permisos.puedeUnirse`)
- Alerta de "Solicitud pendiente" (si `estadoSolicitud === 'PENDIENTE'`)
- Alerta de "Solicitud rechazada" con mensaje de reintentar (si `estadoSolicitud === 'RECHAZADO'`)

**NavbarComponent:**
- Badge de notificaciones con contador en tiempo real
- Dropdown de notificaciones (futuro)

**SolicitudesComponent (pendiente):**
- Lista de solicitudes pendientes para el líder
- Vista previa del perfil del solicitante
- Botones "Aceptar" / "Rechazar"

## Flujo de Usuario Completo

### 1. Usuario solicita unirse

1. Usuario navega a detalle del grupo
2. Ve botón "Unirse al grupo" (si tiene permiso)
3. Hace clic
4. Frontend llama `POST /api/grupos/{id}/unirse`
5. Backend valida y crea ParticipanteGrupo con estado PENDIENTE
6. Backend envía notificación WebSocket al líder
7. Usuario ve mensaje "Solicitud enviada exitosamente"
8. Estado cambia a PENDIENTE (botón desaparece, aparece alerta)

### 2. Líder recibe notificación

1. Líder ve badge en navbar actualizado en tiempo real (WebSocket)
2. Hace clic en el ícono de notificaciones
3. Ve notificación: "Juan Pérez quiere unirse al grupo 'Viaje a París'"
4. Hace clic en "Ver solicitudes del grupo" (o navega directamente)

### 3. Líder revisa solicitudes

1. Líder llama `GET /api/grupos/{id}/solicitudes-pendientes`
2. Ve lista de solicitantes con foto, nombre, email, fecha
3. Puede hacer clic en el perfil para ver más detalles
4. Decide aceptar o rechazar

### 4A. Líder acepta

1. Hace clic en "Aceptar"
2. Frontend llama `POST /api/grupos/{id}/solicitudes/{idUsuario}/aceptar`
3. Backend:
   - Actualiza estado a ACEPTADO
   - Asigna rol MIEMBRO
   - Envía notificación al solicitante
4. Solicitante recibe notificación en tiempo real: "Tu solicitud ha sido aceptada"
5. Ahora puede acceder al chat y todas las funcionalidades de miembro

### 4B. Líder rechaza

1. Hace clic en "Rechazar"
2. Frontend llama `POST /api/grupos/{id}/solicitudes/{idUsuario}/rechazar`
3. Backend:
   - Actualiza estado a RECHAZADO
   - Remueve cualquier rol
   - Envía notificación con contador de intentos
4. Solicitante recibe: "Tu solicitud fue rechazada. Te quedan 2 intentos más"
5. Si vuelve a intentar (máximo 3 veces):
   - Se incrementa `intentosSolicitud`
   - Si llega a 3, no puede volver a intentar

## Configuración de Seguridad

En `SecurityConfig.java`, asegúrate de permitir estos endpoints:

```java
.requestMatchers("/api/grupos/*/unirse").authenticated()
.requestMatchers("/api/grupos/*/solicitudes-pendientes").authenticated()
.requestMatchers("/api/grupos/*/solicitudes/*/aceptar").authenticated()
.requestMatchers("/api/grupos/*/solicitudes/*/rechazar").authenticated()
.requestMatchers("/api/notificaciones/**").authenticated()
```

## Configuración WebSocket

En `WebSocketConfig.java`:

```java
registry.addEndpoint("/ws")
    .setAllowedOrigins(allowedOrigins.split(","))
    .withSockJS();
```

Variables de entorno en `application.properties`:

```properties
websocket.allowed-origins=http://localhost:4200
```

Para producción en `application-prod.properties`:

```properties
websocket.allowed-origins=${WEBSOCKET_ALLOWED_ORIGINS:https://tudominio.com}
```

## Testing Manual

### 1. Probar solicitud de unión

```bash
# Solicitar unirse (autenticado como usuario normal)
curl -X POST http://localhost:8080/api/grupos/1/unirse \
  -H "Authorization: Bearer YOUR_TOKEN"

# Respuesta esperada:
# {"success": true, "message": "Solicitud enviada exitosamente al líder del grupo"}
```

### 2. Probar solicitudes pendientes (como líder)

```bash
curl http://localhost:8080/api/grupos/1/solicitudes-pendientes \
  -H "Authorization: Bearer LIDER_TOKEN"

# Respuesta esperada:
# {"solicitudes": [...], "total": 1}
```

### 3. Probar aceptación

```bash
curl -X POST http://localhost:8080/api/grupos/1/solicitudes/5/aceptar \
  -H "Authorization: Bearer LIDER_TOKEN"

# Respuesta esperada:
# {"success": true, "message": "Solicitud aceptada exitosamente"}
```

### 4. Probar rechazo

```bash
curl -X POST http://localhost:8080/api/grupos/1/solicitudes/5/rechazar \
  -H "Authorization: Bearer LIDER_TOKEN"

# Respuesta esperada:
# {"success": true, "message": "Solicitud rechazada"}
```

### 5. Verificar notificaciones

```bash
curl http://localhost:8080/api/notificaciones \
  -H "Authorization: Bearer YOUR_TOKEN"

# Respuesta esperada:
# {"notificaciones": [...], "total": 5, "noLeidas": 2}
```

## Próximos Pasos (Pendientes)

1. **Componente de notificaciones en tiempo real:**
   - Dropdown en navbar con lista de notificaciones
   - WebSocket subscription a `/queue/notificaciones/{userId}`
   - Actualización automática del badge

2. **Componente de gestión de solicitudes:**
   - Vista para el líder con lista de solicitudes
   - Botones Aceptar/Rechazar
   - Preview del perfil del solicitante
   - Filtros y búsqueda

3. **Mejoras UI/UX:**
   - Animaciones para notificaciones
   - Toast notifications en lugar de alerts
   - Confirmación antes de rechazar
   - Indicador de "intentos restantes" visible

4. **Optimizaciones:**
   - Paginación de solicitudes
   - Caché de notificaciones
   - Lazy loading de perfiles

## Arquitectura de Permisos

El sistema usa `PermisosService` para gestionar roles dentro de cada grupo:

- **LIDER_GRUPO**: Creador del grupo (asignado automáticamente)
- **MIEMBRO**: Usuario aceptado (asignado al aceptar solicitud)

Permisos del MIEMBRO:
- `ACCEDER_CHAT`
- Ver itinerarios
- Ver participantes
- Abandonar grupo
- Calificar viajeros (cuando el viaje se cierra)

Permisos del LIDER_GRUPO (hereda MIEMBRO + adicionales):
- `APROBAR_SOLICITUDES`
- `EDITAR_GRUPO`
- `ELIMINAR_GRUPO`
- `CERRAR_GRUPO`
- `GESTIONAR_PERMISOS`

## Notas de Seguridad

1. **Validación de capacidad:** Siempre verificar antes de aceptar
2. **Autorización:** Solo el líder puede ver/aprobar solicitudes
3. **Límite de intentos:** Evita spam de solicitudes
4. **WebSocket authentication:** Solo usuarios autenticados pueden conectarse
5. **CORS:** Configurar correctamente para producción
