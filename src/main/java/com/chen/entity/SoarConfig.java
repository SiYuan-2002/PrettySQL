package com.chen.entity;

import java.util.Map;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/20 8:16
 */
public class SoarConfig {
    public Map<String, Dsn> online_dsn;
    public Map<String, Dsn> test_dsn;
    public boolean allow_online_as_test;
    public boolean drop_test_temporary;
    public boolean only_syntax_check;
    public int sampling_statistic_target;
    public boolean sampling;
    public int log_level;
    public String log_output;
    public String report_type;
    public String[] ignore_rules;
    public String blacklist;
    public int max_join_table_count;
    public int max_group_by_cols_count;
    public int max_distinct_count;
    public int max_index_cols_count;
    public int max_total_rows;
    public int spaghetti_query_length;
    public boolean allow_drop_index;
    public String explain_sql_report_type;
    public String explain_type;
    public String explain_format;
    public String[] explain_warn_select_type;
    public String[] explain_warn_access_type;
    public int explain_max_keys;
    public int explain_min_keys;
    public int explain_max_rows;
    public String[] explain_warn_extra;
    public int explain_max_filtered;
    public String[] explain_warn_scalability;
    public String query;
    public boolean list_heuristic_rules;
    public boolean list_test_sqls;
    public boolean verbose;

    public static class Dsn {
        public String addr;
        public String schema;
        public String user;
        public String password;
        public boolean disable;
    }

    public Map<String, Dsn> getOnline_dsn() {
        return online_dsn;
    }

    public void setOnline_dsn(Map<String, Dsn> online_dsn) {
        this.online_dsn = online_dsn;
    }

    public Map<String, Dsn> getTest_dsn() {
        return test_dsn;
    }

    public void setTest_dsn(Map<String, Dsn> test_dsn) {
        this.test_dsn = test_dsn;
    }

    public boolean isAllow_online_as_test() {
        return allow_online_as_test;
    }

    public void setAllow_online_as_test(boolean allow_online_as_test) {
        this.allow_online_as_test = allow_online_as_test;
    }

    public boolean isDrop_test_temporary() {
        return drop_test_temporary;
    }

    public void setDrop_test_temporary(boolean drop_test_temporary) {
        this.drop_test_temporary = drop_test_temporary;
    }

    public boolean isOnly_syntax_check() {
        return only_syntax_check;
    }

    public void setOnly_syntax_check(boolean only_syntax_check) {
        this.only_syntax_check = only_syntax_check;
    }

    public int getSampling_statistic_target() {
        return sampling_statistic_target;
    }

    public void setSampling_statistic_target(int sampling_statistic_target) {
        this.sampling_statistic_target = sampling_statistic_target;
    }

    public boolean isSampling() {
        return sampling;
    }

    public void setSampling(boolean sampling) {
        this.sampling = sampling;
    }

    public int getLog_level() {
        return log_level;
    }

    public void setLog_level(int log_level) {
        this.log_level = log_level;
    }

    public String getLog_output() {
        return log_output;
    }

    public void setLog_output(String log_output) {
        this.log_output = log_output;
    }

    public String getReport_type() {
        return report_type;
    }

    public void setReport_type(String report_type) {
        this.report_type = report_type;
    }

    public String[] getIgnore_rules() {
        return ignore_rules;
    }

    public void setIgnore_rules(String[] ignore_rules) {
        this.ignore_rules = ignore_rules;
    }

    public String getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(String blacklist) {
        this.blacklist = blacklist;
    }

    public int getMax_join_table_count() {
        return max_join_table_count;
    }

    public void setMax_join_table_count(int max_join_table_count) {
        this.max_join_table_count = max_join_table_count;
    }

    public int getMax_group_by_cols_count() {
        return max_group_by_cols_count;
    }

    public void setMax_group_by_cols_count(int max_group_by_cols_count) {
        this.max_group_by_cols_count = max_group_by_cols_count;
    }

    public int getMax_distinct_count() {
        return max_distinct_count;
    }

    public void setMax_distinct_count(int max_distinct_count) {
        this.max_distinct_count = max_distinct_count;
    }

    public int getMax_index_cols_count() {
        return max_index_cols_count;
    }

    public void setMax_index_cols_count(int max_index_cols_count) {
        this.max_index_cols_count = max_index_cols_count;
    }

    public int getMax_total_rows() {
        return max_total_rows;
    }

    public void setMax_total_rows(int max_total_rows) {
        this.max_total_rows = max_total_rows;
    }

    public int getSpaghetti_query_length() {
        return spaghetti_query_length;
    }

    public void setSpaghetti_query_length(int spaghetti_query_length) {
        this.spaghetti_query_length = spaghetti_query_length;
    }

    public boolean isAllow_drop_index() {
        return allow_drop_index;
    }

    public void setAllow_drop_index(boolean allow_drop_index) {
        this.allow_drop_index = allow_drop_index;
    }

    public String getExplain_sql_report_type() {
        return explain_sql_report_type;
    }

    public void setExplain_sql_report_type(String explain_sql_report_type) {
        this.explain_sql_report_type = explain_sql_report_type;
    }

    public String getExplain_type() {
        return explain_type;
    }

    public void setExplain_type(String explain_type) {
        this.explain_type = explain_type;
    }

    public String getExplain_format() {
        return explain_format;
    }

    public void setExplain_format(String explain_format) {
        this.explain_format = explain_format;
    }

    public String[] getExplain_warn_select_type() {
        return explain_warn_select_type;
    }

    public void setExplain_warn_select_type(String[] explain_warn_select_type) {
        this.explain_warn_select_type = explain_warn_select_type;
    }

    public String[] getExplain_warn_access_type() {
        return explain_warn_access_type;
    }

    public void setExplain_warn_access_type(String[] explain_warn_access_type) {
        this.explain_warn_access_type = explain_warn_access_type;
    }

    public int getExplain_max_keys() {
        return explain_max_keys;
    }

    public void setExplain_max_keys(int explain_max_keys) {
        this.explain_max_keys = explain_max_keys;
    }

    public int getExplain_min_keys() {
        return explain_min_keys;
    }

    public void setExplain_min_keys(int explain_min_keys) {
        this.explain_min_keys = explain_min_keys;
    }

    public int getExplain_max_rows() {
        return explain_max_rows;
    }

    public void setExplain_max_rows(int explain_max_rows) {
        this.explain_max_rows = explain_max_rows;
    }

    public String[] getExplain_warn_extra() {
        return explain_warn_extra;
    }

    public void setExplain_warn_extra(String[] explain_warn_extra) {
        this.explain_warn_extra = explain_warn_extra;
    }

    public int getExplain_max_filtered() {
        return explain_max_filtered;
    }

    public void setExplain_max_filtered(int explain_max_filtered) {
        this.explain_max_filtered = explain_max_filtered;
    }

    public String[] getExplain_warn_scalability() {
        return explain_warn_scalability;
    }

    public void setExplain_warn_scalability(String[] explain_warn_scalability) {
        this.explain_warn_scalability = explain_warn_scalability;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isList_heuristic_rules() {
        return list_heuristic_rules;
    }

    public void setList_heuristic_rules(boolean list_heuristic_rules) {
        this.list_heuristic_rules = list_heuristic_rules;
    }

    public boolean isList_test_sqls() {
        return list_test_sqls;
    }

    public void setList_test_sqls(boolean list_test_sqls) {
        this.list_test_sqls = list_test_sqls;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
