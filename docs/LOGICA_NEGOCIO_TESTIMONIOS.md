# Lógica del Negocio - Sistema de Testimonios

## 🎯 Flujo Completo

### 1. Usuario deja testimonio
```
Usuario cierra viaje 
→ Aparece modal automático
→ Completa formulario (estrellas + comentario + anonimato)
→ Click "Enviar testimonio"
→ Backend guarda con aprobado=FALSE, destacado=FALSE
→ Mensaje: "Tu testimonio será revisado pronto"
```

### 2. Admin revisa testimonios
```sql
-- Ver testimonios pendientes
SELECT * FROM Testimonio WHERE aprobado = FALSE ORDER BY fecha DESC;

-- Revisar contenido y decidir:
-- ✅ Aprobar si el testimonio es apropiado
-- ❌ Rechazar (eliminar) si es spam/inapropiado
```

### 3. Admin aprueba testimonio
```sql
-- Aprobar testimonio con ID 1
UPDATE Testimonio SET aprobado = TRUE WHERE id_testimonio = 1;
```

**Estado actual:**
- ✅ Aprobado: Testimonio válido
- ❌ Destacado: NO aparece en index aún
- 📍 Visible en: `/api/testimonios/aprobados` (futura página de todos los testimonios)

### 4. Admin selecciona los mejores (destacados)
```sql
-- Entre los aprobados, seleccionar los mejores para el index
UPDATE Testimonio SET destacado = TRUE WHERE id_testimonio = 1;

-- O aprobar Y destacar en un solo paso:
UPDATE Testimonio SET aprobado = TRUE, destacado = TRUE WHERE id_testimonio IN (1, 2, 3);
```

**Estado actual:**
- ✅ Aprobado: Sí
- ✅ Destacado: SÍ aparece en index
- 📍 Visible en: 
  - `/api/testimonios/destacados` (index público)
  - `/api/testimonios/aprobados` (todos los testimonios)

### 5. Index muestra testimonios dinámicos

**JavaScript automático:**
```javascript
// Al cargar la página index
fetch('/api/testimonios/destacados?limit=6')
  .then(response => response.json())
  .then(testimonios => {
    // Renderiza tarjetas dinámicamente
    testimonios.forEach(t => {
      // Muestra nombre o "Viajero Anónimo"
      // Muestra ubicación o "Latinoamérica"
      // Renderiza estrellas según calificación
    });
  });
```

## 📊 Estados de un Testimonio

| Estado | aprobado | destacado | Visible en index | Visible en /aprobados | Descripción |
|--------|----------|-----------|------------------|----------------------|-------------|
| **Pendiente** | `FALSE` | `FALSE` | ❌ No | ❌ No | Recién creado, esperando revisión |
| **Aprobado** | `TRUE` | `FALSE` | ❌ No | ✅ Sí | Válido pero no seleccionado para index |
| **Destacado** | `TRUE` | `TRUE` | ✅ Sí | ✅ Sí | Los mejores, aparecen en index público |
| **Rechazado** | - | - | ❌ No | ❌ No | Eliminado de la BD |

## 🔐 Permisos

### Endpoints públicos (sin login):
- `GET /api/testimonios/destacados?limit=6` - Ver testimonios del index
- `GET /api/testimonios/aprobados?limit=20` - Ver todos los aprobados

### Endpoints autenticados:
- `POST /api/testimonios` - Crear testimonio (cualquier usuario logueado)

### Endpoints admin (futuro):
- `GET /api/testimonios/pendientes` - Ver pendientes de aprobación
- `PUT /api/testimonios/{id}/aprobar` - Aprobar testimonio
- `PUT /api/testimonios/{id}/destacar?destacado=true` - Marcar como destacado
- `DELETE /api/testimonios/{id}` - Eliminar testimonio

## 🎨 Anonimato

### Si `anonimo = TRUE`:
```json
{
  "comentario": "Excelente experiencia con AddVenture...",
  "calificacion": 5,
  "nombreAutor": null,
  "apellidoAutor": null,
  "ciudadAutor": null,
  "paisAutor": null,
  "fotoPerfilAutor": null
}
```

**Se muestra en index como:**
- Nombre: "Viajero Anónimo"
- Ubicación: "Latinoamérica"
- Avatar: "?" en círculo de color

### Si `anonimo = FALSE`:
```json
{
  "comentario": "Excelente experiencia con AddVenture...",
  "calificacion": 5,
  "nombreAutor": "María",
  "apellidoAutor": "García",
  "ciudadAutor": "Lima",
  "paisAutor": "Perú",
  "fotoPerfilAutor": "/uploads/profiles/123.jpg"
}
```

**Se muestra en index como:**
- Nombre: "María G."
- Ubicación: "Lima, Perú"
- Avatar: Foto de perfil o iniciales "MG"

## 🚀 Proceso de Gestión Diaria

### Para el Administrador:

#### 1️⃣ Revisar nuevos testimonios (diario)
```sql
SELECT 
    id_testimonio,
    SUBSTRING(comentario, 1, 50) as comentario_preview,
    calificacion,
    CONCAT(u.nombre, ' ', u.apellidos) as autor,
    fecha
FROM Testimonio t
JOIN Usuario u ON t.id_autor = u.id_usuario
WHERE t.aprobado = FALSE
ORDER BY t.fecha DESC;
```

