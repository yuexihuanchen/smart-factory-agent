package com.smartfactory.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler advice = new GlobalExceptionHandler();

    @Test
    void accessDeniedReturns403WithUnifiedBody() throws Exception {

        standaloneSetup(new DeniedController())
                .setControllerAdvice(advice)
                .build()
                .perform(get("/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("无权访问"));
    }

    @RestController
    static class DeniedController {

        @GetMapping("/denied")
        public void denied() {
            throw new AccessDeniedException("denied");
        }
    }
}
