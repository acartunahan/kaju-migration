package db.migration.goc;

import java.nio.charset.StandardCharsets;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Spring Batch metadata tablolarını (BATCH_*) Flyway ile kurar.
 *
 * <p>Script elle kopyalanmaz. Classpath'teki spring-batch-core jar'ının içindeki Oracle
 * script'i çalıştırılır. Böylece tablolar, kullanılan Spring Batch sürümüyle her zaman
 * uyumlu olur. Boot'un kendi şema başlatması kapalıdır ({@code spring.batch.jdbc.initialize-schema=never}).
 *
 * <p>Script yolu Spring Batch 5'te bu idi. Spring Batch 6 için derleme/ilk çalıştırmada
 * doğrulanmalı (Aşama 4 notları, 6. bölüm).
 */
public class V1001__spring_batch_semasi extends BaseJavaMigration {

    private static final String BATCH_ORACLE_SEMASI = "org/springframework/batch/core/schema-oracle.sql";

    @Override
    public void migrate(Context context) throws Exception {
        ClassPathResource script = new ClassPathResource(BATCH_ORACLE_SEMASI);
        if (!script.exists()) {
            throw new IllegalStateException("Spring Batch Oracle şema script'i bulunamadı: " + BATCH_ORACLE_SEMASI);
        }
        ScriptUtils.executeSqlScript(context.getConnection(), new EncodedResource(script, StandardCharsets.UTF_8));
    }
}