#### 2️⃣ Aprobar testimonios apropiados
```sql
-- Aprobar testimonio ID 5
UPDATE Testimonio SET aprobado = TRUE WHERE id_testimonio = 5;
```

#### 3️⃣ Seleccionar los mejores para destacar
```sql
-- Marcar como destacado (solo si ya está aprobado)
UPDATE Testimonio SET destacado = TRUE WHERE id_testimonio = 5;
```

#### 4️⃣ Mantener ~6 testimonios destacados
```sql
-- Ver cuántos destacados hay actualmente
SELECT COUNT(*) FROM Testimonio WHERE aprobado = TRUE AND destacado = TRUE;

-- Si hay más de 6, desmarcar los más antiguos
UPDATE Testimonio 
SET destacado = FALSE 
WHERE id_testimonio IN (
    SELECT id_testimonio FROM (
        SELECT id_testimonio 
        FROM Testimonio 
        WHERE aprobado = TRUE AND destacado = TRUE 
        ORDER BY fecha ASC 
        LIMIT 3
    ) as subquery
);
```

#### 5️⃣ Eliminar spam o contenido inapropiado
```sql
DELETE FROM Testimonio WHERE id_testimonio = 99;
```

## 📈 Métricas Útiles

### Estadísticas generales
```sql
SELECT 
    COUNT(*) as total_testimonios,
    SUM(CASE WHEN aprobado = TRUE THEN 1 ELSE 0 END) as aprobados,
    SUM(CASE WHEN destacado = TRUE THEN 1 ELSE 0 END) as destacados,
    SUM(CASE WHEN aprobado = FALSE THEN 1 ELSE 0 END) as pendientes,
    AVG(calificacion) as calificacion_promedio
FROM Testimonio;
```

### Testimonios por calificación
```sql
SELECT 
    calificacion,
    COUNT(*) as cantidad,
    COUNT(*) * 100.0 / (SELECT COUNT(*) FROM Testimonio WHERE aprobado = TRUE) as porcentaje
FROM Testimonio
WHERE aprobado = TRUE
GROUP BY calificacion
ORDER BY calificacion DESC;
```

### Usuarios con más testimonios
```sql
SELECT 
    u.nombre,
    u.apellidos,
    COUNT(t.id_testimonio) as testimonios_dados
FROM Usuario u
JOIN Testimonio t ON u.id_usuario = t.id_autor
GROUP BY u.id_usuario
ORDER BY testimonios_dados DESC
LIMIT 10;
```

## 🔄 Ciclo de Vida de un Testimonio

```
1. CREACIÓN
   └─> Usuario cierra viaje
       └─> Modal de testimonio
           └─> Envío POST /api/testimonios
               └─> DB: aprobado=FALSE, destacado=FALSE

2. REVISIÓN
   └─> Admin consulta pendientes
       └─> Lee comentario y contexto
           ├─> ✅ APROBAR
           │   └─> UPDATE aprobado=TRUE
           │       └─> Testimonio entra a pool de aprobados
           │
           └─> ❌ RECHAZAR
               └─> DELETE FROM Testimonio
               
3. SELECCIÓN
   └─> Admin revisa aprobados
       └─> Selecciona los mejores
           └─> UPDATE destacado=TRUE
               └─> Testimonio aparece en index

4. PUBLICACIÓN
   └─> Index carga automáticamente
       └─> GET /api/testimonios/destacados
           └─> JavaScript renderiza tarjetas
               └─> Usuarios ven testimonios reales
```

## 🎯 Criterios de Selección (Recomendados)

### Para APROBAR un testimonio:
✅ Habla sobre la plataforma AddVenture (no sobre un viajero específico)
✅ Tiene al menos 20 caracteres
✅ No contiene spam, enlaces, o lenguaje ofensivo
✅ Es una experiencia genuina
✅ Calificación entre 3-5 estrellas

### Para DESTACAR un testimonio:
⭐ Calificación de 4-5 estrellas
⭐ Comentario específico y detallado
⭐ Menciona beneficios concretos de la plataforma
⭐ Tiene buen balance entre aprobados anónimos y no anónimos
⭐ Diversidad geográfica (diferentes países)

## 🛠️ Comandos Rápidos para Admin

```sql
-- Aprobar los últimos 5 testimonios de 5 estrellas
UPDATE Testimonio 
SET aprobado = TRUE 
WHERE calificacion = 5 AND aprobado = FALSE 
ORDER BY fecha DESC 
LIMIT 5;

-- Destacar solo los aprobados con 5 estrellas
UPDATE Testimonio 
SET destacado = TRUE 
WHERE aprobado = TRUE AND calificacion = 5 AND destacado = FALSE 
LIMIT 6;

-- Rotar testimonios destacados (quitar los más viejos)
UPDATE Testimonio 
SET destacado = FALSE 
WHERE id_testimonio IN (
    SELECT * FROM (
        SELECT id_testimonio 
        FROM Testimonio 
        WHERE destacado = TRUE 
        ORDER BY fecha ASC 
        LIMIT 2
    ) as tmp
);
```

---

**Resultado Final:** Index público con testimonios 100% reales, aprobados por admin, que se actualizan dinámicamente sin necesidad de modificar código HTML.
