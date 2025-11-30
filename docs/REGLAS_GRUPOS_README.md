# 📋 Guía de Reglas para Gestión de Grupos de Viaje - AddVenture

## 📖 Índice
- [Reglas de Edición](#-reglas-de-edición)
- [Reglas de Eliminación](#-reglas-de-eliminación)
- [Sistema de Permisos](#-sistema-de-permisos)
- [Restricciones Temporales](#-restricciones-temporales)
- [Notificaciones](#-notificaciones)
- [Casos Especiales](#-casos-especiales)

---

## ✏️ Reglas de Edición

### 🔐 **Permisos Requeridos**
- **Creador del grupo**: Siempre puede editar (sujeto a restricciones temporales)
- **Usuarios con rol**: Deben tener el permiso `EDITAR_GRUPO`

### ⏰ **Restricciones Temporales**
| Tiempo transcurrido | Acción permitida | Observaciones |
|-------------------|------------------|---------------|
| **< 24 horas** | ✅ Edición completa | Todas las características del grupo |
| **≥ 24 horas** | ❌ Edición bloqueada | Se muestra página `editar-grupo-bloqueado` |

### 📝 **Campos Editables (dentro del período permitido)**
- ✅ Nombre del viaje
- ✅ Descripción
- ✅ Destino principal
- ✅ Fechas de inicio y fin
- ✅ Punto de encuentro
- ✅ Rango de edad
- ✅ Máximo de participantes
- ✅ Tipo de viaje
- ✅ Etiquetas
- ✅ **Itinerario completo** (con actualización inteligente)
- ✅ Imagen destacada

### 🔄 **Actualización Inteligente de Itinerarios**
El sistema implementa una estrategia eficiente para manejar cambios en el itinerario:

```
📍 Estrategia de Actualización:
├── 🔍 Comparar itinerarios existentes vs nuevos
├── ✏️ Actualizar días existentes (mantiene IDs)
├── ➕ Crear días nuevos
└── 🗑️ Eliminar días que ya no están
```

**Beneficios:**
- Preserva los IDs de base de datos
- Evita eliminación masiva y recreación
- Mejor rendimiento y trazabilidad

---

## 🗑️ Reglas de Eliminación

### 🔐 **Permisos Requeridos**
- **Creador del grupo**: Siempre puede solicitar eliminación
- **Usuarios con rol**: Deben tener el permiso `ELIMINAR_GRUPO`

### 📊 **3 Reglas de Evaluación**

#### **REGLA 1: Sin Participantes Aceptados**
```
✅ ELIMINACIÓN INMEDIATA
├── Condición: No hay usuarios con estado "ACEPTADO"
├── Acción: Eliminación directa
└── Notificación: Solo a usuarios con solicitudes pendientes
```

#### **REGLA 2: Menos de 24 Horas**
```
✅ ELIMINACIÓN PERMITIDA
├── Condición: < 24 horas desde creación
├── Acción: Eliminación directa con notificaciones
└── Notificación: Todos los participantes (aceptados + pendientes)
```

#### **REGLA 3: Más de 24 Horas + Participantes Activos**
```
❌ ELIMINACIÓN BLOQUEADA
├── Condición: ≥ 24 horas + participantes aceptados
├── Acción: Proceso especial requerido
├── Mensaje: Información de contacto con soporte
└── Alternativa: Votación (próximamente) o justificación especial
```

### 📧 **Notificaciones de Eliminación**
| Tipo de Usuario | Tipo de Notificación | Contenido |
|----------------|---------------------|-----------|
| **Participantes Aceptados** | `GRUPO_ELIMINADO` | Información sobre pérdida de acceso |
| **Solicitudes Pendientes** | `SOLICITUD_CANCELADA` | Cancelación de solicitud |

---

## 🔐 Sistema de Permisos

### 👑 **Creador del Grupo**
- **Privilegios especiales**: Bypass de verificaciones de permisos
- **Auto-asignación**: Rol de `LIDER_GRUPO` automáticamente
- **Permisos completos**: Todas las acciones sobre el grupo

### 🎭 **Roles y Permisos**
| Permiso | Descripción | Roles Típicos |
|---------|-------------|---------------|
| `EDITAR_GRUPO` | Modificar información del grupo | Líder, Co-líder |
| `ELIMINAR_GRUPO` | Solicitar eliminación del grupo | Solo Líder |
| `ASIGNAR_ROLES` | Asignar roles descriptivos | Líder, Co-líder |
| `GESTIONAR_MIEMBROS` | Expulsar/aceptar miembros | Líder, Moderador |
| `ACCEDER_CHAT` | Acceso al chat del grupo | Todos los miembros |
| `ENVIAR_MENSAJES` | Enviar mensajes en el chat | Todos los miembros |
| `COMPARTIR_ARCHIVOS` | Subir archivos/imágenes | Todos los miembros |

### 🏗️ **Verificación de Permisos**
```java
// Ejemplo de verificación
if (permisosService.usuarioTienePermiso(usuario, grupo, "EDITAR_GRUPO")) {
    // Verificación adicional de tiempo
    if (tiempoDesdeCreacion.toHours() < 24) {
        // Permitir edición
    }
}
```

---

## ⏰ Restricciones Temporales

### 📅 **Ventana de 24 Horas**
La ventana de 24 horas es crítica para varias operaciones:

```
🕐 CRONOLOGÍA DE PERMISOS:
│
├── 0h ────────────────────────── 24h ─────────────────▶
│   │                              │
│   └── ✅ Edición Libre           └── ❌ Edición Bloqueada
│   └── ✅ Eliminación Flexible    └── ❌ Proceso Especial
```

### 🎯 **Justificación del Límite**
- **Prevenir cambios arbitrarios** después de que participantes se comprometan
- **Proteger inversiones** de tiempo y planificación de participantes
- **Mantener confianza** en la plataforma
- **Evitar conflictos** por cambios inesperados

---

## 🔔 Notificaciones

### 📧 **Tipos de Notificaciones Automáticas**

#### **Por Edición de Grupo**
- ✅ Cambios significativos → Notificar participantes
- ✅ Cambios de fechas → Alerta especial
- ✅ Cambios de itinerario → Resumen de modificaciones

#### **Por Eliminación de Grupo**
| Escenario | Notificación | Destinatarios |
|-----------|-------------|---------------|
| Sin participantes | `GRUPO_SIN_MIEMBROS` | Solo creador |
| Con participantes < 24h | `GRUPO_ELIMINADO` | Todos |
| Con participantes ≥ 24h | `ELIMINACION_BLOQUEADA` | Solo solicitante |

#### **Por Gestión de Miembros**
- ✅ Nuevas solicitudes → Líder del grupo
- ✅ Aceptación/Rechazo → Solicitante
- ✅ Expulsión → Usuario expulsado
- ✅ Cambios de rol → Usuario afectado

---

## 🚨 Casos Especiales

### 🔄 **Migración de Datos**
- **Itinerarios**: Actualización inteligente preserva IDs
- **Participantes**: Mantiene historial de roles
- **Mensajes**: Se preservan durante ediciones
- **Archivos**: Links se mantienen estables

### 🛡️ **Medidas de Seguridad**
1. **CSRF Protection**: Todos los endpoints requieren token CSRF
2. **Autenticación**: Verificación de usuario en cada operación
3. **Autorización**: Sistema de permisos granular
4. **Validación temporal**: Verificación de ventanas de tiempo
5. **Auditoría**: Log de cambios importantes

### 🔧 **Solución de Problemas**

#### **Error 403 Forbidden**
```
Posibles causas:
├── ❌ Token CSRF faltante o inválido
├── ❌ Usuario sin permisos necesarios
├── ❌ Ventana de tiempo expirada (24h)
└── ❌ Grupo en estado no editable
```

#### **Edición Bloqueada**
```
Soluciones:
├── ⏰ Verificar si han pasado 24 horas
├── 👤 Confirmar permisos del usuario
├── 📞 Contactar soporte para casos especiales
└── 🗳️ Esperar implementación de votaciones
```

---

## 📞 Contacto y Soporte

Para casos que requieren intervención manual:
- **Email**: support@addventure.com
- **Casos especiales**: Eliminación después de 24h con participantes
- **Disputas**: Cambios controvertidos en grupos
- **Técnico**: Problemas con permisos o errores del sistema

---

## 🔄 Próximas Implementaciones

### 🗳️ **Sistema de Votaciones**
- Eliminación por consenso después de 24h
- Cambios mayores por votación
- Threshold configurable (ej: 70% de aprobación)

### 📊 **Dashboard de Gestión**
- Métricas de actividad del grupo
- Historial de cambios
- Análisis de participación

### 🤖 **Automatizaciones**
- Recordatorios automáticos
- Sugerencias de mejora
- Detección de grupos inactivos

---

**Versión del documento**: 1.0  
**Última actualización**: julio del 2025  
**Mantenido por**: Equipo de Desarrollo AddVenture 