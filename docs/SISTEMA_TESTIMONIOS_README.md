# Sistema de Testimonios - AddVenture

## 📋 Descripción

Sistema completo para que los usuarios dejen testimonios sobre su experiencia con la plataforma AddVenture. Los testimonios son diferentes a las reseñas entre viajeros:

- **Reseñas**: Un viajero califica a OTRO viajero después de un viaje
- **Testimonios**: Un usuario habla sobre su experiencia con la PLATAFORMA AddVenture

## 🎯 Características

### 1. Modal automático después de cerrar viaje
- Cuando el organizador cierra un viaje, aparece automáticamente un modal
- Pregunta: "¿Cómo fue tu experiencia con AddVenture?"
- Sistema de calificación con estrellas (1-5)
- Campo de comentario (20-500 caracteres)
- Opción de anonimato

### 2. Flujo de aprobación
- Todos los testimonios empiezan con `aprobado = false`
- Administrador revisa y aprueba testimonios
- Solo testimonios aprobados son visibles públicamente
- Administrador puede marcar testimonios como "destacados" para el index

### 3. Anonimato opcional
- Usuario decide si quiere aparecer con su nombre o como "Viajero Anónimo"
- Si es anónimo: no se muestra nombre, foto, ni ciudad
- Si NO es anónimo: aparece nombre, foto y ubicación

## 🗄️ Base de Datos

### Tabla: `Testimonio`

```sql
CREATE TABLE Testimonio (
    id_testimonio BIGINT AUTO_INCREMENT PRIMARY KEY,
    comentario TEXT NOT NULL,
    calificacion INT CHECK (calificacion BETWEEN 1 AND 5),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    aprobado BOOLEAN DEFAULT FALSE,
    destacado BOOLEAN DEFAULT FALSE,
    anonimo BOOLEAN DEFAULT FALSE,
    id_autor BIGINT NOT NULL,
    id_grupo BIGINT,
    FOREIGN KEY (id_autor) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_grupo) REFERENCES GrupoViaje(id_grupo)
);
```

### Índices
- `idx_testimonio_aprobado`: Para filtrar aprobados rápidamente
- `idx_testimonio_destacado`: Para obtener destacados
- `idx_testimonio_fecha`: Ordenamiento por fecha descendente

## 🔌 Endpoints REST

### Públicos

#### `GET /api/testimonios/destacados?limit=6`
Obtiene testimonios destacados para mostrar en el index
- Respuesta: Array de `TestimonioDTO`
- Solo devuelve `aprobado = true AND destacado = true`
- Ordenados por fecha DESC

#### `GET /api/testimonios/aprobados?limit=20`
Obtiene todos los testimonios aprobados
- Para página de testimonios completa
- Solo `aprobado = true`

### Autenticados

#### `POST /api/testimonios`
Crear un nuevo testimonio
```json
{
  "comentario": "Gracias a AddVenture...",
  "calificacion": 5,
  "anonimo": false,
  "idGrupo": 123
}
```
- Validaciones:
  - `comentario`: 20-500 caracteres
  - `calificacion`: 1-5
  - `anonimo`: boolean requerido

### Admin

#### `GET /api/testimonios/pendientes`
Lista testimonios pendientes de aprobación

#### `PUT /api/testimonios/{id}/aprobar`
Aprobar un testimonio

#### `PUT /api/testimonios/{id}/destacar?destacado=true`
Marcar/desmarcar como destacado

#### `DELETE /api/testimonios/{id}`
Eliminar testimonio

## 🎨 Frontend

### Componentes modificados

#### `GrupoDetalleComponent`
- Método `cerrarViaje()` modificado para mostrar modal
- Nuevo modal de testimonio con:
  - Calificación con estrellas interactivas
  - Textarea con contador (20-500 caracteres)
  - Checkbox de anonimato
  - Validaciones en tiempo real

### Servicios

#### `TestimonioService`
```typescript
crearTestimonio(request): Observable<any>
getTestimoniosDestacados(limit): Observable<Testimonio[]>
getTestimoniosAprobados(limit): Observable<Testimonio[]>
```

### Modelos

