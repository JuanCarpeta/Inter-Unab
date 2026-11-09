package com.silvia.beautycare.repository;

import com.silvia.beautycare.model.Mensaje;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** Mensajes del chat de una propuesta de intercambio. */
@Repository
@RequiredArgsConstructor
public class MensajeRepository {

    private static final String COLUMNAS = "id, intercambio_id, autor, texto, fecha";

    private static final RowMapper<Mensaje> MAPPER = (rs, fila) -> Mensaje.builder()
            .id(rs.getLong("id"))
            .intercambioId(rs.getLong("intercambio_id"))
            .autor(rs.getString("autor"))
            .texto(rs.getString("texto"))
            .fecha(rs.getObject("fecha", LocalDateTime.class))
            .build();

    private final JdbcTemplate jdbc;

    public List<Mensaje> listarPorIntercambio(long intercambioId) {
        return jdbc.query(
                "SELECT " + COLUMNAS + " FROM intercambio_mensaje "
              + "WHERE intercambio_id = ? ORDER BY fecha, id",
                MAPPER, intercambioId);
    }

    public void crear(Mensaje m) {
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO intercambio_mensaje (intercambio_id, autor, texto, fecha) VALUES (?, ?, ?, ?)",
                m.getIntercambioId(), m.getAutor(), m.getTexto(), ahora);

        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        m.setId(id);
        m.setFecha(ahora);
    }
}
