CREATE TABLE IF NOT EXISTS prestamos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    libro_id BIGINT NOT NULL,
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE NULL,
    estado VARCHAR(20) NOT NULL,
    INDEX idx_prestamos_usuario (usuario_id),
    INDEX idx_prestamos_estado (estado)
);