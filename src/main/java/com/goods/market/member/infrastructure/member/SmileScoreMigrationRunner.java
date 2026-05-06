package com.goods.market.member.infrastructure.member;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SmileScoreMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Boolean legacyColumnExists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = current_schema()
                      and table_name = 'member'
                      and column_name = 'manner_temp'
                )
                """, Boolean.class);

        Boolean newColumnExists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = current_schema()
                      and table_name = 'member'
                      and column_name = 'smile_score'
                )
                """, Boolean.class);

        if (Boolean.TRUE.equals(legacyColumnExists) && Boolean.TRUE.equals(newColumnExists)) {
            jdbcTemplate.update("update member set smile_score = manner_temp");
            jdbcTemplate.update("alter table member drop column manner_temp");
        }
    }
}
