package com.silvia.beautycare.repository;

import com.silvia.beautycare.model.EstadoIntercambio;
import com.silvia.beautycare.model.Intercambio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso a la tabla intercambio (propuestas de trueque entre dos items).
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class IntercambioRepository {

    private static final String SELECT_BASE =
            "SELECT x.id, x.item_ofrecido_id, x.item_solicitado_id, x.mensaje, x.estado, "
          + "       x.fecha_propuesta, x.fecha_respuesta, "
          + "       io.nombre AS nombre_ofrecido, isl.nombre AS nombre_solicitado, "
          + "       io.publicado_por AS dueno_ofrecido, isl.publicado_por AS dueno_solicitado "
          + "FROM intercambio x "
          + "JOIN item io  ON io.id  = x.item_ofrecido_id "
          + "JOIN item isl ON isl.id = x.item_solicitado_id ";

    private static final RowMapper<Intercambio> MAPPER = (rs, fila) -> Intercambio.builder()
            .id(rs.getLong("id"))
            .itemOfrecidoId(rs.getLong("item_ofrecido_id"))
            .itemSolicitadoId(rs.getLong("item_solicitado_id"))
            .mensaje(rs.getString("mensaje"))
            .estado(EstadoIntercambio.valueOf(rs.getString("estado")))
            .fechaPropuesta(rs.getObject("fecha_propuesta", LocalDateTime.class))
            .fechaRespuesta(rs.getObject("fecha_respuesta", LocalDateTime.class))
            .nombreOfrecido(rs.getString("nombre_ofrecido"))
            .nombreSolicitado(rs.getString("nombre_solicitado"))
            .duenoOfrecido(rs.getString("dueno_ofrecido"))
            .duenoSolicitado(rs.getString("dueno_solicitado"))
            .build();

    private final JdbcTemplate jdbc;

    // READ: todas las propuestas, la mas reciente primero
    public List<Intercambio> listar() {
        return jdbc.query(SELECT_BASE + "ORDER BY x.fecha_propuesta DESC", MAPPER);
    }

    public Optional<Intercambio> buscarPorId(long id) {
        List<Intercambio> lista = jdbc.query(SELECT_BASE + "WHERE x.id = ?", MAPPER, id);
        return lista.stream().findFirst();
    }

    /** Propuestas pendientes que involucran a un item (como ofrecido o como solicitado). */
    public List<Intercambio> listarPendientesDe(long itemId) {
        return jdbc.query(
                SELECT_BASE + "WHERE x.estado = 'PENDIENTE' "
                            + "AND (x.item_ofrecido_id = ? OR x.item_solicitado_id = ?) "
                            + "ORDER BY x.fecha_propuesta DESC",
                MAPPER, itemId, itemId);
    }

    // CREATE
    public void crear(Intercambio x) {
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO intercambio (item_ofrecido_id, item_solicitado_id, mensaje, estado, fecha_propuesta) "
              + "VALUES (?, ?, ?, ?, ?)",
                x.getItemOfrecidoId(),
                x.getItemSolicitadoId(),
                x.getMensaje(),
                (x.getEstado() == null ? EstadoIntercambio.PENDIENTE : x.getEstado()).name(),
                ahora);

        Long nuevoId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        x.setId(nuevoId);
        x.setFechaPropuesta(ahora);
        if (x.getEstado() == null) x.setEstado(EstadoIntercambio.PENDIENTE);
        log.debug("Intercambio creado con id {}", nuevoId);
    }

    /** Cambia el estado. Si el nuevo estado es final, deja fecha_respuesta. */
    public int cambiarEstado(long id, EstadoIntercambio estado) {
        if (estado == EstadoIntercambio.PENDIENTE) {
            return jdbc.update("UPDATE intercambio SET estado = ? WHERE id = ?",
                    estado.name(), id);
        }
        return jdbc.update(
                "UPDATE intercambio SET estado = ?, fecha_respuesta = ? WHERE id = ?",
                estado.name(), LocalDateTime.now(), id);
    }

    /** Rechaza automaticamente las otras propuestas pendientes que toquen el mismo item. */
    public int rechazarPendientesDe(long itemId, long exceptoId) {
        return jdbc.update(
                "UPDATE intercambio SET estado = 'RECHAZADO', fecha_respuesta = ? "
              + "WHERE estado = 'PENDIENTE' AND id <> ? "
              + "AND (item_ofrecido_id = ? OR item_solicitado_id = ?)",
                LocalDateTime.now(), exceptoId, itemId, itemId);
    }

    /** Ids de items ya comprometidos (aceptados o completados): no se pueden volver a ofrecer. */
    public Set<Long> idsOcupados() {
        Set<Long> ids = new HashSet<>();
        jdbc.query("SELECT item_ofrecido_id, item_solicitado_id FROM intercambio "
                 + "WHERE estado IN ('ACEPTADO','COMPLETADO')",
                rs -> {
                    ids.add(rs.getLong("item_ofrecido_id"));
                    ids.add(rs.getLong("item_solicitado_id"));
                });
        return ids;
    }

    public boolean existePendiente(long itemOfrecidoId, long itemSolicitadoId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM intercambio WHERE estado = 'PENDIENTE' "
              + "AND item_ofrecido_id = ? AND item_solicitado_id = ?",
                Long.class, itemOfrecidoId, itemSolicitadoId);
        return n != null && n > 0;
    }

    public boolean itemOcupado(long itemId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM intercambio "
              + "WHERE estado IN ('ACEPTADO','COMPLETADO') "
              + "AND (item_ofrecido_id = ? OR item_solicitado_id = ?)",
                Long.class, itemId, itemId);
        return n != null && n > 0;
    }

    public void eliminarPorItemId(long itemId) {
        jdbc.update("DELETE FROM intercambio WHERE item_ofrecido_id = ? OR item_solicitado_id = ?",
                itemId, itemId);
    }
}
