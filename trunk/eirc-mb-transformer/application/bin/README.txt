1. Настройка базы данных перед сборкой в eirc/eirc-mb-transformer/src/main/resources/mybatis-config.xml.
2. Сборка в корне eirc: maven package.
3. Архивы в eirc/eirc-mb-transformer/target.
4. Настройка окружения: bin/properties
 mbOrganizationId   - id организации МБ.
 eircOrganizationId - id организации ЕИРЦ.
 tmpDir             - директория временных файлов.
5. Запускаем mb-transformer.sh в директории bin c аргументами:
 -c,--corrections <file>   Input MB corrections pathname
 -h,--help                 Print this message
 -n,--charges <file>       Input MB charges pathname
 -r,--registry <file>      Output EIRC registry pathname