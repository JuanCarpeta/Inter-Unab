package com.silvia.beautycare.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migraciones minimas para bases de datos creadas antes de existir una columna.
 * schema.sql usa CREATE TABLE IF NOT EXISTS, que no altera tablas ya creadas,
 * asi que aqui agregamos a mano lo que falte.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EsquemaMigracion implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        agregarColumnaSiFalta("item", "publicado_por", "TEXT");
    }

    private void agregarColumnaSiFalta(String tabla, String columna, String tipo) {
        if (!existeTabla(tabla)) return;

        List<String> columnas = jdbc.query(
                "PRAGMA table_info(" + tabla + ")",
                (rs, i) -> rs.getString("name"));

        boolean existe = columnas.stream().anyMatch(c -> c.equalsIgnoreCase(columna));
        if (!existe) {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + tipo);
            log.info("Migracion aplicada: {}.{} ({}) agregada", tabla, columna, tipo);
        }
    }

    private boolean existeTabla(String tabla) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Long.class, tabla);
        return n != null && n > 0;
    }
}
