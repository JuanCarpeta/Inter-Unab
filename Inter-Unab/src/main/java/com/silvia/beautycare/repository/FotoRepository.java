package com.silvia.beautycare.repository;

import com.silvia.beautycare.model.Foto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar fotos de items.
 */
@Repository
@RequiredArgsConstructor
public class FotoRepository {

    private static final String COLUMNAS = "id, item_id, nombre_archivo, ruta_archivo, es_principal, fecha_subida";

    private static final RowMapper<Foto> MAPPER = (rs, fila) -> Foto.builder()
            .id(rs.getLong("id"))
            .itemId(rs.getLong("item_id"))
            .nombreArchivo(rs.getString("nombre_archivo"))
            .rutaArchivo(rs.getString("ruta_archivo"))
            .esPrincipal(rs.getBoolean("es_principal"))
            .fechaSubida(rs.getObject("fecha_subida", LocalDateTime.class))
            .build();

    private final JdbcTemplate jdbc;

    public List<Foto> buscarPorItemId(Long itemId) {
        return jdbc.query(
                "SELECT " + COLUMNAS + " FROM item_foto WHERE item_id = ? ORDER BY es_principal DESC, fecha_subida",
                MAPPER, itemId);
    }

    public Optional<Foto> buscarPorId(Long id) {
        List<Foto> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM item_foto WHERE id = ?", MAPPER, id);
        return lista.stream().findFirst();
    }

    public void guardar(Foto foto) {
        if (foto.getId() == null) {
            // Si es la primera foto, marcarla como principal
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM item_foto WHERE item_id = ?", Long.class, foto.getItemId());
            boolean esPrincipal = (count == null || count == 0);
            
            jdbc.update(
                    "INSERT INTO item_foto (item_id, nombre_archivo, ruta_archivo, es_principal, fecha_subida) VALUES (?, ?, ?, ?, ?)",
                    foto.getItemId(), foto.getNombreArchivo(), foto.getRutaArchivo(), 
                    esPrincipal, LocalDateTime.now());
        } else {
            jdbc.update(
                    "UPDATE item_foto SET nombre_archivo = ?, ruta_archivo = ?, es_principal = ? WHERE id = ?",
                    foto.getNombreArchivo(), foto.getRutaArchivo(), foto.getEsPrincipal(), foto.getId());
        }
    }

    public void eliminar(Long id) {
        jdbc.update("DELETE FROM item_foto WHERE id = ?", id);
    }

    public void eliminarPorItemId(Long itemId) {
        jdbc.update("DELETE FROM item_foto WHERE item_id = ?", itemId);
    }

    public void establecerPrincipal(Long fotoId, Long itemId) {
        // Quitar principal de las demás
        jdbc.update("UPDATE item_foto SET es_principal = 0 WHERE item_id = ?", itemId);
        // Poner principal a la seleccionada
        jdbc.update("UPDATE item_foto SET es_principal = 1 WHERE id = ?", fotoId);
    }

    public Optional<Foto> buscarPrincipalPorItemId(Long itemId) {
        List<Foto> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM item_foto WHERE item_id = ? AND es_principal = 1", MAPPER, itemId);
        return lista.stream().findFirst();
    }
}