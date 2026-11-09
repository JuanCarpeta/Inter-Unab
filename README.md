Inter-Unab

Plataforma web de trueque entre estudiantes de la UNAB. Publicas lo que ya no usas y otro estudiante te ofrece algo que sí necesitas. No hay dinero de por medio: es intercambio puro dentro de la comunidad universitaria.

Proyecto académico desarrollado para la UNAB.

Demo en vivo: http://54.83.67.15:8080 (solo cuentas @unab.edu.co)

Tecnologías

Java 17 · Spring Boot 3.5 · Thymeleaf · Bootstrap 5 · SQLite (JdbcTemplate) · Lombok · BCrypt · Maven

Funcionalidades
Módulo	Qué hace
Cuentas	Registro y login solo con correo @unab.edu.co. La contraseña se guarda cifrada con BCrypt.
Ítems	Publicar un objeto con nombre, descripción, categoría, estado y varias fotos.
Catálogo	Ver lo publicado por otros estudiantes y reclamar un ítem disponible.
Intercambios	Proponer un trueque eligiendo cuál de mis ítems ofrezco. La propuesta se puede aceptar, rechazar, cancelar o completar.
Chat	Cada propuesta tiene su hilo de mensajes para acordar la entrega.
Categorías: Libros, Tecnología, Ropa
Estados del ítem: Nuevo, Usado excelente, Usado bueno, Usado regular
Estados del intercambio: PENDIENTE → ACEPTADO → COMPLETADO (o RECHAZADO / CANCELADO)
Estructura del repositorio
Inter-Unab/                  <- raíz del repositorio
├── README.md                este archivo
└── Inter-Unab/              proyecto Maven (aquí se ejecutan los comandos)
    ├── pom.xml
    ├── src/main/java/com/silvia/beautycare/
    │   ├── InterUnabApplication.java      arranque de Spring Boot
    │   ├── controller/                    Auth, Item e Intercambio
    │   ├── service/AuthService.java       regla @unab.edu.co y BCrypt
    │   ├── repository/                    acceso a datos con JdbcTemplate
    │   ├── model/                         entidades y enums
    │   ├── config/                        interceptor de login, esquema, configuración web
    │   └── util/SesionUsuario.java        usuario autenticado en la sesión
    └── src/main/resources/
        ├── application.properties
        ├── schema.sql                     esquema de la base de datos
        ├── templates/                     auth, items, intercambios, fragments
        └── static/                        css/estilos.css, img/unab.png
Cómo ejecutarlo

Requisitos: JDK 17 o 21 y Maven.

bash
git clone https://github.com/JuanCarpeta/Inter-Unab.git
cd Inter-Unab/Inter-Unab
mvn spring-boot:run

Luego abre http://localhost:8080.

Para generar el jar ejecutable:

bash
mvn clean package
java -jar target/inter-unab.jar

Para cambiar el puerto:

bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

Nota sobre Lombok: no compila con Java 26. Asegúrate de que JAVA_HOME apunte a un JDK 17 o 21. En IntelliJ o NetBeans activa el annotation processing, o el editor mostrará errores en rojo aunque Maven compile bien.

La base de datos (inter-unab.db) y la carpeta uploads/ se crean solas la primera vez.

Rutas principales
Método	Ruta	Descripción
GET	/	Redirige al catálogo o al login
GET/POST	/login, /registro	Autenticación con correo institucional
GET	/logout	Cierra la sesión
GET	/items	Catálogo de ítems
GET	/items/nuevo	Formulario para publicar un ítem
POST	/items/guardar	Guarda el ítem y sus fotos
POST	/items/{id}/reclamar	Marca un ítem como reclamado
POST	/items/{id}/eliminar	Elimina un ítem propio
GET	/items/{id}/intercambiar	Formulario para proponer un trueque
GET	/intercambios	Mis propuestas (enviadas y recibidas)
POST	/intercambios/guardar	Crea la propuesta
POST	/intercambios/{id}/aceptar, rechazar, cancelar, completar	Cambios de estado
POST	/intercambios/{id}/mensajes	Envía un mensaje en el chat

Todas las rutas, salvo login y registro, pasan por LoginInterceptor: sin sesión redirigen a /login.

Base de datos

SQLite en un solo archivo, sin servidor que instalar. El esquema está en src/main/resources/schema.sql y se aplica en cada arranque con CREATE TABLE IF NOT EXISTS.

Tabla	Contenido
usuario	Cuentas UNAB (email único, nombre, hash de contraseña)
item	Ítems publicados (nombre, descripción, categoría, estado, dueño)
item_foto	Fotos de cada ítem, una marcada como principal
intercambio	Propuesta entre dos ítems (ofrecido, solicitado, estado)
intercambio_mensaje	Mensajes del chat de cada propuesta

intercambio tiene un CHECK que impide proponer un ítem contra sí mismo, y las fotos y los mensajes usan ON DELETE CASCADE.

Configuración

Archivo src/main/resources/application.properties:

Propiedad	Valor por defecto	Para qué
server.port	8080	Puerto HTTP
spring.datasource.url	jdbc:sqlite:${INTER_UNAB_DB_PATH:./inter-unab.db}	Ruta de la base de datos (se puede cambiar con la variable de entorno INTER_UNAB_DB_PATH)
spring.datasource.hikari.maximum-pool-size	1	Una sola conexión: SQLite admite una escritura a la vez
spring.servlet.multipart.max-file-size	5MB	Tamaño máximo por foto
spring.servlet.multipart.max-request-size	20MB	Tamaño máximo del formulario
app.upload.dir	${user.dir}/uploads	Carpeta donde se guardan las fotos
Notas
Bootstrap 5 se carga por CDN, así que el navegador necesita internet.
Es un proyecto académico: no está pensado para producción abierta a internet.
La demo usa HTTP, sin certificado.
Autor

Juan Diego · @JuanCarpeta
