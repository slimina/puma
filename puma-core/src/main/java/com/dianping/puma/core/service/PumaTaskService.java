package com.dianping.puma.core.service;

import com.dianping.puma.core.entity.PumaTask;

import java.util.List;

public interface PumaTaskService {

	PumaTask find(String id);

	PumaTask findByName(String name);

	List<PumaTask> findBySrcDBInstanceId(String srcDBInstanceId);

	List<PumaTask> findByPumaServerId(String pumaServerName);

	List<PumaTask> findAll();

	void create(PumaTask entity);

	void update(PumaTask entity);

	void remove(String id);
}