# 🔐 Sistema de Roles y Permisos - AddVenture

## 📋 Descripción General

Este sistema permite una gestión granular de permisos en grupos de viaje, solucionando problemas como:
- ✅ El creador del grupo no podía acceder al chat
- ✅ Falta de control granular sobre quién puede hacer qué
- ✅ Lógica de permisos dispersa por todo el código

## 🏗️ Arquitectura

### Entidades Principales
```
Usuario ←→ UsuarioRolGrupo ←→ GrupoViaje
              ↓
            Rol ←→ Permiso
```

### Roles del Sistema
1. **ADMIN_SISTEMA** - Todos los permisos
2. **LIDER_GRUPO** - Puede gestionar completamente el grupo
3. **CO_LIDER** - Puede ayudar en la gestión básica
4. **MIEMBRO** - Permisos básicos de participación
5. **MIEMBRO_PREMIUM** - Permisos extendidos

### Permisos Disponibles
- `ACCEDER_CHAT` - Acceder al chat del grupo
- `ENVIAR_MENSAJES` - Enviar mensajes al chat
- `EDITAR_GRUPO` - Modificar información del grupo
- `ELIMINAR_GRUPO` - Eliminar el grupo completo
- `EXPULSAR_MIEMBROS` - Expulsar miembros del grupo
- `ASIGNAR_ROLES` - Asignar roles a otros miembros
- Y más...

## 🚀 Uso en Controladores

```java
@Autowired
private IPermisosService permisosService;

// Verificar si un usuario puede editar un grupo
if (permisosService.usuarioTienePermiso(usuario, grupo, "EDITAR_GRUPO")) {
    // Permitir edición
}

// Verificar si es el creador
if (permisosService.esCreadorDelGrupo(usuario, grupo)) {
    // Lógica para creador
}
```

## 🎨 Uso en Plantillas Thymeleaf

```html
<!-- Mostrar botón solo si tiene permiso -->
<button th:if="${@permisos.puedeEditarGrupo(grupo.idGrupo)}">
    Editar Grupo
</button>

<!-- Verificar permiso específico -->
<div th:if="${@permisos.tienePermiso(grupo.idGrupo, 'EXPULSAR_MIEMBROS')}">
    <button>Expulsar Miembro</button>
</div>

<!-- Mostrar rol del usuario -->
<span th:text="${@permisos.obtenerRol(grupo.idGrupo)}"></span>
```

## 🔄 Flujo de Creación de Grupo

1. Usuario crea grupo → Se asigna automáticamente como LIDER_GRUPO
2. LIDER_GRUPO puede invitar/aprobar miembros
3. LIDER_GRUPO puede asignar roles (CO_LIDER, MIEMBRO_PREMIUM)
4. Cada rol tiene permisos específicos predefinidos

## 🛠️ Métodos Principales del Servicio

### Verificación de Permisos
- `usuarioTienePermiso(usuario, grupo, permiso)`
- `esCreadorDelGrupo(usuario, grupo)`
- `puedeGestionarUsuario(gestor, objetivo, grupo)`

### Gestión de Roles
- `asignarRolEnGrupo(usuario, grupo, rol, asignadoPor)`
- `cambiarRolEnGrupo(usuario, grupo, nuevoRol, cambiadoPor)`
- `removerRolEnGrupo(usuario, grupo, removidoPor)`

### Helper para Thymeleaf
- `@permisos.puedeAccederChat(idGrupo)`
- `@permisos.puedeEditarGrupo(idGrupo)`
- `@permisos.esCreador(idGrupo)`

## 📊 Base de Datos

### Tablas Creadas Automáticamente
- `rol` - Roles del sistema
- `permiso` - Permisos disponibles
- `usuario_rol_grupo` - Asignación de roles por grupo
- `rol_permiso` - Relación roles-permisos

### Carga Inicial
Al iniciar la aplicación, `DataLoader` carga automáticamente:
- Todos los permisos del enum `PermisosSistema`
- Todos los roles del enum `RolesSistema`
- Asignación de permisos a roles

## 🔧 Próximas Mejoras Sugeridas

1. **Admin Panel** - Interfaz para gestionar roles y permisos
2. **Roles Personalizados** - Permitir crear roles específicos por grupo
3. **Historial de Cambios** - Auditoría de cambios de roles
4. **Notificaciones** - Notificar cambios de rol a usuarios

## 🆘 Problemas Solucionados

- ✅ **Creador sin acceso al chat** - Ahora el creador tiene automáticamente el rol LIDER_GRUPO
- ✅ **Verificaciones dispersas** - Centralizadas en `PermisosService`
- ✅ **Control granular** - Cada acción tiene su permiso específico
- ✅ **Jerarquía de roles** - Los roles superiores pueden gestionar inferiores 