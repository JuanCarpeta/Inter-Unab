package com.silvia.beautycare.controller;

import com.silvia.beautycare.model.Foto;
import com.silvia.beautycare.model.Item;
import com.silvia.beautycare.model.Categoria;
import com.silvia.beautycare.model.UsuarioSesion;
import com.silvia.beautycare.repository.FotoRepository;
import com.silvia.beautycare.repository.IntercambioRepository;
import com.silvia.beautycare.repository.ItemRepository;
import com.silvia.beautycare.util.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recibe las peticiones del navegador y decide que pagina HTML mostrar.
 *
 *   GET  /items               -> lista
 *   GET  /items/nuevo         -> formulario vacio
 *   GET  /items/{id}/editar   -> formulario con los datos
 *   POST /items/guardar       -> crea o actualiza (con fotos)
 *   POST /items/{id}/eliminar -> borra
 *   POST /items/{id}/foto/eliminar -> elimina una foto
 *   POST /items/{id}/foto/principal -> marca foto como principal
 *
 * Lombok: @RequiredArgsConstructor crea el constructor con el repositorio
 * y @Slf4j crea el "log" para dejar rastro en la consola.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository repo;
    private final FotoRepository fotoRepository;
    private final IntercambioRepository intercambioRepo;

    @Value("${app.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    /**
     * Las fotos se reciben como archivos multipart aparte (parametro "archivos").
     * Evitamos que Spring intente bindearlas contra la propiedad "fotos" del Item,
     * lo que produciria un error de conversion y el formulario no se guardaria.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("fotos");
    }

    // Disponible en todas las vistas de este controlador (lista del <select>)
    @ModelAttribute("categorias")
    public List<String> categorias() {
        return Arrays.stream(Categoria.values())
                .map(Enum::name)
                .toList();
    }

    // Disponible en todas las vistas de este controlador (lista del <select> para estado)
    @ModelAttribute("estados")
    public List<String> estados() {
        return Arrays.stream(com.silvia.beautycare.model.Estado.values())
                .map(Enum::name)
                .toList();
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/items";
    }

    // READ (lista)
    @GetMapping("/items")
    public String listar(Model model) {
        List<Item> items = repo.listar();
        var ocupados = intercambioRepo.idsOcupados();
        items.forEach(i -> i.setDisponible(!ocupados.contains(i.getId())));
        model.addAttribute("items", items);
        return "items/lista";
    }

    // CREATE (mostrar formulario)
    @GetMapping("/items/nuevo")
    public String nuevo(Model model, HttpSession sesion, RedirectAttributes flash) {
        Optional<UsuarioSesion> usuario = SesionUsuario.actual(sesion);
        if (usuario.isEmpty()) {
            flash.addFlashAttribute("error", "Inicia sesión para publicar un item.");
            return "redirect:/login";
        }

        Item item = new Item();
        item.setEstado(com.silvia.beautycare.model.Estado.NUEVO);
        item.setFechaPublicacion(LocalDateTime.now());
        model.addAttribute("item", item);
        model.addAttribute("titulo", "Nuevo item para intercambio");
        return "items/form";
    }

    /**
     * Los items publicados no se pueden editar: si algo esta mal, se eliminan y se
     * vuelven a publicar. Unica excepcion: reclamar un item antiguo sin dueño.
     */
    @PostMapping("/items/{id}/reclamar")
    public String reclamar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<UsuarioSesion> usuario = SesionUsuario.actual(sesion);
        if (usuario.isEmpty()) {
            return "redirect:/login";
        }

        Optional<Item> itemOpt = repo.buscarPorId(id);
        if (itemOpt.isEmpty()) {
            flash.addFlashAttribute("error", "El item ya no existe.");
            return "redirect:/items";
        }

        Item item = itemOpt.get();
        if (item.getPublicadoPor() != null && !item.getPublicadoPor().isBlank()) {
            flash.addFlashAttribute("error", "Ese item ya tiene dueño.");
            return "redirect:/items";
        }

        repo.asignarDueno(id, usuario.get().getEmail());
        log.info("Item {} reclamado por {}", id, usuario.get().getEmail());
        flash.addFlashAttribute("mensaje", "El item quedó a tu nombre.");
        return "redirect:/items";
    }

    // CREATE / UPDATE (guardar con fotos)
    @PostMapping("/items/guardar")
    public String guardar(@Valid @ModelAttribute("item") Item item,
                          BindingResult errores,
                          @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
                          Model model, RedirectAttributes flash,
                          HttpSession sesion) throws IOException {

        Optional<UsuarioSesion> usuarioOpt = SesionUsuario.actual(sesion);
        if (usuarioOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Inicia sesión para publicar un item.");
            return "redirect:/login";
        }
        String miCorreo = usuarioOpt.get().getEmail();

        // Los items publicados son inmutables: este endpoint solo crea.
        if (item.getId() != null) {
            flash.addFlashAttribute("error",
                    "Los items publicados no se pueden editar. Si algo está mal, elimínalo y publícalo de nuevo.");
            return "redirect:/items";
        }

        if (errores.hasErrors()) {
            model.addAttribute("titulo", "Nuevo item");
            return "items/form";
        }

        item.setPublicadoPor(miCorreo);
        repo.crear(item);
        log.info("Item creado: {} (dueño {})", item.getNombre(), miCorreo);
        flash.addFlashAttribute("mensaje", "Item creado.");

        // Procesar fotos subidas
        if (archivos != null && !archivos.isEmpty()) {
            try {
                int guardadas = procesarFotos(item.getId(), archivos);
                if (guardadas > 0) {
                    log.info("{} foto(s) guardada(s) para el item {}", guardadas, item.getId());
                }
            } catch (IOException e) {
                log.error("Error guardando fotos del item {}", item.getId(), e);
                flash.addFlashAttribute("error", "El item se guardo, pero hubo un problema al subir las fotos.");
            }
        }

        return "redirect:/items";
    }

    /**
     * Guarda en disco y en la BD las fotos recibidas.
     * @return cantidad de fotos realmente guardadas
     */
    private int procesarFotos(Long itemId, List<MultipartFile> archivos) throws IOException {
        if (itemId == null) {
            log.warn("No se procesan fotos: el item no tiene id");
            return 0;
        }

        Path uploadPath = uploadPath();
        Files.createDirectories(uploadPath);

        int guardadas = 0;
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) continue;

            // Solo imagenes
            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Archivo ignorado (no es imagen): {} ({})",
                        archivo.getOriginalFilename(), contentType);
                continue;
            }

            String nombreOriginal = StringUtils.cleanPath(
                    archivo.getOriginalFilename() == null ? "foto" : archivo.getOriginalFilename());
            String extension = "";
            int idx = nombreOriginal.lastIndexOf('.');
            if (idx > 0) {
                extension = nombreOriginal.substring(idx).toLowerCase();
            }
            String nombreUnico = UUID.randomUUID() + extension;

            Path rutaDestino = uploadPath.resolve(nombreUnico).normalize();
            // Defensa contra path traversal
            if (!rutaDestino.startsWith(uploadPath)) {
                log.warn("Ruta de destino no permitida: {}", rutaDestino);
                continue;
            }

            Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

            Foto foto = Foto.builder()
                    .itemId(itemId)
                    .nombreArchivo(nombreOriginal)
                    .rutaArchivo(nombreUnico)
                    .esPrincipal(false)
                    .fechaSubida(LocalDateTime.now())
                    .build();

            fotoRepository.guardar(foto);
            guardadas++;
            log.info("Foto guardada para item {}: {} -> {}", itemId, nombreOriginal, nombreUnico);
        }
        return guardadas;
    }

    private Path uploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Un item sin dueño registrado (datos antiguos) se considera editable por cualquiera. */
    private boolean esPropietario(Item item, String correo) {
        if (item.getPublicadoPor() == null || item.getPublicadoPor().isBlank()) return true;
        return item.getPublicadoPor().equalsIgnoreCase(correo);
    }

    /** Borra del disco el archivo de una foto (si existe). */
    private void borrarArchivo(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isBlank()) return;
        try {
            Path base = uploadPath();
            Path archivo = base.resolve(rutaArchivo).normalize();
            if (archivo.startsWith(base)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo de la foto: {}", rutaArchivo, e);
        }
    }

    // DELETE item
    @PostMapping("/items/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<UsuarioSesion> usuario = SesionUsuario.actual(sesion);
        if (usuario.isEmpty()) {
            flash.addFlashAttribute("error", "Inicia sesión para eliminar un item.");
            return "redirect:/login";
        }

        Optional<Item> itemOpt = repo.buscarPorId(id);
        if (itemOpt.isEmpty()) {
            flash.addFlashAttribute("error", "El item ya no existe.");
            return "redirect:/items";
        }
        if (!esPropietario(itemOpt.get(), usuario.get().getEmail())) {
            flash.addFlashAttribute("error", "Ese item es de otro estudiante: no puedes eliminarlo.");
            return "redirect:/items";
        }

        // Borrar primero los archivos de las fotos del disco
        itemOpt.get().getFotos().forEach(f -> borrarArchivo(f.getRutaArchivo()));

        // Borrar propuestas de intercambio que lo referencian
        intercambioRepo.eliminarPorItemId(id);

        if (repo.eliminar(id) > 0) {
            log.info("Item eliminado: id={}", id);
            flash.addFlashAttribute("mensaje", "Item eliminado.");
        } else {
            flash.addFlashAttribute("error", "El item ya no existe.");
        }
        return "redirect:/items";
    }
}
