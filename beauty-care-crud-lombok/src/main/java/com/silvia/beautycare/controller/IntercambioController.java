package com.silvia.beautycare.controller;

import com.silvia.beautycare.model.EstadoIntercambio;
import com.silvia.beautycare.model.Intercambio;
import com.silvia.beautycare.model.Item;
import com.silvia.beautycare.model.Mensaje;
import com.silvia.beautycare.model.UsuarioSesion;
import com.silvia.beautycare.repository.IntercambioRepository;
import com.silvia.beautycare.repository.ItemRepository;
import com.silvia.beautycare.repository.MensajeRepository;
import com.silvia.beautycare.util.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Intercambios ("trueques") entre items. No hay dinero de por medio.
 *
 *   GET  /intercambios                  -> lista de propuestas
 *   GET  /items/{id}/intercambiar       -> formulario para proponer un trueque por ese item
 *   POST /intercambios/guardar          -> crea la propuesta
 *   POST /intercambios/{id}/aceptar     -> acepta (compromete ambos items)
 *   POST /intercambios/{id}/rechazar    -> rechaza
 *   POST /intercambios/{id}/cancelar    -> cancela (libera los items si estaba aceptado)
 *   POST /intercambios/{id}/completar   -> marca como realizado
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class IntercambioController {

    private final IntercambioRepository repo;
    private final ItemRepository itemRepo;
    private final MensajeRepository mensajeRepo;

    // READ: listado de propuestas (con sus mensajes de chat)
    @GetMapping("/intercambios")
    public String listar(Model model) {
        List<Intercambio> lista = repo.listar();
        Map<Long, List<Mensaje>> mensajes = new HashMap<>();
        for (Intercambio x : lista) {
            mensajes.put(x.getId(), mensajeRepo.listarPorIntercambio(x.getId()));
        }
        model.addAttribute("intercambios", lista);
        model.addAttribute("mensajes", mensajes);
        return "intercambios/lista";
    }

    // Chat: enviar un mensaje a una propuesta (solo las dos partes)
    @PostMapping("/intercambios/{id}/mensajes")
    public String enviarMensaje(@PathVariable long id,
                                @RequestParam(required = false) String texto,
                                RedirectAttributes flash,
                                HttpSession sesion) {

        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }
        if (!esParte(opt.get(), sesion)) {
            flash.addFlashAttribute("error", "Solo las partes del intercambio pueden escribir en el chat.");
            return "redirect:/intercambios";
        }

        EstadoIntercambio estado = opt.get().getEstado();
        if (estado == EstadoIntercambio.RECHAZADO
                || estado == EstadoIntercambio.CANCELADO
                || estado == EstadoIntercambio.COMPLETADO) {
            flash.addFlashAttribute("error", "El intercambio está cerrado: el chat es de solo lectura.");
            return "redirect:/intercambios?chat=" + id;
        }

        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            flash.addFlashAttribute("error", "El mensaje no puede estar vacío.");
            return "redirect:/intercambios?chat=" + id;
        }
        if (limpio.length() > 500) {
            limpio = limpio.substring(0, 500);
        }

        Mensaje m = Mensaje.builder()
                .intercambioId(id)
                .autor(correoActual(sesion).orElse("desconocido"))
                .texto(limpio)
                .build();
        mensajeRepo.crear(m);

        return "redirect:/intercambios?chat=" + id;
    }

    // Formulario: elegir cual de MIS items ofrezco por el item indicado
    @GetMapping("/items/{id}/intercambiar")
    public String proponer(@PathVariable long id, Model model, RedirectAttributes flash, HttpSession sesion) {
        Optional<UsuarioSesion> usuarioOpt = SesionUsuario.actual(sesion);
        if (usuarioOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Inicia sesión para proponer un intercambio.");
            return "redirect:/login";
        }
        String miCorreo = usuarioOpt.get().getEmail();

        Optional<Item> objetivoOpt = itemRepo.buscarPorId(id);
        if (objetivoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "El item ya no existe.");
            return "redirect:/items";
        }

        Item objetivo = objetivoOpt.get();

        if (esMio(objetivo, miCorreo)) {
            flash.addFlashAttribute("error", "No puedes intercambiar un item contigo mismo.");
            return "redirect:/items";
        }
        if (repo.itemOcupado(id)) {
            flash.addFlashAttribute("error", "Ese item ya fue intercambiado.");
            return "redirect:/items";
        }

        Set<Long> ocupados = repo.idsOcupados();
        List<Item> ofrecibles = itemRepo.listar().stream()
                .filter(i -> esMio(i, miCorreo))
                .filter(i -> !ocupados.contains(i.getId()))
                .toList();

        if (ofrecibles.isEmpty()) {
            flash.addFlashAttribute("error",
                    "Necesitas publicar al menos un item disponible para ofrecer a cambio.");
            return "redirect:/items";
        }

        model.addAttribute("itemObjetivo", objetivo);
        model.addAttribute("itemsOfrecibles", ofrecibles);
        return "intercambios/form";
    }

    // CREATE: guardar la propuesta
    @PostMapping("/intercambios/guardar")
    public String guardar(@RequestParam long itemOfrecidoId,
                          @RequestParam long itemSolicitadoId,
                          @RequestParam(required = false) String mensaje,
                          RedirectAttributes flash,
                          HttpSession sesion) {

        Optional<UsuarioSesion> usuarioOpt = SesionUsuario.actual(sesion);
        if (usuarioOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Inicia sesión para proponer un intercambio.");
            return "redirect:/login";
        }
        String miCorreo = usuarioOpt.get().getEmail();

        if (itemOfrecidoId == itemSolicitadoId) {
            flash.addFlashAttribute("error", "No puedes intercambiar un item consigo mismo.");
            return "redirect:/items";
        }

        Optional<Item> ofrecidoOpt = itemRepo.buscarPorId(itemOfrecidoId);
        Optional<Item> solicitadoOpt = itemRepo.buscarPorId(itemSolicitadoId);
        if (ofrecidoOpt.isEmpty() || solicitadoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Alguno de los items ya no existe.");
            return "redirect:/items";
        }

        Item ofrecido = ofrecidoOpt.get();
        Item solicitado = solicitadoOpt.get();

        if (!esMio(ofrecido, miCorreo)) {
            flash.addFlashAttribute("error", "Solo puedes ofrecer items que hayas publicado tú.");
            return "redirect:/items";
        }
        if (esMio(solicitado, miCorreo)) {
            flash.addFlashAttribute("error", "No puedes intercambiar un item contigo mismo.");
            return "redirect:/items";
        }
        if (repo.itemOcupado(itemOfrecidoId) || repo.itemOcupado(itemSolicitadoId)) {
            flash.addFlashAttribute("error", "Alguno de los items ya fue intercambiado.");
            return "redirect:/items";
        }
        if (repo.existePendiente(itemOfrecidoId, itemSolicitadoId)) {
            flash.addFlashAttribute("error", "Ya existe una propuesta pendiente con esos mismos items.");
            return "redirect:/intercambios";
        }

        Intercambio x = Intercambio.builder()
                .itemOfrecidoId(itemOfrecidoId)
                .itemSolicitadoId(itemSolicitadoId)
                .mensaje(mensaje)
                .estado(EstadoIntercambio.PENDIENTE)
                .build();

        repo.crear(x);
        log.info("Propuesta creada: id={}, {} ofrece item {} y pide item {}",
                x.getId(), miCorreo, itemOfrecidoId, itemSolicitadoId);
        flash.addFlashAttribute("mensaje", "Propuesta de intercambio enviada.");
        return "redirect:/intercambios";
    }

    // ACEPTAR: solo el dueño del item solicitado
    @PostMapping("/intercambios/{id}/aceptar")
    public String aceptar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }

        Intercambio x = opt.get();
        if (!puedeResponder(x, sesion)) {
            flash.addFlashAttribute("error",
                    "Solo el dueño del item solicitado puede aceptar esta propuesta.");
            return "redirect:/intercambios";
        }
        if (x.getEstado() != EstadoIntercambio.PENDIENTE) {
            flash.addFlashAttribute("error", "Solo se pueden aceptar propuestas pendientes.");
            return "redirect:/intercambios";
        }
        if (repo.itemOcupado(x.getItemOfrecidoId()) || repo.itemOcupado(x.getItemSolicitadoId())) {
            flash.addFlashAttribute("error", "Alguno de los items ya fue intercambiado en otra propuesta.");
            return "redirect:/intercambios";
        }

        repo.cambiarEstado(id, EstadoIntercambio.ACEPTADO);
        repo.rechazarPendientesDe(x.getItemOfrecidoId(), id);
        repo.rechazarPendientesDe(x.getItemSolicitadoId(), id);

        log.info("Intercambio aceptado: id={}", id);
        flash.addFlashAttribute("mensaje", "Intercambio aceptado. Coordinen la entrega.");
        return "redirect:/intercambios";
    }

    @PostMapping("/intercambios/{id}/rechazar")
    public String rechazar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }
        if (!puedeResponder(opt.get(), sesion)) {
            flash.addFlashAttribute("error",
                    "Solo el dueño del item solicitado puede rechazar esta propuesta.");
            return "redirect:/intercambios";
        }
        return cambiar(id, EstadoIntercambio.RECHAZADO, "Propuesta rechazada.", flash);
    }

    @PostMapping("/intercambios/{id}/cancelar")
    public String cancelar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }
        if (!esParte(opt.get(), sesion)) {
            flash.addFlashAttribute("error", "Solo las partes del intercambio pueden cancelarlo.");
            return "redirect:/intercambios";
        }
        return cambiar(id, EstadoIntercambio.CANCELADO, "Intercambio cancelado.", flash);
    }

    @PostMapping("/intercambios/{id}/completar")
    public String completar(@PathVariable long id, RedirectAttributes flash, HttpSession sesion) {
        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }
        if (!esParte(opt.get(), sesion)) {
            flash.addFlashAttribute("error", "Solo las partes del intercambio pueden completarlo.");
            return "redirect:/intercambios";
        }
        return cambiar(id, EstadoIntercambio.COMPLETADO, "Intercambio completado. ¡Gracias!", flash);
    }

    private String cambiar(long id, EstadoIntercambio nuevo, String ok, RedirectAttributes flash) {
        Optional<Intercambio> opt = repo.buscarPorId(id);
        if (opt.isEmpty()) {
            flash.addFlashAttribute("error", "La propuesta ya no existe.");
            return "redirect:/intercambios";
        }

        EstadoIntercambio actual = opt.get().getEstado();
        if (actual == nuevo) {
            flash.addFlashAttribute("error", "La propuesta ya esta en ese estado.");
            return "redirect:/intercambios";
        }
        if (actual == EstadoIntercambio.COMPLETADO || actual == EstadoIntercambio.RECHAZADO) {
            flash.addFlashAttribute("error", "Esa propuesta ya esta cerrada.");
            return "redirect:/intercambios";
        }
        if (nuevo == EstadoIntercambio.COMPLETADO && actual != EstadoIntercambio.ACEPTADO) {
            flash.addFlashAttribute("error", "Primero hay que aceptar el intercambio.");
            return "redirect:/intercambios";
        }

        repo.cambiarEstado(id, nuevo);
        log.info("Intercambio {} -> {}", id, nuevo);
        flash.addFlashAttribute("mensaje", ok);
        return "redirect:/intercambios";
    }

    // ---------- reglas de propiedad ----------

    private Optional<String> correoActual(HttpSession sesion) {
        return SesionUsuario.actual(sesion).map(UsuarioSesion::getEmail);
    }

    private boolean esMio(Item item, String correo) {
        return item.getPublicadoPor() != null
                && item.getPublicadoPor().equalsIgnoreCase(correo);
    }

    /**
     * Quien puede aceptar/rechazar: el dueño del item solicitado (quien recibe la propuesta).
     * Si el item es antiguo y no tiene dueño registrado, se permite a cualquier usuario identificado.
     */
    private boolean puedeResponder(Intercambio x, HttpSession sesion) {
        Optional<String> correo = correoActual(sesion);
        if (correo.isEmpty()) return false;
        String dueno = x.getDuenoSolicitado();
        if (dueno == null || dueno.isBlank()) return true;
        return dueno.equalsIgnoreCase(correo.get());
    }

    /** Cancelar o completar: cualquiera de las dos partes. */
    private boolean esParte(Intercambio x, HttpSession sesion) {
        Optional<String> correo = correoActual(sesion);
        if (correo.isEmpty()) return false;
        String c = correo.get();
        boolean duenoOfrecido = x.getDuenoOfrecido() != null && x.getDuenoOfrecido().equalsIgnoreCase(c);
        boolean duenoSolicitado = x.getDuenoSolicitado() != null && x.getDuenoSolicitado().equalsIgnoreCase(c);
        return duenoOfrecido || duenoSolicitado;
    }
}
