-- ============================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS
-- AddVenture - Estructura completa
-- ============================================
-- Ejecutar en el servidor de producción después de:
-- CREATE DATABASE addventure CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE addventure;

-- IMPORTANTE: Este script solo crea la estructura
-- Los datos de prueba NO se incluyen

-- ============================================
-- INSTRUCCIONES PARA GENERAR ESTE ARCHIVO
-- ============================================
-- En tu MySQL local, ejecuta:
-- mysqldump -u root -p --no-data --skip-triggers addventure > database-structure.sql
--
-- Esto genera SOLO la estructura de todas las tablas
-- sin incluir los datos de prueba

-- ============================================
-- PARA IMPORTAR EN PRODUCCIÓN
-- ============================================
-- 1. Conectarse al servidor EC2:
--    ssh -i ~/.ssh/addventure-key.pem ubuntu@TU-IP-PUBLICA
--
-- 2. Subir el archivo (desde tu PC local):
--    scp -i ~/.ssh/addventure-key.pem database-structure.sql ubuntu@TU-IP-PUBLICA:~/
--
-- 3. En el servidor, importar:
--    mysql -u addventure_user -p addventure < ~/database-structure.sql

-- ============================================
-- ALTERNATIVA: Dejar que Hibernate cree las tablas
-- ============================================
-- Si prefieres que Spring Boot cree las tablas automáticamente:
--
-- 1. En application-prod.properties cambiar temporalmente:
--    spring.jpa.hibernate.ddl-auto=update
--
-- 2. Iniciar la aplicación (creará todas las tablas)
--
-- 3. Cambiar de vuelta a:
--    spring.jpa.hibernate.ddl-auto=validate
--
-- 4. Reiniciar la aplicación

-- ============================================
-- DATOS INICIALES RECOMENDADOS
-- ============================================

-- Usuario administrador (cambiar la contraseña)
-- Nota: La contraseña debe estar encriptada con BCrypt
-- Puedes usar: https://bcrypt-generator.com/
-- Ejemplo: "admin123" encriptado = $2a$10$...

INSERT INTO Usuario (username, email, password, nombre, apellido, pais, ciudad, fecha_nacimiento, verificado, rol)
VALUES 
('admin', 'admin@addventure.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
 'Administrador', 'Sistema', 'Colombia', 'Bogotá', '1990-01-01', TRUE, 'ADMIN');

-- Nota: La contraseña del ejemplo es "admin123" (¡CAMBIARLA en producción!)

-- ============================================
-- VERIFICACIÓN POST-IMPORTACIÓN
-- ============================================
-- Ejecutar después de importar:

-- Ver todas las tablas creadas
SHOW TABLES;

-- Verificar estructura de tabla Usuario
DESCRIBE Usuario;

-- Verificar que el admin se creó
SELECT username, email, rol FROM Usuario WHERE rol = 'ADMIN';

-- ============================================
-- MANTENIMIENTO
-- ============================================

-- Backup manual
-- mysqldump -u addventure_user -p addventure > backup_$(date +%Y%m%d).sql

-- Restaurar backup
-- mysql -u addventure_user -p addventure < backup_20250127.sql