#### `Testimonio`
```typescript
interface Testimonio {
  idTestimonio?: number;
  comentario: string;
  calificacion: number;
  anonimo: boolean;
  nombreAutor?: string;
  apellidoAutor?: string;
  ciudadAutor?: string;
  paisAutor?: string;
  fotoPerfilAutor?: string;
  fecha?: Date;
  aprobado?: boolean;
  destacado?: boolean;
  idGrupo?: number;
}
```

## 🔄 Flujo de Usuario

1. **Usuario cierra un viaje**
   - Click en "Cerrar viaje"
   - Confirmación
   - Si confirma → viaje se cierra
   - Espera 1 segundo → aparece modal de testimonio

2. **Modal de testimonio**
   - Usuario selecciona estrellas (1-5)
   - Escribe su experiencia (mínimo 20 caracteres)
   - Decide si quiere ser anónimo
   - Click "Enviar testimonio"

3. **Después de enviar**
   - Mensaje: "¡Gracias por tu testimonio!"
   - Info: "Será revisado por nuestro equipo"
   - Espera 3 segundos → modal se cierra
   - Página se recarga

4. **Panel de administrador** (pendiente de implementar)
   - Ver testimonios pendientes
   - Aprobar/rechazar
   - Marcar como destacados

## 📊 Uso en Index

En el index HTML monolítico (`templates/index.html`), actualizar la sección de testimonios:

```html
<!-- Antes: Datos estáticos -->
<div class="testimonial-card">
  <h5>Mariana López</h5>
  <p>"Gracias a AddVenture..."</p>
</div>

<!-- Después: Datos dinámicos desde API -->
<script>
fetch('/api/testimonios/destacados?limit=6')
  .then(res => res.json())
  .then(testimonios => {
    // Renderizar testimonios dinámicamente
  });
</script>
```

## ✅ Validaciones

### Backend
- `@Size(min=20, max=500)` en comentario
- `@Min(1) @Max(5)` en calificación
- `@NotNull` en anonimo
- Usuario autenticado requerido

### Frontend
- Contador de caracteres en tiempo real
- Botón deshabilitado si < 20 o > 500 caracteres
- Calificación requerida (mínimo 1 estrella)
- Feedback visual con `is-invalid` class

## 🔐 Seguridad

- Solo usuarios autenticados pueden crear testimonios
- Solo admin puede aprobar/destacar
- Solo autor o admin puede eliminar
- `withCredentials: true` para autenticación con cookies

## 🎭 Anonimato

### Si `anonimo = true`:
```json
{
  "comentario": "Excelente plataforma...",
  "calificacion": 5,
  "nombreAutor": null,
  "apellidoAutor": null,
  "ciudadAutor": null,
  "fotoPerfilAutor": null
}
```

### Si `anonimo = false`:
```json
{
  "comentario": "Excelente plataforma...",
  "calificacion": 5,
  "nombreAutor": "María",
  "apellidoAutor": "García",
  "ciudadAutor": "Lima",
  "paisAutor": "Perú",
  "fotoPerfilAutor": "/uploads/profiles/123.jpg"
}
```

## 📝 TODO

- [ ] Ejecutar script SQL para crear tabla
- [ ] Implementar panel de admin para gestionar testimonios
- [ ] Actualizar index.html para consumir API de testimonios
- [ ] Agregar tests unitarios
- [ ] Agregar paginación en endpoint de aprobados
- [ ] Implementar filtros (por calificación, fecha, etc.)
- [ ] Notificación al usuario cuando su testimonio es aprobado

## 🚀 Instalación

1. **Backend:**
```bash
# Ejecutar script SQL
mysql -u root -p addventure_db < CREATE_TABLA_TESTIMONIO.sql

# Reiniciar aplicación Spring Boot
# Las entidades JPA se mapearán automáticamente
```

2. **Frontend:**
```bash
# Ya están creados los archivos:
# - models/testimonio.model.ts
# - services/testimonio.service.ts
# - Modificaciones en grupo-detalle component

# No requiere npm install adicional
```

## 📸 Screenshots sugeridos

1. Modal de testimonio con estrellas
2. Mensaje de éxito después de enviar
3. Panel de admin (pendiente)
4. Testimonios en index público

---

**Autor:** Sistema de testimonios AddVenture  
**Fecha:** 2025-01-27  
**Versión:** 1.0
