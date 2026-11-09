# Inter-Unab

Plataforma web de **intercambio de objetos entre estudiantes de la UNAB**. La idea es sencilla:
publicas lo que ya no usas y otro estudiante te ofrece algo que sí necesitas. **No hay dinero
de por medio**: es trueque puro, dentro de la comunidad universitaria.

**Demo en vivo:** <http://54.83.67.15:8080> (solo cuentas `@unab.edu.co`)

**Stack:** Java 17 · Spring Boot 3.5 · Thymeleaf · Bootstrap 5 · SQLite (JdbcTemplate) · Lombok · BCrypt

---

## Funcionalidades

| Módulo | Qué hace |
|---|---|
| **Cuentas** | Registro y login solo con correo `@unab.edu.co`. La contraseña se guarda cifrada con BCrypt, nunca en texto plano. |
| **Ítems** | Publicar un objeto con nombre, descripción, categoría, estado y varias fotos. Los ítems publicados quedan inmutables. |
| **Catálogo** | Ver todo lo publicado por otros estudiantes y reclamar un ítem disponible. |
| **Intercambios** | Proponer un trueque eligiendo cuál de mis ítems ofrezco. La propuesta se puede aceptar, rechazar, cancelar o marcar como completada. |
| **Chat** | Cada propuesta tiene su hilo de mensajes para acordar la entrega. |

**Categorías:** `Libros`, `Tecnología`, `Ropa`
**Estados del ítem:** `Nuevo`, `Usado excelente`, `Usado bueno`, `Usado regular`
**Estados del intercambio:** `PENDIENTE` → `ACEPTADO` → `COMPLETADO` (o `RECHAZADO` / `CANCELADO`)

---

## Cómo ejecutarlo

Requisitos: **JDK 17 o 21** y **Maven**.

```bash
mvn spring-boot:run
```

Luego abre <http://localhost:8080>.

> ⚠️ **Lombok no compila con Java 26** (`EndPosTable`). Asegúrate de que `JAVA_HOME` apunte a
> un JDK 17 o 21. En Windows, el JDK 21 de Android Studio sirve:
> `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
> En IntelliJ/NetBeans hay que activar el *annotation processing* de Lombok, o el editor
> mostrará errores en rojo aunque Maven compile bien.

Jar ejecutable:

```bash
mvn clean package
java -jar target/inter-unab.jar
```

Cambiar el puerto: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.

---

## Rutas principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Redirige al catálogo o al login |
| GET/POST | `/login`, `/registro` | Autenticación con correo institucional |
| GET | `/logout` | Cierra la sesión |
| GET | `/items` | Catálogo de ítems |
| GET | `/items/nuevo` | Formulario para publicar un ítem |
| POST | `/items/guardar` | Guarda el ítem y sus fotos |
| POST | `/items/{id}/reclamar` | Marca un ítem como reclamado |
| POST | `/items/{id}/eliminar` | Elimina un ítem propio |
| GET | `/items/{id}/intercambiar` | Formulario para proponer un trueque |
| GET | `/intercambios` | Mis propuestas (enviadas y recibidas) |
| POST | `/intercambios/guardar` | Crea la propuesta |
| POST | `/intercambios/{id}/aceptar` · `rechazar` · `cancelar` · `completar` | Cambios de estado |
| POST | `/intercambios/{id}/mensajes` | Enviar mensaje en el chat |

Todas las rutas salvo login y registro pasan por `LoginInterceptor`, así que sin sesión
redirigen a `/login`.

---

## Base de datos

SQLite: un solo archivo, sin servidor que instalar. El esquema está en
`src/main/resources/schema.sql` y se aplica en cada arranque con `CREATE TABLE IF NOT EXISTS`.

```
item                  ítems publicados (nombre, descripción, categoría, estado, dueño)
item_foto             fotos de cada ítem (una marcada como principal)
usuario               cuentas UNAB (email único, nombre, password_hash)
intercambio           propuesta entre dos ítems (item_ofrecido_id, item_solicitado_id, estado)
intercambio_mensaje   mensajes del chat de cada propuesta
```

`intercambio` tiene un `CHECK` que impide proponer un intercambio de un ítem consigo mismo, y
las claves foráneas de fotos y mensajes usan `ON DELETE CASCADE`.

---

## Configuración (`application.properties`)

| Propiedad | Valor por defecto | Para qué |
|---|---|---|
| `server.port` | `8080` | Puerto HTTP |
| `spring.datasource.url` | `jdbc:sqlite:${INTER_UNAB_DB_PATH:./inter-unab.db}` | Ruta del archivo de base de datos (se puede sobreescribir con la variable de entorno `INTER_UNAB_DB_PATH`) |
| `spring.datasource.hikari.maximum-pool-size` | `1` | Una sola conexión: SQLite admite una escritura a la vez |
| `spring.servlet.multipart.max-file-size` | `5MB` | Tamaño máximo de cada foto |
| `spring.servlet.multipart.max-request-size` | `20MB` | Tamaño máximo total del formulario |
| `app.upload.dir` | `${user.dir}/uploads` | Carpeta donde se guardan las fotos subidas |
| `spring.thymeleaf.cache` | `false` | Permite ver cambios en plantillas al recargar (modo clase) |

---

## Estructura

```
src/main/java/com/silvia/beautycare/
├── InterUnabApplication.java      arranque de Spring Boot
├── controller/
│   ├── AuthController.java        login, registro, logout
│   ├── ItemController.java        catálogo, publicación, fotos, reclamar
│   └── IntercambioController.java propuestas, estados y chat
├── service/AuthService.java       reglas de correo @unab.edu.co y BCrypt
├── repository/                    ItemRepository, FotoRepository, UsuarioRepository,
│                                  IntercambioRepository, MensajeRepository (JdbcTemplate)
├── model/                         Item, Foto, Intercambio, Mensaje, Usuario, UsuarioSesion
│                                  y los enums Categoria, Estado, EstadoIntercambio
├── config/                        EsquemaMigracion, DatosIniciales, LoginInterceptor,
│                                  WebConfig, GlobalModelAttributes
└── util/SesionUsuario.java        usuario autenticado en la sesión HTTP

src/main/resources/
├── application.properties
├── schema.sql
├── templates/  auth/login.html, auth/registro.html, items/lista.html, items/form.html,
│               intercambios/lista.html, intercambios/form.html, fragments/layout.html
└── static/     css/estilos.css, img/unab.png
```

---

## Despliegue

La app corre como servicio `systemd` en una instancia EC2 `t3.micro` (AWS, `us-east-1`):

| Elemento | Ruta |
|---|---|
| Jar | `/opt/beauty-care-crud/inter-unab.jar` |
| Base de datos | `/var/lib/beautycare-crud/inter-unab.db` |
| Fotos | `/var/lib/beautycare-crud/uploads` |
| Servicio | `beauty-care-crud.service` |

Para actualizar: se copia el jar nuevo, se reinicia el servicio y listo — la base de datos y
las fotos están fuera del jar, así que se conservan.

```bash
scp -i llave.pem target/inter-unab.jar ec2-user@<ip>:/tmp/
ssh -i llave.pem ec2-user@<ip> "sudo mv /tmp/inter-unab.jar /opt/beauty-care-crud/ && sudo systemctl restart beauty-care-crud"
```

---

## Notas

- Bootstrap 5 se carga por CDN: el navegador necesita internet.
- Es un proyecto académico: no está pensado para producción abierta a internet.
- La demo usa HTTP sin certificado.
