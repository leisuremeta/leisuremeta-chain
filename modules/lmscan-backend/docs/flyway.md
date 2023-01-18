<flyway command with sbt>
1. sbt flywayBaseline -> baselineVersion까지의 migration 제외한 현재 db의 baseline 지정 
2. sbt flywayClean -> 해당 schema 모든 내용 삭제
3. sbt flywayMigrate -> baselineVersion 이후 migration version file update
4. sbt flywayInfo -> migration status info 출력
5. sbt flywayRepair -> flyway_schema_history table 수정 (DDL 트랜젝션 없이 db에 실패한 migration 삭제 및 잘못된 checksum 수정)


<use case>
1. 처음 빈 스키마의 경우 -> sbt flywayMigrate
2. 처음 빈 스키마가 아닌 경우
    (1) 스키마를 모두 비우고 해당 migration file로 다시 시작하는 경우 -> sbt flywayClean && sbt flywayMigrate
    (2) sbt flywayBaseline으로 flyway_schema_history 생성 후 sbt flywayMigrate 통해 특정 버전 이후부터 관리 및 적용 가능
3. 이후부터는 sbt flywayMigrate 명령어를 통해 변경된 migration이 있을경우마다 반영가능


<refs>
1. https://davidmweber.github.io/flyway-sbt-docs/repair.html
2. https://flywaydb.org/documentation/command/migrate

