# ==============================================================
# Etapa 1: Build (Maven + JDK 21)
# ==============================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copiar descriptor de Maven y código fuente
COPY pom.xml .
COPY src ./src

# Compilar el código y generar el fat JAR sin ejecutar tests unitarios
RUN mvn clean package -DskipTests -B

# ==============================================================
# Etapa 2: Ejecución (JRE 21 liviano)
# ==============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios de root
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copiar únicamente el archivo JAR generado desde la etapa de compilación
COPY --from=builder /build/target/*.jar app.jar

# Exponer el puerto por defecto de la API
EXPOSE 8080

# Comando de ejecución con soporte para límites de memoria en contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
