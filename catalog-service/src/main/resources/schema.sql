CREATE TABLE IF NOT EXISTS libros (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(30) NOT NULL UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    autor VARCHAR(150) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    stock_total INT NOT NULL,
    stock_disponible INT NOT NULL,
    CONSTRAINT chk_stock_disponible CHECK (stock_disponible >= 0)
);