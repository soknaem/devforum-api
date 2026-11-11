package com.setec.forum.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Hibernate 6 puts a CHECK constraint on every @Enumerated(STRING) column
 * (e.g. check (type in ('FOLLOW','COMMENT',...))). `ddl-auto: update` creates it once but never
 * widens it, so a database created BEFORE the NEW_POST notification type existed would reject the
 * new value (ORA-02290) and creating a post would fail.
 *
 * On startup this drops that stale constraint on NOTIFICATIONS.TYPE so new enum values just work.
 * Oracle only; every step is best-effort and never prevents the application from starting.
 * Safe to delete once your database has been migrated.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class NotificationSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Map<String, Object>> checks = jdbc.queryForList(
                    "select constraint_name, search_condition_vc as cond from user_constraints "
                            + "where table_name = 'NOTIFICATIONS' and constraint_type = 'C'");

            for (Map<String, Object> row : checks) {
                String name = String.valueOf(row.get("CONSTRAINT_NAME"));
                String condition = String.valueOf(row.get("COND")).toUpperCase();

                boolean enumCheck = condition.contains("COMMENT_REPLY");
                if (enumCheck && !condition.contains("NEW_POST")) {
                    jdbc.execute("alter table notifications drop constraint \"" + name + "\"");
                    log.info("Dropped outdated enum check constraint {} on notifications.type", name);
                }
            }
        } catch (Exception ex) {
            // Not Oracle, table not created yet, or no permission: nothing to fix.
            log.debug("Notification schema check skipped: {}", ex.getMessage());
        }
    }
}
