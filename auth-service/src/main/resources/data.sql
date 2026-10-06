INSERT IGNORE INTO roles (nombre) VALUES ('ADMIN'), ('BIBLIOTECARIO'), ('LECTOR');

INSERT IGNORE INTO usuarios (nombre, email, password, estado, rol_id)
SELECT 'Administrador', 'admin@biblioteca.com',
       '$2a$10$7xKCgXajPgEqb0fGk6Pts.P/c3bVkmUThZD.aepfUyX6aZZq8jfwu',
       'ACTIVO', id
FROM roles WHERE nombre = 'ADMIN';