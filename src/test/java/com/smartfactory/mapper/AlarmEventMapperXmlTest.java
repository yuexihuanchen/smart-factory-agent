package com.smartfactory.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmEventMapperXmlTest {

    private static final String MAPPER_RESOURCE =
            "mapper/AlarmEventMapper.xml";

    @Test
    void mapperXmlRegistersRequiredStatements() throws Exception {

        Configuration configuration = new Configuration();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(MAPPER_RESOURCE)) {

            assertThat(inputStream).isNotNull();

            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    MAPPER_RESOURCE,
                    configuration.getSqlFragments()
            );

            mapperBuilder.parse();
        }

        assertThat(configuration.hasStatement(
                "com.smartfactory.mapper.AlarmEventMapper.insert"
        )).isTrue();

        assertThat(configuration.hasStatement(
                "com.smartfactory.mapper.AlarmEventMapper"
                        + ".findBySourceAndEventId"
        )).isTrue();

        assertThat(configuration.hasStatement(
                "com.smartfactory.mapper.AlarmEventMapper.updateAlarmId"
        )).isTrue();
    }
}
