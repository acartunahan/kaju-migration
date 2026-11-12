-- =====================================================================================
-- V3: Demirbaş durumuna IADE eklenir.
--
-- R_DEMIRBAS.DURUM comment'i yalnızca "1-Normal, 2-Arıza" diyordu. Kapalı ağdaki ilk KURU çalıştırmada
-- 14.226 demirbaşta DURUM = '3' görüldü; eski Delphi kodunda 3 = İade (kullanıcı, 2026-11-12).
-- Kısıt yeniden kurulur; mevcut satırlar zaten NORMAL/ARIZALI olduğu için doğrulama sorunsuz geçer.
-- =====================================================================================

ALTER TABLE ENVANTER DROP CONSTRAINT CK_ENVANTER_DURUM;

ALTER TABLE ENVANTER ADD CONSTRAINT CK_ENVANTER_DURUM CHECK (DURUM IN ('NORMAL', 'ARIZALI', 'IADE'));
