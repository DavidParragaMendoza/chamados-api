package com.davidparragamendoza.chamados.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retorna404CuandoElRecursoNoExiste() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.caminho").value("/test/not-found"))
                .andExpect(jsonPath("$.message").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    void retorna409CuandoHayConflictoDeNegocio() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value("Conflicto de negocio"))
                .andExpect(jsonPath("$.caminho").value("/test/conflict"))
                .andExpect(jsonPath("$.message").value("Conflicto de negocio"))
                .andExpect(jsonPath("$.path").value("/test/conflict"));
    }

    @Test
    void retorna409CuandoHayViolacionDeIntegridad() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value(
                        "No fue posible completar la operación debido a una violación de integridad de datos."))
                .andExpect(jsonPath("$.caminho").value("/test/data-integrity"))
                .andExpect(jsonPath("$.message").value(
                        "No fue posible completar la operación debido a una violación de integridad de datos."))
                .andExpect(jsonPath("$.path").value("/test/data-integrity"));
    }

    @Test
    void retorna409CuandoFallaUnaReglaDeNegocio() throws Exception {
        mockMvc.perform(get("/test/business-validation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value("Regla de negocio incumplida"))
                .andExpect(jsonPath("$.caminho").value("/test/business-validation"))
                .andExpect(jsonPath("$.message").value("Regla de negocio incumplida"))
                .andExpect(jsonPath("$.path").value("/test/business-validation"));
    }

    @Test
    void retorna400ConCamposInvalidosCuandoFallaLaValidacion() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensagem").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.caminho").value("/test/validation"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"));
    }

    @Test
    void retorna400CuandoJsonEstaMalformado() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed_json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensagem").value("Formato JSON inválido en la petición."))
                .andExpect(jsonPath("$.caminho").value("/test/validation"));
    }

    @Test
    void permiteLaPeticionCuandoLosDatosSonValidos() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"João Silva\"}"))
                .andExpect(status().isOk());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Recurso no encontrado");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new BusinessConflictException("Conflicto de negocio");
        }

        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("Violación de integridad");
        }

        @GetMapping("/business-validation")
        void businessValidation() {
            throw new BusinessValidationException("Regla de negocio incumplida");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        record TestRequest(@NotBlank(message = "El nombre es obligatorio") String nome) {
        }
    }
}
