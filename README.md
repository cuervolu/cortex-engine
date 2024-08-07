# Motor de Ejecución de Código de Cortex

## Requisitos

Para trabajar con este motor de ejecución de código, necesitas tener instalado lo siguiente:

1. Java 21 (OpenJDK)
2. Docker y Docker Compose
3. IDE de tu preferencia (recomendado: IntelliJ IDEA)
4. Maven

Además, asegúrate de tener las siguientes imágenes de Docker instaladas:

- `python:3.12-slim`
- `eclipse-temurin:21`
- `node:20-alpine3.19`
- `rust:1.80-slim`
- `mcr.microsoft.com/dotnet/sdk:8.0`
- `golang:1.22-bookworm`

Para instalar estas imágenes, puedes usar el comando:

```bash
docker pull <nombre_de_la_imagen>
```

## Configuración del entorno

Se proporciona un archivo `docker-compose.yml` para configurar fácilmente los servicios necesarios.
Este archivo incluye:

- PostgreSQL
- Directus (CMS)
- MailDev (para pruebas de correo)
- RabbitMQ
- Redis

Para iniciar todos los servicios, ejecuta:

```bash
docker-compose up -d
```

Asegúrate de tener un archivo `.env` en el mismo directorio con las variables de entorno necesarias.

## Cómo funciona

1. **Envío de código**:
    - El cliente envía una solicitud POST a `/execute` con el código encriptado en Base64 y los
      parámetros de ejecución.
    - El sistema genera un ID de tarea único y lo devuelve al cliente.

2. **Procesamiento**:
    - El código se coloca en una cola RabbitMQ para su procesamiento.
    - Un worker toma la tarea de la cola y crea un contenedor Docker para el lenguaje especificado.
    - El código se ejecuta dentro del contenedor con límites de recursos establecidos.

3. **Resultados**:
    - Los resultados de la ejecución (salida estándar, errores, uso de recursos) se almacenan en
      Redis.
    - El cliente puede consultar el estado y el resultado usando el endpoint GET con el ID de tarea.

4. **Limpieza**:
    - Un servicio programado limpia los contenedores Docker detenidos periódicamente.

5. **Lenguajes soportados**:
    - Python (3.12)
    - Java (21)
    - JavaScript (Node.js 20)
    - Rust (1.80) - WIP
    - C# (.NET SDK 8.0)
    - Go (1.22)

Para ejecutar el proyecto:

1. Clona el repositorio
2. Configura las variables de entorno necesarias (ver `application-dev.yml` y `.env`)
3. Inicia los servicios con Docker Compose
4. Ejecuta `mvn spring-boot:run` o inicia la aplicación desde tu IDE

Nota: Asegúrate de que todos los servicios en Docker Compose estén en ejecución antes de iniciar la
aplicación Spring Boot.