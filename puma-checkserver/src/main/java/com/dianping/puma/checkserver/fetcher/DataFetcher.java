package com.dianping.puma.checkserver.fetcher;

import javax.sql.DataSource;

/**
 * Dozer @ 2015-09
 * mail@dozer.cc
 * http://www.dozer.cc
 */
public interface DataFetcher {
    void init(DataSource dataSource);
}