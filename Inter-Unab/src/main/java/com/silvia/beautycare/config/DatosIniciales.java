package com.silvia.beautycare.config;

import com.silvia.beautycare.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración inicial de la aplicación.
 * Actualmente no carga datos de ejemplo para comenzar con base vacía.
 */
@Slf4j
@Configuration
public class DatosIniciales {

    @Bean
    ApplicationRunner inicializar(ItemRepository repo) {
        return args -> {
            // Intencionalmente left blank para comenzar con base de datos vacía
            // Los usuarios cargarán sus propios items para intercambio
            if (repo.contar() > 0) {
                return; // Ya hay datos, no hacemos nada
            }
            // No cargamos ningún dato de ejemplo - base completamente vacía
            log.info("Base de datos inicializada vacía para Inter-Unab");
        };
    }
}
