package br.com.fiap.alpha4.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O JSON precisa respeitar os tipos do contrato, sem conversões silenciosas. */
@Configuration
public class StrictJsonConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder
            .featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS,
                DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .postConfigurer(mapper -> {
                var strings = mapper.coercionConfigFor(LogicalType.Textual);
                strings.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                strings.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                strings.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            });
    }
}
