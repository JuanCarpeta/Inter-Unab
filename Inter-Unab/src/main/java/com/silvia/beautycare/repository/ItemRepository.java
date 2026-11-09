package com.silvia.beautycare.repository;

import com.silvia.beautycare.model.Categoria;
import com.silvia.beautycare.model.Estado;
import com.silvia.beautycare.model.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Las 4 operaciones del CRUD contra SQLite, escritas con SQL puro:
 * Create (INSERT), Read (SELECT), Update (UPDATE) y Delete (DELETE).
 *
 * @RequiredArgsConstructor (Lombok) genera el constructor que recibe el JdbcTemplate,
 * asi Spring lo inyecta sin que escribamos el constructor.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ItemRepository {

    private static final String COLUMNAS = "id, nombre, descripcion, categoria, estado, fecha_publicacion, publicado_por";

    // Convierte cada fila del resultado SQL en un objeto Item (usando el builder de Lombok)
    private static final RowMapper<Item> MAPPER = (rs, fila) -> Item.builder()
            .id(rs.getLong("id"))
            .nombre(rs.getString("nombre"))
            .descripcion(rs.getString("descripcion"))
            .categoria(Categoria.valueOf(rs.getString("categoria")))
            .estado(Estado.valueOf(rs.getString("estado")))
            .fechaPublicacion(rs.getObject("fecha_publicacion", LocalDateTime.class))
            .publicadoPor(rs.getString("publicado_por"))
            .build();

    private final JdbcTemplate jdbc;
    private final FotoRepository fotoRepository;

    // READ: todos los items (ordenados por fecha de publicación descendente)
    public List<Item> listar() {
        List<Item> items = jdbc.query("SELECT " + COLUMNAS + " FROM item ORDER BY fecha_publicacion DESC", MAPPER);
        cargarFotos(items);
        return items;
    }

    // READ: un item por id
    public Optional<Item> buscarPorId(long id) {
        List<Item> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM item WHERE id = ?", MAPPER, id);
        Optional<Item> itemOpt = lista.stream().findFirst();
        itemOpt.ifPresent(this::cargarFotos);
        return itemOpt;
    }

    private void cargarFotos(List<Item> items) {
        for (Item item : items) {
            item.setFotos(fotoRepository.buscarPorItemId(item.getId()));
        }
    }

    private void cargarFotos(Item item) {
        item.setFotos(fotoRepository.buscarPorItemId(item.getId()));
    }

    // CREATE: inserta y deja el id generado en el objeto (necesario para asociar las fotos)
    public void crear(Item item) {
        LocalDateTime fecha = item.getFechaPublicacion() != null
                ? item.getFechaPublicacion()
                : LocalDateTime.now();

        jdbc.update(
                "INSERT INTO item (nombre, descripcion, categoria, estado, fecha_publicacion, publicado_por) "
              + "VALUES (?, ?, ?, ?, ?, ?)",
                item.getNombre(), item.getDescripcion(),
                item.getCategoria().name(),
                item.getEstado().name(),
                fecha,
                item.getPublicadoPor());

        // El pool tiene una sola conexion, asi que last_insert_rowid() es seguro aqui
        Long nuevoId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        item.setId(nuevoId);
        item.setFechaPublicacion(fecha);
        log.debug("Item creado con id {}", nuevoId);
    }

    // UPDATE: devuelve cuantas filas cambio (0 si el id ya no existe)
    public int actualizar(Item item) {
        return jdbc.update(
                "UPDATE item SET nombre = ?, descripcion = ?, categoria = ?, estado = ?, publicado_por = ? WHERE id = ?",
                item.getNombre(), item.getDescripcion(),
                item.getCategoria().name(),
                item.getEstado().name(),
                item.getPublicadoPor(),
                item.getId());
    }

    /** Asigna dueño a un item que no lo tiene (items antiguos). */
    public int asignarDueno(long id, String correo) {
        return jdbc.update(
                "UPDATE item SET publicado_por = ? "
              + "WHERE id = ? AND (publicado_por IS NULL OR publicado_por = '')",
                correo, id);
    }

    // DELETE: devuelve cuantas filas borro
    public int eliminar(long id) {
        // Eliminar fotos asociadas (CASCADE en BD también lo hace, pero por si acaso)
        fotoRepository.eliminarPorItemId(id);
        return jdbc.update("DELETE FROM item WHERE id = ?", id);
    }

    public long contar() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM item", Long.class);
        return total == null ? 0 : total;
    }
}