-- =====================================================================================
-- Flyway callback (beforeMigrate): şema script'leri çalışmadan önce hedef kullanıcının yetkilerini kontrol eder.
--
-- Oracle'da DDL geri alınamaz: V1 yarıda kalırsa (ör. CREATE TRIGGER yetkisi yoksa 676. satırdaki ilk
-- trigger'da ORA-01031) hedefte yarım tablolar ve başarısız bir Flyway kaydı kalır, sonraki her çalışma
-- "Validate failed" ile durur. Bu kontrol, eksik yetkide hiçbir nesne oluşturulmadan açık bir mesajla durdurur.
--
-- Gereken yetkiler V1/V2/V1000/V1001'deki nesne türlerinden: tablo (index dahil), sequence, trigger
-- (fiziksel silme yasağı) ve varsayılan tablespace'te kota. Her migrate çağrısında çalışır; maliyeti yok.
-- =====================================================================================
DECLARE
    v_eksik VARCHAR2(400);
    v_kota  NUMBER;
BEGIN
    SELECT LISTAGG(gerekli.yetki, ', ') WITHIN GROUP (ORDER BY gerekli.yetki)
      INTO v_eksik
      FROM (SELECT 'CREATE TABLE' AS yetki FROM dual
            UNION ALL SELECT 'CREATE SEQUENCE' FROM dual
            UNION ALL SELECT 'CREATE TRIGGER' FROM dual) gerekli
     WHERE gerekli.yetki NOT IN (SELECT privilege FROM session_privs);

    IF v_eksik IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Hedef kullanıcıda eksik yetki: ' || v_eksik
            || '. Hiçbir nesne oluşturulmadı. DBA: GRANT ' || v_eksik || ' TO ' || USER || ';');
    END IF;

    SELECT COUNT(*)
      INTO v_kota
      FROM dual
     WHERE EXISTS (SELECT 1 FROM session_privs WHERE privilege = 'UNLIMITED TABLESPACE')
        OR EXISTS (SELECT 1
                     FROM user_ts_quotas q
                     JOIN user_users u ON u.default_tablespace = q.tablespace_name
                    WHERE q.max_bytes = -1 OR q.max_bytes > q.bytes);

    IF v_kota = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Hedef kullanıcının varsayılan tablespace''inde kotası yok. '
            || 'Hiçbir nesne oluşturulmadı. DBA: ALTER USER ' || USER || ' QUOTA UNLIMITED ON <tablespace>;');
    END IF;
END;
/
