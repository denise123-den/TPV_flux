-- =============================================================================
-- TPV Core (TPV_flux) - Datos Semilla (Mockups Iniciales)
-- Ejecutar después de 01_schema.sql
-- =============================================================================

-- 0. Usuarios del sistema (Login Mockup Página 1)
INSERT INTO usuarios (username, password, nombre_completo, puesto, turno, sucursal, activo) VALUES
    ('ana.martinez', '123456', 'Ana Martínez', 'Cajera', 'Turno AM', 'Sucursal Centro — Caja 1', TRUE),
    ('carlos.ruiz', '123456', 'Carlos Ruiz', 'Supervisor', 'Turno PM', 'Sucursal Centro — Caja 2', TRUE)
ON CONFLICT (username) DO NOTHING;

-- 1. Métodos de Pago
INSERT INTO metodos_pago (nombre, activo) VALUES
    ('EFECTIVO', TRUE),
    ('TARJETA', TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 2. Categorías (basadas en los Mockups de TPV Core)
INSERT INTO categorias (nombre, activo) VALUES
    ('Celulares', TRUE),
    ('Cómputo', TRUE),
    ('Audio', TRUE),
    ('Cables y cargadores', TRUE),
    ('Protección', TRUE),
    ('Accesorios gaming', TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 3. Productos iniciales del catálogo e inventario
-- En la columna 'foto' puedes poner el nombre del archivo dentro de:
-- src/main/resources/imagenes/ (ej. 'iphon.jpeg') o una ruta absoluta en tu PC.
INSERT INTO productos (nombre, descripcion, categoria_id, costo, precio_venta, foto, stock_actual, activo) VALUES
    (
        'Funda iPhone 14',
        'SKU: FN-IP14-001 | TAG: FUNDA | Funda protectora para iPhone 14',
        (SELECT id FROM categorias WHERE nombre = 'Celulares'),
        120.00,
        249.00,
        'iphon.jpeg',
        18,
        TRUE
    ),
    (
        'Cable USB-C 1m',
        'SKU: CB-USBC-100 | TAG: CABLE | Cable de carga rápida USB-C de 1 metro',
        (SELECT id FROM categorias WHERE nombre = 'Cómputo'),
        45.00,
        99.00,
        'USB.jpeg',
        42,
        TRUE
    ),
    (
        'Audífonos Bluetooth X2',
        'SKU: AU-BT-X2 | TAG: AUDIO | Audífonos inalámbricos Bluetooth con estuche de carga',
        (SELECT id FROM categorias WHERE nombre = 'Audio'),
        230.00,
        450.00,
        'audiculares.jpeg',
        6,
        TRUE
    ),
    (
        'Cargador 20W',
        'SKU: CG-20W-002 | TAG: CARGA | Adaptador de corriente USB-C de 20W carga rápida',
        (SELECT id FROM categorias WHERE nombre = 'Cómputo'),
        95.00,
        189.00,
        'Cargador.jpeg',
        25,
        TRUE
    ),
    (
        'Mica templada iPhone 14',
        'SKU: MC-IP14-01 | TAG: MICA | Cristal templado 9H para pantalla de iPhone 14',
        (SELECT id FROM categorias WHERE nombre = 'Celulares'),
        25.00,
        79.00,
        'Mica.jpeg',
        0,
        TRUE
    ),
    (
        'Power bank 10000mAh',
        'SKU: PB-10K-003 | TAG: POWER | Batería portátil de 10000mAh doble salida USB',
        (SELECT id FROM categorias WHERE nombre = 'Cómputo'),
        210.00,
        399.00,
        'Power.jpeg',
        11,
        TRUE
    ),
    (
        'Memoria USB 32GB',
        'SKU: MU-32GB-01 | TAG: USB | Memoria flash USB 3.0 de 32GB metálica',
        (SELECT id FROM categorias WHERE nombre = 'Cómputo'),
        65.00,
        129.00,
        'Memoria.jpeg',
        30,
        TRUE
    ),
    (
        'Soporte celular',
        'SKU: SP-CEL-004 | TAG: SOPORTE | Soporte ajustable de escritorio para celular',
        (SELECT id FROM categorias WHERE nombre = 'Celulares'),
        70.00,
        149.00,
        'Soporte.jpeg',
        14,
        TRUE
    );

-- 4. Movimientos de inventario iniciales
INSERT INTO inventario (producto_id, tipo, cantidad, stock_resultante, fecha) VALUES
    ((SELECT id FROM productos WHERE nombre = 'Funda iPhone 14' LIMIT 1), 'ENTRADA', 50, 18, CURRENT_TIMESTAMP - INTERVAL '4 hours'),
    ((SELECT id FROM productos WHERE nombre = 'Cable USB-C 1m' LIMIT 1), 'SALIDA', 2, 42, CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    ((SELECT id FROM productos WHERE nombre = 'Audífonos Bluetooth X2' LIMIT 1), 'ENTRADA', 20, 6, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ((SELECT id FROM productos WHERE nombre = 'Power bank 10000mAh' LIMIT 1), 'SALIDA', 1, 11, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ((SELECT id FROM productos WHERE nombre = 'Memoria USB 32GB' LIMIT 1), 'ENTRADA', 30, 30, CURRENT_TIMESTAMP - INTERVAL '2 days');

-- 5. Ventas de ejemplo (del turno actual en los mockups)
INSERT INTO ventas (folio_ticket, subtotal, iva, total, metodo_pago_id, estado, fecha_venta) VALUES
    ('#0478', 387.93, 62.07, 450.00, (SELECT id FROM metodos_pago WHERE nombre = 'EFECTIVO'), 'COMPLETADA', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    ('#0479', 162.93, 26.07, 189.00, (SELECT id FROM metodos_pago WHERE nombre = 'TARJETA'), 'COMPLETADA', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    ('#0480', 85.34, 13.66, 99.00, (SELECT id FROM metodos_pago WHERE nombre = 'EFECTIVO'), 'COMPLETADA', CURRENT_TIMESTAMP - INTERVAL '90 minutes'),
    ('#0481', 68.10, 10.90, 79.00, (SELECT id FROM metodos_pago WHERE nombre = 'EFECTIVO'), 'DEVOLUCION', CURRENT_TIMESTAMP - INTERVAL '60 minutes')
ON CONFLICT (folio_ticket) DO NOTHING;

-- 6. Detalle de las ventas de ejemplo
INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario, subtotal) VALUES
    (
        (SELECT id FROM ventas WHERE folio_ticket = '#0478'),
        (SELECT id FROM productos WHERE nombre = 'Audífonos Bluetooth X2' LIMIT 1),
        1,
        450.00,
        450.00
    ),
    (
        (SELECT id FROM ventas WHERE folio_ticket = '#0479'),
        (SELECT id FROM productos WHERE nombre = 'Cargador 20W' LIMIT 1),
        1,
        189.00,
        189.00
    ),
    (
        (SELECT id FROM ventas WHERE folio_ticket = '#0480'),
        (SELECT id FROM productos WHERE nombre = 'Cable USB-C 1m' LIMIT 1),
        1,
        99.00,
        99.00
    );

-- 7. Notificaciones iniciales (basadas en la vista de Notificaciones del mockup)
INSERT INTO notificaciones (titulo, mensaje, leida, fecha_creacion) VALUES
    ('Stock bajo', 'Audífonos Bluetooth X2 tiene solo 6 unidades', FALSE, CURRENT_TIMESTAMP - INTERVAL '12 minutes'),
    ('Caja', 'Recuerda cerrar la caja del turno matutino', FALSE, CURRENT_TIMESTAMP - INTERVAL '38 minutes'),
    ('Devolución', 'Se registró una devolución en el ticket #0481', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    ('Sistema', 'Actualización disponible: TPV Core v1.1', FALSE, CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    ('Venta', 'Venta completada por $450.00 — Ticket #0478', TRUE, CURRENT_TIMESTAMP - INTERVAL '5 hours'),
    ('Stock', 'Mica templada iPhone 14 se quedó sin stock', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('Inventario', 'Se registró entrada de 50 unidades: Funda iPhone 14', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 day');
