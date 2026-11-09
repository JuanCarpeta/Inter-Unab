package com.silvia.beautycare.repository;

import com.silvia.beautycare.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioRepository {

    private static final String COLUMNAS = "id, email, nombre, password_hash, fecha_registro";

    private static final RowMapper<Usuario> MAPPER = (rs, fila) -> Usuario.builder()
            .id(rs.getLong("id"))
            .email(rs.getString("email"))
            .nombre(rs.getString("nombre"))
            .passwordHash(rs.getString("password_hash"))
            .fechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class))
            .build();

    private final JdbcTemplate jdbc;

    public Optional<Usuario> buscarPorEmail(String email) {
        List<Usuario> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM usuario WHERE email = ?", MAPPER, email);
        return lista.stream().findFirst();
    }

    public boolean existeEmail(String email) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?", Long.class, email);
        return n != null && n > 0;
    }

    public void crear(Usuario u) {
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO usuario (email, nombre, password_hash, fecha_registro) VALUES (?, ?, ?, ?)",
                u.getEmail(), u.getNombre(), u.getPasswordHash(), ahora);

        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        u.setId(id);
        u.setFechaRegistro(ahora);
    }
}
