/*
 * Copyright 2024 Jesper Udby
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.bankopladerne.online.server.test.filecache.api;

import eu.bankopladerne.online.server.test.filecache.configuration.FileCacheConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import({
        FileCacheConfiguration.class,
        NumbersImageController.class,
        Responses.class
})
@WebMvcTest(NumbersImageController.class)
class NumbersImageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPng_sunshine_succeeds() throws Exception {
        mockMvc.perform(get(NumbersImageController.BASE_PATH + "/100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Configuration
    static class NumbersImageControllerTestConfiguration {
        @Bean
        public FileCacheConfiguration.FileCacheConfig fileCacheConfig() {
            return new FileCacheConfiguration.FileCacheConfig(100, 10, 100);
        }
    }
}
